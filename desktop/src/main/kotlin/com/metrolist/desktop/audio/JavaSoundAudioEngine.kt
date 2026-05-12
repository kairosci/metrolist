package com.metrolist.desktop.audio

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import javax.sound.sampled.*

class JavaSoundAudioEngine(
    private val httpClient: HttpClient,
) : AudioEngine {
    private val _state = MutableStateFlow(AudioEngineState())
    override val state = _state.asStateFlow()

    private var sourceDataLine: SourceDataLine? = null
    private var audioFormat: AudioFormat? = null
    private var playbackScope: CoroutineScope? = null
    private var playbackJob: Job? = null
    private var audioBytes: ByteArray? = null
    private var totalBytes: Long = 0
    private var bytesPerMs: Double = 0.0
    private var seekPosition: Long = -1L

    private var deferredPlay = false
    private var pausedPosition: Long = 0L
    private var loadUrl: String? = null
    private var targetDuration: Long = 0L

    override suspend fun load(url: String, duration: Long) {
        release()
        _state.value = AudioEngineState(
            playbackState = PlaybackState.BUFFERING,
            volume = _state.value.volume,
            quality = _state.value.quality,
        )
        loadUrl = url
        targetDuration = duration

        withContext(Dispatchers.IO) {
            try {
                val bytes = httpClient.prepareGet(url).execute().body<ByteArray>() ?: return@withContext
                audioBytes = bytes
                totalBytes = bytes.size.toLong()

                val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(audioBytes))
                audioFormat = stream.format
                val decodedStream = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, stream)
                val decodedBuffer = decodedStream.readAllBytes()
                totalBytes = decodedBuffer.size.toLong()

                val format = decodedStream.format
                val info = DataLine.Info(SourceDataLine::class.java, format)
                val line = AudioSystem.getLine(info) as SourceDataLine
                line.open(format)
                sourceDataLine = line

                val frameSize = format.frameSize
                val frameRate = format.frameRate
                bytesPerMs = (frameSize * frameRate) / 1000.0

                audioBytes = decodedBuffer
                sourceDataLine?.start()

                _state.value = _state.value.copy(
                    playbackState = PlaybackState.STOPPED,
                    duration = if (duration > 0) duration else (totalBytes / bytesPerMs).toLong().coerceAtLeast(0),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    playbackState = PlaybackState.STOPPED,
                    error = "Failed to load audio: ${e.message}",
                )
            }
        }

        if (deferredPlay) {
            deferredPlay = false
            play()
        }
    }

    private var lastLogTime = 0L

    override suspend fun play() {
        val bytes = audioBytes
        val line = sourceDataLine
        if (bytes == null || line == null) {
            if (loadUrl != null) {
                deferredPlay = true
            }
            return
        }

        if (_state.value.playbackState == PlaybackState.PLAYING) return

        _state.value = _state.value.copy(playbackState = PlaybackState.PLAYING)

        if (playbackScope == null) {
            playbackScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }

        playbackJob?.cancel()
        playbackJob = playbackScope?.launch {
            var offset = (pausedPosition * bytesPerMs).toLong().coerceAtMost(totalBytes - 1).coerceAtLeast(0)
            _state.value = _state.value.copy(position = pausedPosition)
            lastLogTime = 0L

            while (isActive) {
                val seekPos = seekPosition
                if (seekPos >= 0) {
                    offset = (seekPos * bytesPerMs).toLong().coerceAtMost(totalBytes - 1).coerceAtLeast(0)
                    seekPosition = -1
                    _state.value = _state.value.copy(position = seekPos)
                }

                val remaining = totalBytes - offset
                if (remaining <= 0) {
                    _state.value = _state.value.copy(playbackState = PlaybackState.STOPPED, position = _state.value.duration)
                    break
                }

                val chunkSize = (bytesPerMs * 100).toInt().coerceIn(1024, 65536).coerceAtMost(remaining.toInt())
                val chunk = bytes.copyOfRange(offset.toInt(), (offset + chunkSize).toInt())
                line.write(chunk, 0, chunk.size)
                offset += chunkSize

                val ms = (offset / bytesPerMs).toLong()
                _state.value = _state.value.copy(position = ms)

                if (!line.isRunning || _state.value.playbackState != PlaybackState.PLAYING) break

                val sleepTime = (chunkSize / bytesPerMs).toLong().coerceAtMost(100)
                delay(sleepTime.coerceAtLeast(1))
            }
        }
    }

    override suspend fun pause() {
        pausedPosition = _state.value.position
        _state.value = _state.value.copy(playbackState = PlaybackState.PAUSED)
        playbackJob?.cancel()
        playbackJob = null
    }

    override suspend fun stop() {
        _state.value = _state.value.copy(
            playbackState = PlaybackState.STOPPED,
            position = 0L,
        )
        pausedPosition = 0L
        playbackJob?.cancel()
        playbackJob = null
    }

    override suspend fun seek(position: Long) {
        seekPosition = position.coerceAtLeast(0)
        _state.value = _state.value.copy(position = position)
    }

    override suspend fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)

        withContext(Dispatchers.IO) {
            try {
                val line = sourceDataLine
                if (line != null && line.isOpen && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val min = gainControl.minimum
                    val max = gainControl.maximum
                    val gain = if (clamped <= 0f) min else min + (max - min) * clamped
                    gainControl.value = gain
                }
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun setQuality(quality: AudioQuality) {
        _state.value = _state.value.copy(quality = quality)
    }

    override suspend fun release() {
        playbackJob?.cancel()
        playbackJob = null
        playbackScope?.cancel()
        playbackScope = null
        withContext(Dispatchers.IO) {
            try {
                sourceDataLine?.stop()
                sourceDataLine?.close()
            } catch (_: Exception) {
            }
            sourceDataLine = null
        }
        audioBytes = null
        audioFormat = null
        totalBytes = 0
        bytesPerMs = 0.0
        pausedPosition = 0L
        deferredPlay = false
        seekPosition = -1
        loadUrl = null
        targetDuration = 0L
    }
}
