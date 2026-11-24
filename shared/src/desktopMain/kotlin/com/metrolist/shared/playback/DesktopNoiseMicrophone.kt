/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.playback

import com.metrolist.shared.equalizer.NoiseCancellation
import com.metrolist.shared.equalizer.NoiseMicrophone
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/** Captures the local microphone for feed-forward noise reduction; no sample is sent anywhere. */
object DesktopNoiseMicrophone : NoiseMicrophone {
    private val format =
        AudioFormat(
            NoiseCancellation.MICROPHONE_SAMPLE_RATE.toFloat(),
            16,
            1,
            true,
            false,
        )

    @Volatile private var captureThread: Thread? = null

    @Volatile private var activeLine: TargetDataLine? = null
    private val session = AtomicInteger(0)

    override val isSupported: Boolean
        get() = AudioSystem.isLineSupported(DataLine.Info(TargetDataLine::class.java, format))

    override fun hasPermission(): Boolean = isSupported

    override fun requestPermission() = Unit

    override fun start(
        sampleRate: Int,
        onSamples: (ShortArray, Int, Int) -> Unit,
        onFailure: () -> Unit,
    ): Boolean {
        val previous = captureThread
        if (previous?.isAlive == true) {
            val deadline = System.nanoTime() + 500_000_000L
            while (captureThread?.isAlive == true && System.nanoTime() < deadline) Thread.sleep(10)
            if (captureThread?.isAlive == true) return false
        }
        val line =
            try {
                AudioSystem.getLine(DataLine.Info(TargetDataLine::class.java, format)) as TargetDataLine
            } catch (_: Exception) {
                return false
            }
        return try {
            line.open(format, sampleRate / 50 * 2)
            line.start()
            val captureSession = session.incrementAndGet()
            activeLine = line
            val thread =
                Thread(
                    {
                        val bytes = ByteArray(sampleRate / 50 * 2)
                        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                        val samples = ShortArray(bytes.size / 2)
                        var emptyReads = 0
                        try {
                            while (session.get() == captureSession) {
                                val read = line.read(bytes, 0, bytes.size)
                                if (read < 0) {
                                    if (session.get() == captureSession) onFailure()
                                    break
                                }
                                if (read == 0) {
                                    if (++emptyReads >= 10) {
                                        if (session.get() == captureSession) onFailure()
                                        break
                                    }
                                    Thread.sleep(20)
                                    continue
                                }
                                emptyReads = 0
                                buffer.rewind()
                                val count = read / 2
                                for (index in 0 until count) samples[index] = buffer.getShort()
                                onSamples(samples, count, line.format.sampleRate.toInt())
                            }
                        } catch (_: Exception) {
                            if (session.get() == captureSession) onFailure()
                        } finally {
                            runCatching { line.stop() }
                            runCatching { line.close() }
                            if (session.get() == captureSession) {
                                activeLine = null
                                captureThread = null
                            }
                        }
                    },
                    "MetrolistNoiseMicrophone",
                ).apply { isDaemon = true }
            captureThread = thread
            thread.start()
            true
        } catch (_: Exception) {
            session.incrementAndGet()
            activeLine = null
            captureThread = null
            runCatching { line.close() }
            false
        }
    }

    override fun stop() {
        session.incrementAndGet()
        val line = activeLine
        activeLine = null
        runCatching { line?.stop() }
        runCatching { line?.close() }
    }
}
