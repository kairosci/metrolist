package com.metrolist.shared.playback

import com.metrolist.shared.equalizer.NoiseCancellation
import com.metrolist.shared.equalizer.NoiseMicrophone
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetoothA2DP
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.isInputAvailable
import platform.AVFAudio.setActive
import kotlin.concurrent.Volatile
import kotlin.math.roundToInt

private const val INPUT_BUS = 0uL
private const val TAP_FRAMES = 1024u
private const val PCM_SCALE = 32_767f

/**
 * Reads the microphone through an AVAudioEngine tap. Samples are converted to PCM16 and published
 * only to [NoiseCancellation]; the engine owns the tap and the session for its lifetime.
 */
internal object IosNoiseMicrophone : NoiseMicrophone {
    @Volatile private var engine: AVAudioEngine? = null

    private var samples = ShortArray(0)

    val isCapturing: Boolean
        get() = engine != null

    override val isSupported: Boolean
        get() = AVAudioSession.sharedInstance().isInputAvailable()

    override fun hasPermission(): Boolean = AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermissionGranted

    override fun requestPermission() {
        AVAudioSession.sharedInstance().requestRecordPermission {
            NoiseCancellation.refreshPermission()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun start(
        sampleRate: Int,
        onSamples: (ShortArray, Int, Int) -> Unit,
        onFailure: () -> Unit,
    ): Boolean =
        synchronized(this) {
            if (engine != null) return@synchronized true
            if (!hasPermission()) return@synchronized false
            if (!configureIosAudioSession(activate = true, noiseCapture = true)) return@synchronized false
            val captureEngine = AVAudioEngine()
            var tapInstalled = false
            try {
                val input = captureEngine.inputNode
                val format = input.inputFormatForBus(INPUT_BUS)
                val rate = format.sampleRate.toInt()
                if (rate <= 0 || format.channelCount.toInt() <= 0) return@synchronized false
                input.installTapOnBus(INPUT_BUS, TAP_FRAMES, format) { buffer, _ ->
                    val frames = buffer?.frameLength?.toInt() ?: 0
                    val channel = buffer?.floatChannelData?.get(0)
                    if (frames <= 0 || channel == null) return@installTapOnBus
                    if (samples.size < frames) samples = ShortArray(frames)
                    for (index in 0 until frames) {
                        val value = channel.get(index).coerceIn(-1f, 1f)
                        samples[index] = (value * PCM_SCALE).roundToInt().toShort()
                    }
                    onSamples(samples, frames, rate)
                }
                tapInstalled = true
                captureEngine.prepare()
                if (!captureEngine.startAndReturnError(null)) return@synchronized false
                engine = captureEngine
                true
            } catch (_: Exception) {
                false
            } finally {
                if (engine != captureEngine) {
                    if (tapInstalled) runCatching { captureEngine.inputNode.removeTapOnBus(INPUT_BUS) }
                    runCatching { captureEngine.stop() }
                    configureIosAudioSession(noiseCapture = false)
                }
            }
        }

    override fun stop() =
        synchronized(this) {
            val captureEngine = engine ?: return@synchronized
            engine = null
            runCatching { captureEngine.inputNode.removeTapOnBus(INPUT_BUS) }
            runCatching { captureEngine.stop() }
            configureIosAudioSession(noiseCapture = false)
        }
}

/**
 * PlayAndRecord keeps the microphone route available while capture runs. MixWithOthers,
 * DefaultToSpeaker and the Bluetooth options preserve music output through the selected route.
 * Other players call this before activating the session so they cannot silently drop the capture category.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun configureIosAudioSession(
    activate: Boolean = false,
    noiseCapture: Boolean? = null,
): Boolean {
    val session = AVAudioSession.sharedInstance()
    val needsInput = noiseCapture ?: (NoiseCancellation.isActive || IosNoiseMicrophone.isCapturing)
    return runCatching {
        if (needsInput) {
            session.setCategory(
                AVAudioSessionCategoryPlayAndRecord,
                mode = AVAudioSessionModeDefault,
                options =
                    AVAudioSessionCategoryOptionMixWithOthers or
                        AVAudioSessionCategoryOptionDefaultToSpeaker or
                        AVAudioSessionCategoryOptionAllowBluetooth or
                        AVAudioSessionCategoryOptionAllowBluetoothA2DP,
                error = null,
            )
        } else if (session.category != AVAudioSessionCategoryPlayback) {
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        }
        if (activate) session.setActive(true, error = null)
        true
    }.getOrDefault(false)
}
