package com.metrolist.android.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.metrolist.shared.equalizer.AmbientNoiseCanceller
import java.nio.ByteBuffer

/** Applies the microphone correction after Media3's silence skipping and speed/pitch processors. */
@OptIn(UnstableApi::class)
internal class NoiseCancellationAudioProcessor : BaseAudioProcessor() {
    private var canceller: AmbientNoiseCanceller? = null
    private var samples = ShortArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioProcessor.AudioFormat.NOT_SET
        canceller = AmbientNoiseCanceller(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val output = replaceOutputBuffer(size)
        val count = size / 2
        if (samples.size < count) samples = ShortArray(count)
        // Media3 PCM buffers use native byte order, which the short views inherit.
        inputBuffer.asShortBuffer().get(samples, 0, count)
        inputBuffer.position(inputBuffer.limit())
        canceller?.processPcm16(samples, count)
        output.asShortBuffer().put(samples, 0, count)
        output.position(count * 2)
        output.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        canceller?.reset()
    }

    override fun onReset() {
        canceller = null
    }
}
