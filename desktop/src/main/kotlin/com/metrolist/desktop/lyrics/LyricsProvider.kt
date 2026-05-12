package com.metrolist.desktop.lyrics

interface LyricsProvider {
    val name: String

    fun isEnabled(): Boolean

    suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String>
}
