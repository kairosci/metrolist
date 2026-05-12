package com.metrolist.desktop.lyrics

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YouTubeLyricsDesktopProvider : LyricsProvider {
    override val name = "YouTube Music"

    override fun isEnabled(): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val nextResult = YouTube.next(WatchEndpoint(videoId = id)).getOrThrow()
            YouTube.lyrics(
                endpoint = nextResult.lyricsEndpoint
                    ?: throw IllegalStateException("Lyrics endpoint not found"),
            ).getOrThrow() ?: throw IllegalStateException("Lyrics unavailable")
        }
    }
}
