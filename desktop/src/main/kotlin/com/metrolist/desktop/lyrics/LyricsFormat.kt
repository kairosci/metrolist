package com.metrolist.desktop.lyrics

private val LRC_TIMESTAMP_HINT = Regex("""\[\d{1,2}:\d{2}""")

fun lyricsTextLooksSynced(lyrics: String?): Boolean {
    if (lyrics.isNullOrBlank()) return false
    val t = lyrics.trim().removePrefix("\uFEFF").trimStart()
    if (t.startsWith('[')) return true
    return LRC_TIMESTAMP_HINT.containsMatchIn(t.take(4096))
}
