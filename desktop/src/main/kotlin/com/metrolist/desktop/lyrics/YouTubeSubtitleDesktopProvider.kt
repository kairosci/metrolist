package com.metrolist.desktop.lyrics

import com.metrolist.innertube.YouTube

object YouTubeSubtitleDesktopProvider : LyricsProvider {
    override val name = "YouTube Subtitle"

    override fun isEnabled(): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = YouTube.transcript(id)
}
