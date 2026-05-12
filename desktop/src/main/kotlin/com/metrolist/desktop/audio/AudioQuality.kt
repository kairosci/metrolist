package com.metrolist.desktop.audio

enum class AudioQuality(
    val label: String,
    val bitrate: Int,
    val itag: String,
) {
    LOW("Low", 48, "low"),
    NORMAL("Normal", 128, "medium"),
    HIGH("High", 256, "high"),
}
