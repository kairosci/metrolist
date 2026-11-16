package com.metrolist.android.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.metrolist.shared.playback.normalizationPcm16
import com.metrolist.shared.youtube.playback.PlaybackResource
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
internal class NormalizationAudioProcessor(
    private val gain: (PlaybackResource?) -> Float,
) : BaseAudioProcessor() {
    @Volatile
    var resource: PlaybackResource? = null
        set(value) {
            synchronized(this) {
                if (field === value) return
                field = value
                refreshGainLocked()
            }
        }

    @Volatile
    var normalizationGain: Float = 1f
        private set

    @Volatile private var streamResource: PlaybackResource? = null

    @Volatile private var streamGain = 1f

    @Synchronized
    fun refreshGain() {
        refreshGainLocked()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        // The next item's PCM can reach the sink before the playlist transition reaches the app.
        val nextStreamResource =
            streamMetadata.periodUid?.takeUnless { streamMetadata.timeline.isEmpty }?.let { uid ->
                val windowIndex = streamMetadata.timeline.getPeriodByUid(uid, Timeline.Period()).windowIndex
                streamMetadata.timeline
                    .getWindow(windowIndex, Timeline.Window())
                    .mediaItem.localConfiguration
                    ?.tag as? PlaybackResource
            }
        updateStreamResource(nextStreamResource)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Bypass non-PCM16 output instead of failing sink configuration.
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioProcessor.AudioFormat.NOT_SET
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        // An empty replaceOutputBuffer() can return the shared EMPTY_BUFFER that is also our input.
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        val boost = streamGain
        if (boost == 1f) {
            output.put(inputBuffer)
        } else {
            while (inputBuffer.hasRemaining()) {
                output.putShort(normalizationPcm16(inputBuffer.short, boost))
            }
        }
        output.flip()
    }

    @Synchronized
    private fun updateStreamResource(resource: PlaybackResource?) {
        if (streamResource === resource) return
        streamResource = resource
        refreshGainLocked()
    }

    private fun refreshGainLocked() {
        normalizationGain = gain(resource)
        streamGain = if (streamResource == null || streamResource === resource) normalizationGain else gain(streamResource)
    }
}
