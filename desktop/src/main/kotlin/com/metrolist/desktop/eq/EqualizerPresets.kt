package com.metrolist.desktop.eq

data class EqualizerConfig(
    val bands: List<Float> = List(10) { 0f },
    val preamp: Float = 0f,
    val enabled: Boolean = false,
    val selectedPreset: String = "Flat",
)

object EqualizerPresets {
    val presets = mapOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Rock" to listOf(4f, 3f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 3f),
        "Pop" to listOf(-2f, -1f, 0f, 2f, 3f, 3f, 2f, 1f, 0f, -1f),
        "Jazz" to listOf(3f, 2f, 1f, 1f, 0f, 0f, 1f, 1f, 2f, 3f),
        "Classical" to listOf(4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f),
        "Electronic" to listOf(2f, 0f, -1f, -2f, -1f, 2f, 4f, 5f, 4f, 3f),
        "Hip Hop" to listOf(4f, 3f, 2f, 1f, 0f, -1f, -1f, 0f, 1f, 2f),
        "R&B" to listOf(3f, 3f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 3f),
        "Acoustic" to listOf(4f, 3f, 2f, 1f, 0f, -1f, -1f, 0f, 1f, 2f),
        "Bass Boost" to listOf(6f, 5f, 4f, 2f, 0f, -1f, -2f, -1f, 0f, 1f),
        "Treble Boost" to listOf(-2f, -1f, 0f, 1f, 2f, 3f, 4f, 5f, 6f, 6f),
        "Vocal" to listOf(-1f, 0f, 1f, 2f, 3f, 3f, 2f, 1f, 0f, -1f),
    )
}
