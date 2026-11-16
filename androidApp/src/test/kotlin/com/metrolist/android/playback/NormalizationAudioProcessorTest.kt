package com.metrolist.android.playback

import android.app.Application
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.source.SinglePeriodTimeline
import com.metrolist.shared.playback.playbackNormalizationGain
import com.metrolist.shared.youtube.playback.PlaybackResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [33])
class NormalizationAudioProcessorTest {
    @Test
    fun preferenceRefreshCannotBeOverwrittenByAnOlderConcurrentGainCalculation() {
        var enabled = true
        var blockCalculation = false
        val calculationStarted = CountDownLatch(1)
        val finishCalculation = CountDownLatch(1)
        val resource = PlaybackResource("song", "file:///song", emptyMap(), null, null)
        val processor =
            NormalizationAudioProcessor {
                if (enabled) {
                    if (blockCalculation) {
                        calculationStarted.countDown()
                        check(finishCalculation.await(5, TimeUnit.SECONDS))
                    }
                    2f
                } else {
                    1f
                }
            }.apply {
                this.resource = resource
                configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
                flush(AudioProcessor.StreamMetadata.DEFAULT)
            }
        blockCalculation = true
        val staleRefresh = Thread { processor.refreshGain() }
        staleRefresh.start()
        assertTrue(calculationStarted.await(5, TimeUnit.SECONDS))
        enabled = false
        val preferenceRefreshStarted = CountDownLatch(1)
        val preferenceRefresh =
            Thread {
                preferenceRefreshStarted.countDown()
                processor.refreshGain()
            }
        preferenceRefresh.start()
        assertTrue(preferenceRefreshStarted.await(5, TimeUnit.SECONDS))
        finishCalculation.countDown()
        staleRefresh.join(5_000)
        preferenceRefresh.join(5_000)
        assertTrue(!staleRefresh.isAlive)
        assertTrue(!preferenceRefresh.isAlive)

        processor.queueInput(ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).putShort(0, 1000))
        assertEquals(1000.toShort(), processor.output.getShort(0))
    }

    @Test
    fun queuedPcmUsesItsOwnNormalizationBeforeTheCurrentSongChanges() {
        val outgoing = PlaybackResource("outgoing", "file:///outgoing", emptyMap(), null, null)
        val incoming = outgoing.copy(videoId = "incoming", url = "file:///incoming")
        val processor = NormalizationAudioProcessor { if (it?.videoId == incoming.videoId) 2f else 1f }
        processor.resource = outgoing
        processor.configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
        val timeline =
            SinglePeriodTimeline(
                20_000_000L,
                true,
                false,
                false,
                null,
                MediaItem
                    .Builder()
                    .setUri(incoming.url)
                    .setTag(incoming)
                    .build(),
            )
        processor.flush(
            AudioProcessor.StreamMetadata
                .Builder()
                .setTimeline(timeline)
                .setPeriodUid(timeline.getUidOfPeriod(0))
                .build(),
        )

        processor.queueInput(ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).putShort(0, 1000))
        assertEquals(2000.toShort(), processor.output.getShort(0))
        assertEquals(1f, processor.normalizationGain)

        processor.resource = incoming
        processor.queueInput(ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).putShort(0, 1000))
        assertEquals(2000.toShort(), processor.output.getShort(0))
    }

    @Test
    fun queuedPcmUsesItsOwnAttenuationBeforeTheCurrentSongChanges() {
        val outgoing = PlaybackResource("outgoing", "file:///outgoing", emptyMap(), null, null)
        val incoming = outgoing.copy(videoId = "incoming", url = "file:///incoming")
        val processor = NormalizationAudioProcessor { if (it?.videoId == incoming.videoId) 0.5f else 1f }
        processor.resource = outgoing
        processor.configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
        val timeline =
            SinglePeriodTimeline(
                20_000_000L,
                true,
                false,
                false,
                null,
                MediaItem
                    .Builder()
                    .setUri(incoming.url)
                    .setTag(incoming)
                    .build(),
            )
        processor.flush(
            AudioProcessor.StreamMetadata
                .Builder()
                .setTimeline(timeline)
                .setPeriodUid(timeline.getUidOfPeriod(0))
                .build(),
        )

        processor.queueInput(ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).putShort(0, 1000))
        assertEquals(500.toShort(), processor.output.getShort(0))
        assertEquals(1f, processor.normalizationGain)
    }

    @Test
    fun playersUseTheirOwnMetadataAndLivePreferencesWithoutApplyingAttenuationTwice() {
        var enabled = true
        var calculations = 0

        fun processor(lufs: Double) =
            NormalizationAudioProcessor {
                calculations++
                playbackNormalizationGain(it?.loudnessDb, enabled, perceptualLoudnessDb = it?.perceptualLoudnessDb)
            }.apply {
                resource = PlaybackResource("song", "file:///song", emptyMap(), 0.0, null, perceptualLoudnessDb = lufs)
                configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
                flush(AudioProcessor.StreamMetadata.DEFAULT)
            }

        fun process(
            processor: NormalizationAudioProcessor,
            sample: Short,
        ): Short {
            processor.queueInput(ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).putShort(0, sample))
            return processor.output.getShort(0)
        }
        val outgoing = processor(-8.0)
        val incoming = processor(-17.0)
        assertEquals(1000.toShort(), process(outgoing, 1000))
        assertEquals(1412.toShort(), process(incoming, 1000))
        assertTrue(process(incoming, Short.MAX_VALUE) in 30_000..Short.MAX_VALUE)
        assertTrue(process(incoming, 30_000) < process(incoming, Short.MAX_VALUE))
        incoming.resource = incoming.resource
        assertEquals(2, calculations)
        enabled = false
        incoming.refreshGain()
        assertEquals(1000.toShort(), process(incoming, 1000))
        enabled = true
        incoming.resource = incoming.resource?.copy(perceptualLoudnessDb = -16.0)
        assertEquals(1258.toShort(), process(incoming, 1000))
        incoming.resource = null
        assertEquals(1000.toShort(), process(incoming, 1000))
    }

    @Test
    fun emptyInputBeforeAudioDoesNotCopyTheSharedEmptyBufferIntoItself() {
        val format = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
        val processors =
            listOf(
                NormalizationAudioProcessor { 2f },
                SilenceDetectorAudioProcessor().apply { enabled = true },
            )
        processors.forEach { processor ->
            processor.configure(format)
            processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
            processor.queueInput(AudioProcessor.EMPTY_BUFFER)
            assertEquals(0, processor.output.remaining())
            processor.queueInput(ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()))
            assertEquals(4, processor.output.remaining())
        }
    }
}
