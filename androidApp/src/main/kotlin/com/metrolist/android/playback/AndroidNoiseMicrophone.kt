/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.playback

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.metrolist.android.App
import com.metrolist.shared.equalizer.NoiseMicrophone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/** Captures the local microphone for feed-forward noise reduction; no sample is sent anywhere. */
internal object AndroidNoiseMicrophone : NoiseMicrophone {
    @Volatile private var permissionLauncher: ActivityResultLauncher<String>? = null

    @Volatile private var job: Job? = null
    private val session = AtomicInteger(0)

    fun attach(launcher: ActivityResultLauncher<String>) {
        permissionLauncher = launcher
    }

    override val isSupported: Boolean
        get() = App.instance.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(App.instance, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun requestPermission() {
        if (!hasPermission()) permissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
    }

    @SuppressLint("MissingPermission")
    override fun start(
        sampleRate: Int,
        onSamples: (ShortArray, Int, Int) -> Unit,
        onFailure: () -> Unit,
    ): Boolean {
        if (job?.isActive == true) return true
        if (!hasPermission()) return false
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) return false
        val buffer = ShortArray(max(minBuffer, sampleRate / 50) / 2)
        val captureSession = session.incrementAndGet()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        job =
            scope.launch {
                var recorder: AudioRecord? = null
                try {
                    if (!isActive) return@launch
                    val activeRecorder =
                        AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            buffer.size * 2,
                        )
                    recorder = activeRecorder
                    if (activeRecorder.state != AudioRecord.STATE_INITIALIZED) error("AudioRecord not initialized")
                    activeRecorder.startRecording()
                    while (isActive) {
                        val read =
                            activeRecorder.read(
                                buffer,
                                0,
                                buffer.size,
                                AudioRecord.READ_BLOCKING,
                            )
                        if (read <= 0) error("AudioRecord read failed")
                        onSamples(buffer, read, activeRecorder.sampleRate)
                    }
                } catch (_: Exception) {
                    if (isActive) onFailure()
                } finally {
                    recorder?.let {
                        runCatching { it.stop() }
                        it.release()
                    }
                    if (session.get() == captureSession) job = null
                    scope.cancel()
                }
            }
        return true
    }

    override fun stop() {
        session.incrementAndGet()
        job?.cancel()
        job = null
    }
}
