package com.metrolist.desktop.core

import com.metrolist.desktop.audio.AudioEngine
import com.metrolist.desktop.audio.AudioEngineState
import com.metrolist.desktop.audio.AudioQuality
import com.metrolist.desktop.audio.JavaSoundAudioEngine
import com.metrolist.desktop.core.connectivity.ConnectivityManager
import com.metrolist.innertube.InternalLogger
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

data class PlayerState(
    val currentSong: SongItem? = null,
    val queue: List<SongItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val volume: Float = 0.5f,
    val showQueue: Boolean = false,
    val isBuffering: Boolean = false,
    val audioQuality: AudioQuality = AudioQuality.NORMAL,
)

object DesktopPlayer {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val audioEngine: AudioEngine = JavaSoundAudioEngine(httpClient)
    private var originalQueue: List<SongItem> = emptyList()
    private var shuffledIndices: List<Int> = emptyList()
    private val playerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var positionJob: Job? = null
    private var engineStateJob: Job? = null

    init {
        engineStateJob = playerScope.launch {
            audioEngine.state.collect { engineState ->
                syncFromEngine(engineState)
            }
        }
    }

    private fun syncFromEngine(engineState: AudioEngineState) {
        val s = _state.value
        _state.value = s.copy(
            isPlaying = engineState.playbackState == com.metrolist.desktop.audio.PlaybackState.PLAYING,
            isBuffering = engineState.playbackState == com.metrolist.desktop.audio.PlaybackState.BUFFERING,
            position = engineState.position,
            volume = engineState.volume,
        )
    }

    private suspend fun startAudio(song: SongItem) {
        _state.value = _state.value.copy(isBuffering = true)
        try {
            val result = YouTube.player(
                videoId = song.id,
                client = YouTubeClient.WEB_REMIX,
            )
            val response = result.getOrNull()
            val streamingData = response?.streamingData

            val preferredItags = when (_state.value.audioQuality) {
                AudioQuality.LOW -> listOf(140, 256, 258, 139, 250, 251, 249)
                AudioQuality.NORMAL -> listOf(140, 256, 258, 251, 250, 249)
                AudioQuality.HIGH -> listOf(256, 258, 140, 251, 250, 249)
            }

            val url = streamingData
                ?.adaptiveFormats
                ?.firstOrNull { it.itag in preferredItags && it.url != null }
                ?.url
                ?: streamingData
                    ?.formats
                    ?.firstOrNull { it.url != null }
                    ?.url

            if (url != null) {
                audioEngine.load(url, song.duration?.times(1000L) ?: 0L)
                audioEngine.play()
            } else {
                _state.value = _state.value.copy(isBuffering = false)
                startFallbackTimer()
            }
        } catch (e: Exception) {
            InternalLogger.d("Audio engine failed: ${e.message}, using fallback timer")
            _state.value = _state.value.copy(isBuffering = false)
            startFallbackTimer()
        }
    }

    private suspend fun stopAudio() {
        audioEngine.pause()
        positionJob?.cancel()
    }

    private fun startFallbackTimer() {
        positionJob?.cancel()
        positionJob = playerScope.launch {
            while (true) {
                delay(100)
                val s = _state.value
                if (s.isPlaying && s.duration > 0) {
                    val newPos = s.position + 100
                    if (newPos >= s.duration) {
                        _state.value = s.copy(position = s.duration)
                        next()
                    } else {
                        if (!_state.value.isBuffering) {
                            _state.value = s.copy(position = newPos)
                        }
                    }
                } else {
                    break
                }
            }
        }
    }

    fun play(song: SongItem) {
        _state.value = PlayerState(
            currentSong = song,
            queue = listOf(song),
            currentIndex = 0,
            isPlaying = true,
            duration = song.duration?.times(1000L) ?: 0L,
            shuffle = false,
            repeatMode = _state.value.repeatMode,
            volume = _state.value.volume,
            audioQuality = _state.value.audioQuality,
        )
        playerScope.launch {
            startAudio(song)
        }
        syncToConnectedDevices()
    }

    fun playQueue(songs: List<SongItem>, startIndex: Int) {
        if (songs.isEmpty()) return
        originalQueue = songs.toList()
        shuffledIndices = if (_state.value.shuffle) {
            val indices = songs.indices.toMutableList()
            indices.remove(startIndex)
            indices.shuffle()
            listOf(startIndex) + indices
        } else {
            songs.indices.toList()
        }
        val song = songs.getOrNull(startIndex)
        _state.value = _state.value.copy(
            currentSong = song,
            queue = songs,
            currentIndex = startIndex,
            isPlaying = true,
            duration = song?.duration?.times(1000L) ?: 0L,
            position = 0L,
            isBuffering = true,
        )
        if (song != null) {
            playerScope.launch {
                startAudio(song)
            }
        }
        syncToConnectedDevices()
    }

