package com.metrolist.music.playback.video

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.music.playback.video.VideoPlayerService.VideoBinder
import com.metrolist.music.video.VideoMetadata
import com.metrolist.music.video.VideoQueue
import kotlinx.coroutines.flow.StateFlow

@Stable
class VideoPlayerConnection(context: Context) {

    private var service: VideoPlayerService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder is VideoBinder) {
                service = binder.getService()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    val isPlaying: StateFlow<Boolean>
        get() = service?.isPlaying
            ?: throw IllegalStateException("VideoPlayerService not bound")

    val currentVideo: StateFlow<VideoMetadata?>
        get() = service?.currentVideo
            ?: throw IllegalStateException("VideoPlayerService not bound")

    val playbackState: StateFlow<Int>
        get() = service?.playbackState
            ?: throw IllegalStateException("VideoPlayerService not bound")

    val queue: StateFlow<VideoQueue>
        get() = service?.queue
            ?: throw IllegalStateException("VideoPlayerService not bound")

    val playerReady: StateFlow<Boolean>
        get() = service?.playerReady
            ?: throw IllegalStateException("VideoPlayerService not bound")

    fun getPlayer(): ExoPlayer =
        service?.getPlayer()
            ?: throw IllegalStateException("VideoPlayerService not bound")

    fun playVideo(video: VideoMetadata) {
        service?.playVideo(video)
    }

    fun playQueue(videos: List<VideoMetadata>, startIndex: Int = 0) {
        service?.playQueue(videos, startIndex)
    }

    fun togglePlayPause() {
        service?.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        service?.seekTo(positionMs)
    }

    fun skipNext() {
        service?.skipNext()
    }

    fun skipPrevious() {
        service?.skipPrevious()
    }

    fun stopPlayback() {
        service?.stopPlayback()
    }

    fun bind(context: Context) {
        if (!bound) {
            val intent = Intent(context, VideoPlayerService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            bound = true
        }
    }

    fun unbind(context: Context) {
        if (bound) {
            try {
                context.unbindService(connection)
            } catch (_: IllegalArgumentException) {
            }
            bound = false
            service = null
        }
    }
}

@Composable
fun rememberVideoPlayerConnection(): VideoPlayerConnection {
    val context = LocalContext.current
    val connection = remember { VideoPlayerConnection(context) }

    DisposableEffect(Unit) {
        connection.bind(context)
        onDispose { connection.unbind(context) }
    }

    return connection
}
