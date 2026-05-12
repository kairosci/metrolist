package com.metrolist.desktop.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object LrcLibDesktopProvider : LyricsProvider {
    override val name = "LrcLib"

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
            defaultRequest { url("https://lrclib.net") }
            expectSuccess = true
        }
    }

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) cleaned = cleaned.replace(pattern, "")
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun queryLyricsWithParams(
        trackName: String? = null,
        artistName: String? = null,
        albumName: String? = null,
        query: String? = null,
    ): List<LrcLibTrack> = runCatching {
        client.get("/api/search") {
            if (query != null) parameter("q", query)
            if (trackName != null) parameter("track_name", trackName)
            if (artistName != null) parameter("artist_name", artistName)
            if (albumName != null) parameter("album_name", albumName)
        }.body<List<LrcLibTrack>>()
    }.getOrDefault(emptyList())

    private suspend fun queryLyrics(artist: String, title: String, album: String?): List<LrcLibTrack> {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        var results = queryLyricsWithParams(trackName = cleanedTitle, artistName = cleanedArtist, albumName = album)
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        results = queryLyricsWithParams(trackName = cleanedTitle)
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        results = queryLyricsWithParams(query = "$cleanedArtist $cleanedTitle")
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        results = queryLyricsWithParams(query = cleanedTitle)
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        if (cleanedTitle != title.trim()) {
            results = queryLyricsWithParams(trackName = title.trim(), artistName = artist.trim())
                .filter { it.syncedLyrics != null || it.plainLyrics != null }
        }
        return results
    }

    override fun isEnabled(): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = runCatching {
        val tracks = queryLyrics(artist, title, album)
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        val res = if (duration == -1) {
            tracks.bestMatchingFor(duration, cleanedTitle, cleanedArtist)?.let { track ->
                track.syncedLyrics ?: track.plainLyrics
            }
        } else {
            tracks.bestMatchingForRelaxed(duration)?.let { track ->
                track.syncedLyrics ?: track.plainLyrics
            }
        }

        res ?: throw IllegalStateException("Lyrics unavailable")
    }

    @Serializable
    data class LrcLibTrack(
        val id: Long? = null,
        val trackName: String = "",
        val artistName: String = "",
        val albumName: String? = null,
        val duration: Double = 0.0,
        val instrumental: Boolean = false,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
    )
}

private fun List<LrcLibDesktopProvider.LrcLibTrack>.bestMatchingFor(
    duration: Int,
    title: String,
    artist: String,
): LrcLibDesktopProvider.LrcLibTrack? {
    if (isEmpty()) return null
    if (size == 1) return first()

    val scored = map { track ->
        var score = 0.0
        if (track.syncedLyrics != null) score += 1.0
        if (track.trackName.equals(title, ignoreCase = true)) score += 0.5
        if (track.artistName.equals(artist, ignoreCase = true)) score += 0.5
        if (track.albumName?.equals(title, ignoreCase = true) == true) score += 0.25
        track to score
    }
    return scored.maxByOrNull { it.second }?.first
}

private fun List<LrcLibDesktopProvider.LrcLibTrack>.bestMatchingForRelaxed(
    duration: Int,
): LrcLibDesktopProvider.LrcLibTrack? {
    if (isEmpty()) return null
    if (size == 1 && duration == -1) return first()

    val relaxed = filter { kotlin.math.abs(it.duration.toInt() - duration) <= 5 }
    if (relaxed.isNotEmpty()) {
        return relaxed.minByOrNull { kotlin.math.abs(it.duration.toInt() - duration) }
    }

    val wider = filter { kotlin.math.abs(it.duration.toInt() - duration) <= 15 }
    if (wider.isNotEmpty()) {
        return wider.minByOrNull { kotlin.math.abs(it.duration.toInt() - duration) }
    }

    return minByOrNull { kotlin.math.abs(it.duration.toInt() - duration) }
}
