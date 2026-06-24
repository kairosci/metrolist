package com.metrolist.music.playback.video

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient.Companion.TVHTML5
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.video.VideoMetadata
import com.metrolist.music.video.VideoQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class VideoPlayerService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var dataSourceFactory: DefaultDataSource.Factory

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentVideo = MutableStateFlow<VideoMetadata?>(null)
    val currentVideo: StateFlow<VideoMetadata?> = _currentVideo

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState

    private val _queue = MutableStateFlow(VideoQueue(emptyList()))
    val queue: StateFlow<VideoQueue> = _queue

    private val _playerReady = MutableStateFlow(false)
    val playerReady: StateFlow<Boolean> = _playerReady

    @Volatile
    private var latestMediaNotification: Notification? = null

    override fun onCreate() {
        super.onCreate()

        ensureForegroundChannelExists()
        if (!ensureStartedAsForegroundOrStop()) {
            return
        }

        val httpClient = OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .build()

        val httpDataSourceFactory = OkHttpDataSource.Factory(httpClient)

        dataSourceFactory = DefaultDataSource.Factory(
            this,
            httpDataSourceFactory,
        )

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory),
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .build()

        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    _playbackState.value = state
                    _isPlaying.value = state == Player.STATE_READY && player.playWhenReady
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let {
                        _currentVideo.value = VideoMetadata.fromMediaItem(it)
                    }
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    _isPlaying.value = playWhenReady && player.playbackState == Player.STATE_READY
                }
            },
        )

        val defaultMediaNotificationProvider =
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.video_player,
            ).apply {
                setSmallIcon(R.drawable.small_icon)
            }

        setMediaNotificationProvider(
            object : MediaNotification.Provider {
                override fun createNotification(
                    mediaSession: MediaSession,
                    mediaButtonPreferences: ImmutableList<CommandButton>,
                    actionFactory: MediaNotification.ActionFactory,
                    onNotificationChangedCallback: MediaNotification.Provider.Callback,
                ): MediaNotification {
                    val trackingCallback =
                        MediaNotification.Provider.Callback { notification ->
                            latestMediaNotification = notification.notification
                            onNotificationChangedCallback.onNotificationChanged(notification)
                        }

                    return defaultMediaNotificationProvider
                        .createNotification(
                            mediaSession,
                            mediaButtonPreferences,
                            actionFactory,
                            trackingCallback,
                        ).also { mediaNotification ->
                            latestMediaNotification = mediaNotification.notification
                        }
                }

                override fun handleCustomCommand(
                    session: MediaSession,
                    action: String,
                    extras: Bundle,
                ): Boolean = defaultMediaNotificationProvider.handleCustomCommand(session, action, extras)

                override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
                    defaultMediaNotificationProvider.notificationChannelInfo
            },
        )

        setListener(
            object : Listener {
                override fun onForegroundServiceStartNotAllowedException() {
                    handleForegroundServiceStartNotAllowed(null)
                }
            },
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        putExtra("video_session", true)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()

        _playerReady.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ensureForegroundWithLatestNotificationOrStop()) {
                return START_NOT_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (::player.isInitialized && !player.isPlaying) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean,
    ) {
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (e: android.app.ForegroundServiceStartNotAllowedException) {
            handleForegroundServiceStartNotAllowed(e)
        } catch (e: IllegalStateException) {
            if (isForegroundServiceStartNotAllowedException(e)) {
                handleForegroundServiceStartNotAllowed(e)
            } else {
                throw e
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        if (MediaSessionService.SERVICE_INTERFACE == intent.action) {
            return super.onBind(intent)
        }
        return VideoBinder(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        mediaScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private val mediaScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getPlayer(): ExoPlayer = player

    /**
     * Resolves the best video stream for a given videoId using TVHTML5 client,
     * which typically provides muxed (audio+video) formats without cipher.
     * Falls back to adaptive streams merged with MergingMediaSource if no muxed format is found.
     */
    private suspend fun resolveVideoMediaSource(
        video: VideoMetadata,
    ): androidx.media3.exoplayer.source.MediaSource? {
        return try {
            // TVHTML5 is a reliable client that returns both muxed and adaptive formats
            val playerResponse = YouTube.player(
                videoId = video.videoId,
                playlistId = null,
                client = TVHTML5,
                signatureTimestamp = null,
                poToken = null,
            ).getOrNull() ?: run {
                android.util.Log.w(TAG, "Failed to get player response for ${video.videoId}")
                return null
            }

            if (playerResponse.playabilityStatus.status != "OK") {
                android.util.Log.w(
                    TAG,
                    "Video ${video.videoId} not playable: ${playerResponse.playabilityStatus.reason}",
                )
                return null
            }

            // Update video metadata with real data from YouTube
            playerResponse.videoDetails?.let { details ->
                val thumbnail = details.thumbnail.thumbnails
                    .maxByOrNull { it.width ?: 0 }?.url
                    ?: video.thumbnailUrl
                _currentVideo.value = video.copy(
                    title = details.title ?: video.title,
                    author = details.author ?: video.author,
                    thumbnailUrl = thumbnail,
                    durationSeconds = details.lengthSeconds.toLongOrNull() ?: video.durationSeconds,
                )
            }

            val streamingData = playerResponse.streamingData ?: run {
                android.util.Log.w(TAG, "No streaming data for ${video.videoId}")
                return null
            }

            // Prefer muxed formats (audio+video combined) from `formats` list
            val muxedFormat = pickBestMuxedFormat(streamingData.formats)
            if (muxedFormat != null) {
                val url = muxedFormat.url ?: run {
                    android.util.Log.w(TAG, "Muxed format has no direct URL for ${video.videoId}")
                    null
                }
                if (url != null) {
                    android.util.Log.i(TAG, "Using muxed format: itag=${muxedFormat.itag}, quality=${muxedFormat.qualityLabel}")
                    val mediaItem = buildMediaItem(video, url)
                    return DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
                }
            }

            // Fallback: merge best adaptive video + best adaptive audio
            val adaptiveFormats = streamingData.adaptiveFormats
            val bestVideo = pickBestVideoFormat(adaptiveFormats)
            val bestAudio = pickBestAudioFormat(adaptiveFormats)

            val bestVideoUrl = bestVideo?.url
            val bestAudioUrl = bestAudio?.url
            if (bestVideo != null && bestVideoUrl != null && bestAudio != null && bestAudioUrl != null) {
                android.util.Log.i(
                    TAG,
                    "Using adaptive merge: video itag=${bestVideo.itag} (${bestVideo.qualityLabel}), audio itag=${bestAudio.itag}",
                )
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(bestVideoUrl))
                val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(bestAudioUrl))
                return MergingMediaSource(videoSource, audioSource)
            }

            // Last resort: just audio (degrade gracefully)
            val audioOnlyUrl = bestAudio?.url
            if (audioOnlyUrl != null) {
                android.util.Log.w(TAG, "No video stream found, falling back to audio-only for ${video.videoId}")
                val mediaItem = buildMediaItem(video, audioOnlyUrl)
                return DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
            }

            android.util.Log.e(TAG, "No usable stream found for ${video.videoId}")
            null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to resolve video stream for ${video.videoId}", e)
            null
        }
    }

    private fun buildMediaItem(video: VideoMetadata, streamUrl: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(video.videoId)
            .setUri(streamUrl)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(video.title.ifBlank { video.videoId })
                    .setArtist(video.author)
                    .setArtworkUri(video.thumbnailUrl?.let { android.net.Uri.parse(it) })
                    .build(),
            )
            .build()

    /** Selects the best muxed (audio+video) format from the `formats` list, preferring 720p or lower. */
    private fun pickBestMuxedFormat(
        formats: List<PlayerResponse.StreamingData.Format>?,
    ): PlayerResponse.StreamingData.Format? {
        if (formats.isNullOrEmpty()) return null
        // Muxed formats have both width/height (video) and audioQuality (audio)
        val muxed = formats.filter { it.url != null && it.width != null && it.audioQuality != null }
        if (muxed.isEmpty()) return null
        // Prefer up to 720p to avoid huge downloads; sort by height descending capped at 720
        return muxed.sortedByDescending { it.height ?: 0 }
            .firstOrNull { (it.height ?: 0) <= 720 }
            ?: muxed.minByOrNull { it.height ?: Int.MAX_VALUE }
    }

    /** Selects the best adaptive video-only format, preferring up to 1080p. */
    private fun pickBestVideoFormat(
        formats: List<PlayerResponse.StreamingData.Format>,
    ): PlayerResponse.StreamingData.Format? {
        val videoFormats = formats.filter { it.width != null && it.url != null }
        if (videoFormats.isEmpty()) return null
        return videoFormats.sortedByDescending { it.height ?: 0 }
            .firstOrNull { (it.height ?: 0) <= 1080 }
            ?: videoFormats.minByOrNull { it.height ?: Int.MAX_VALUE }
    }

    /** Selects the best adaptive audio-only format. */
    private fun pickBestAudioFormat(
        formats: List<PlayerResponse.StreamingData.Format>,
    ): PlayerResponse.StreamingData.Format? =
        formats
            .filter { it.isAudio && it.url != null }
            .maxByOrNull { it.bitrate }

    fun playVideo(video: VideoMetadata) {
        _currentVideo.value = video
        if (_queue.value.videos.isEmpty()) {
            _queue.value = VideoQueue(listOf(video))
        }

        serviceScope.launch {
            val mediaSource = resolveVideoMediaSource(video)
            if (mediaSource != null) {
                player.setMediaSource(mediaSource)
                player.prepare()
                player.playWhenReady = true
            } else {
                android.util.Log.e(TAG, "Could not resolve stream for video ${video.videoId}")
            }
        }
    }

    fun playQueue(videos: List<VideoMetadata>, startIndex: Int = 0) {
        _queue.value = VideoQueue(videos, startIndex)
        val targetVideo = videos.getOrNull(startIndex) ?: return
        _currentVideo.value = targetVideo

        serviceScope.launch {
            // Resolve and play the first video immediately; others are resolved on demand
            val mediaSource = resolveVideoMediaSource(targetVideo)
            if (mediaSource != null) {
                player.setMediaSource(mediaSource)
                player.prepare()
                player.playWhenReady = true
            } else {
                android.util.Log.e(TAG, "Could not resolve stream for video ${targetVideo.videoId}")
            }
        }
    }

    fun togglePlayPause() {
        if (player.playWhenReady) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun skipNext() {
        player.seekToNextMediaItem()
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            player.prepare()
        }
        player.playWhenReady = true
    }

    fun skipPrevious() {
        player.seekToPreviousMediaItem()
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            player.prepare()
        }
        player.playWhenReady = true
    }

    fun stopPlayback() {
        player.stop()
        player.playWhenReady = false
        _currentVideo.value = null
        _isPlaying.value = false
    }

    class VideoBinder(private val service: VideoPlayerService) : Binder() {
        fun getService(): VideoPlayerService = service
    }

    private fun ensureForegroundChannelExists() {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.video_player),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun createFallbackForegroundNotification(): Notification {
        ensureForegroundChannelExists()
        val pending =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.video_player))
            .setContentText("")
            .setSmallIcon(R.drawable.small_icon)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundSafely(
        notification: Notification,
        deniedMessage: String,
        failureMessage: String,
        stopOnFailure: Boolean = true,
    ): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: android.app.ForegroundServiceStartNotAllowedException) {
            android.util.Log.w(TAG, deniedMessage, e)
            if (stopOnFailure) {
                stopSelf()
            }
            false
        } catch (e: Exception) {
            android.util.Log.e(TAG, failureMessage, e)
            if (stopOnFailure) {
                stopSelf()
            }
            false
        }

    private fun ensureStartedAsForegroundOrStop(): Boolean =
        startForegroundSafely(
            notification = createFallbackForegroundNotification(),
            deniedMessage = "Foreground service start not allowed; stopping video service to avoid ANR",
            failureMessage = "Failed to enter foreground; stopping video service to avoid ANR",
        )

    private fun ensureForegroundWithLatestNotificationOrStop(): Boolean =
        startForegroundSafely(
            notification = latestMediaNotification ?: createFallbackForegroundNotification(),
            deniedMessage = "Foreground promotion denied during notification update; stopping video service",
            failureMessage = "Failed to promote video service during notification update; stopping service",
            stopOnFailure = true,
        )

    private fun tryEnsureForegroundWithLatestNotification(): Boolean =
        startForegroundSafely(
            notification = latestMediaNotification ?: createFallbackForegroundNotification(),
            deniedMessage = "Foreground promotion denied during notification update",
            failureMessage = "Failed to promote video service during notification update",
            stopOnFailure = false,
        )

    private fun handleForegroundServiceStartNotAllowed(error: Throwable?) {
        if (error != null) {
            android.util.Log.w(TAG, "Foreground service start denied during notification update", error)
        } else {
            android.util.Log.w(TAG, "Foreground service start denied by MediaSessionService listener")
        }

        if (tryEnsureForegroundWithLatestNotification()) {
            return
        }

        if (!::player.isInitialized) {
            stopSelf()
            return
        }

        if (player.isPlaying) {
            android.util.Log.w(TAG, "Keeping video playback alive after denied foreground restart request")
            return
        }

        runCatching {
            stopPlayback()
            stopSelf()
        }.onFailure {
            android.util.Log.e(TAG, "Failed to stop video service after foreground start denial", it)
            stopSelf()
        }
    }

    private fun isForegroundServiceStartNotAllowedException(e: IllegalStateException): Boolean {
        val cause = e.cause
        return cause is android.app.ForegroundServiceStartNotAllowedException
    }

    companion object {
        const val CHANNEL_ID = "video_channel_01"
        const val NOTIFICATION_ID = 889

        private const val TAG = "VideoPlayerService"
    }
}
