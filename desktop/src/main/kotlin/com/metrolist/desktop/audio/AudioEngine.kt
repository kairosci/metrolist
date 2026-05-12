package com.metrolist.desktop.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackState {
    STOPPED,
    PLAYING,
    PAUSED,
    BUFFERING,
}

data class AudioEngineState(
    val playbackState: PlaybackState = PlaybackState.STOPPED,
    val position: Long = 0L,
    val duration: Long = 0L,
    val volume: Float = 0.5f,
    val quality: AudioQuality = AudioQuality.NORMAL,
    val error: String? = null,
)

interface AudioEngine {
    val state: StateFlow<AudioEngineState>

    suspend fun load(url: String, duration: Long)

    suspend fun play()

    suspend fun pause()

    suspend fun stop()

    suspend fun seek(position: Long)

    suspend fun setVolume(volume: Float)

    suspend fun setQuality(quality: AudioQuality)

    suspend fun release()
}