    fun playPause() {
        if (_state.value.isBuffering) return
        val newPlaying = !_state.value.isPlaying
        _state.value = _state.value.copy(isPlaying = newPlaying)
        if (newPlaying) {
            playerScope.launch { audioEngine.play() }
        } else {
            playerScope.launch { audioEngine.pause() }
            positionJob?.cancel()
        }
        syncToConnectedDevices()
    }

    fun next() {
        val s = _state.value
        val nextIndex = s.currentIndex + 1
        if (nextIndex < s.queue.size) {
            val nextSong = s.queue[nextIndex]
            playerScope.launch {
                audioEngine.stop()
                _state.value = s.copy(
                    currentSong = nextSong,
                    currentIndex = nextIndex,
                    isPlaying = true,
                    duration = nextSong.duration?.times(1000L) ?: 0L,
                    position = 0L,
                    isBuffering = true,
                )
                startAudio(nextSong)
            }
        } else if (s.repeatMode == RepeatMode.ALL) {
            val firstSong = s.queue.firstOrNull()
            if (firstSong != null) {
                playerScope.launch {
                    audioEngine.stop()
                    _state.value = s.copy(
                        currentSong = firstSong,
                        currentIndex = 0,
                        isPlaying = true,
                        position = 0L,
                        isBuffering = true,
                    )
                    startAudio(firstSong)
                }
            }
        } else {
            playerScope.launch { audioEngine.stop() }
            _state.value = s.copy(isPlaying = false, position = 0L, isBuffering = false)
            positionJob?.cancel()
        }
        syncToConnectedDevices()
    }

    fun previous() {
        val s = _state.value
        val prevIndex = s.currentIndex - 1
        if (prevIndex >= 0) {
            val prevSong = s.queue[prevIndex]
            playerScope.launch {
                audioEngine.stop()
                _state.value = s.copy(
                    currentSong = prevSong,
                    currentIndex = prevIndex,
                    isPlaying = true,
                    duration = prevSong.duration?.times(1000L) ?: 0L,
                    position = 0L,
                    isBuffering = true,
                )
                startAudio(prevSong)
            }
        } else if (s.repeatMode == RepeatMode.ALL) {
            val lastSong = s.queue.lastOrNull()
            if (lastSong != null) {
                playerScope.launch {
                    audioEngine.stop()
                    _state.value = s.copy(
                        currentSong = lastSong,
                        currentIndex = s.queue.size - 1,
                        isPlaying = true,
                        position = 0L,
                        isBuffering = true,
                    )
                    startAudio(lastSong)
                }
            }
        } else {
            positionJob?.cancel()
        }
        syncToConnectedDevices()
    }

    fun seekTo(position: Long) {
        _state.value = _state.value.copy(position = position)
        playerScope.launch { audioEngine.seek(position) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        playerScope.launch { audioEngine.setVolume(clamped) }
    }

    fun setAudioQuality(quality: AudioQuality) {
        _state.value = _state.value.copy(audioQuality = quality)
        playerScope.launch { audioEngine.setQuality(quality) }
    }

    fun toggleShuffle() {
        val s = _state.value
        val newShuffle = !s.shuffle
        if (newShuffle && s.queue.isNotEmpty()) {
            val indices = s.queue.indices.toMutableList()
            indices.remove(s.currentIndex)
            indices.shuffle()
            shuffledIndices = listOf(s.currentIndex) + indices
        }
        _state.value = s.copy(shuffle = newShuffle)
        syncToConnectedDevices()
    }

    fun toggleRepeat() {
        val current = _state.value.repeatMode
        val next = when (current) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        _state.value = _state.value.copy(repeatMode = next)
        syncToConnectedDevices()
    }

    fun toggleQueue() {
        _state.value = _state.value.copy(showQueue = !_state.value.showQueue)
    }

    fun toggleNowPlaying() {
        NavigationManager.navigateToNowPlaying()
    }

    fun removeFromQueue(index: Int) {
        val queue = _state.value.queue.toMutableList()
        if (index in queue.indices) {
            queue.removeAt(index)
            originalQueue = queue.toList()
            
            // Update shuffled indices
            if (_state.value.shuffle && shuffledIndices.isNotEmpty()) {
                shuffledIndices = shuffledIndices.mapNotNull { 
                    val newIndex = if (it > index) it - 1 else it
                    if (newIndex >= 0 && newIndex < queue.size) newIndex else null
                }
            }
            
            _state.value = _state.value.copy(queue = queue)
            syncToConnectedDevices()
        }
    }

    fun clearQueue() {
        originalQueue = emptyList()
        shuffledIndices = emptyList()
        playerScope.launch { audioEngine.stop() }
        _state.value = PlayerState(
            shuffle = false,
            repeatMode = _state.value.repeatMode,
            volume = _state.value.volume,
            audioQuality = _state.value.audioQuality,
        )
        syncToConnectedDevices()
    }

    private fun syncToConnectedDevices() {
        try {
            ConnectivityManager.syncPlaybackState(_state.value)
        } catch (e: Exception) {
            InternalLogger.d("ConnectivityManager not ready: ${e.message}")
        }
    }
}
