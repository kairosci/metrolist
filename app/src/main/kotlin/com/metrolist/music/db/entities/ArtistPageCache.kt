package com.metrolist.music.db.entities

import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.ArtistSection
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
data class CachedArtistPage(
    val artist: CachedArtistItem? = null,
    val sections: List<CachedSection>,
    val description: String? = null,
    val subscriberCountText: String? = null,
    val monthlyListenerCount: String? = null,
    val isSubscribed: Boolean = false,
)

@Serializable
data class CachedArtistItem(
    val id: String,
    val title: String,
    val thumbnail: String? = null,
    val channelId: String? = null,
    val isProfile: Boolean = false,
)

@Serializable
data class CachedSection(
    val title: String,
    val items: List<CachedItem>,
    val moreBrowseId: String? = null,
    val moreParams: String? = null,
)

@Serializable
data class CachedItem(
    val id: String,
    val title: String,
    val thumbnail: String,
    val type: String,
    val explicit: Boolean = false,
    val artists: List<CachedArtist> = emptyList(),
    val album: CachedAlbum? = null,
    val duration: Int? = null,
    val year: Int? = null,
    val songCountText: String? = null,
    val author: CachedArtist? = null,
    val channelId: String? = null,
    val browseId: String? = null,
    val playlistId: String? = null,
    val authorAvatarUrl: String? = null,
    val episodeCountText: String? = null,
    val videoId: String? = null,
)

@Serializable
data class CachedArtist(val name: String, val id: String? = null)

@Serializable
data class CachedAlbum(val name: String, val id: String)

fun serializeArtistPage(
    sections: List<ArtistSection>,
    description: String?,
    subscriberCountText: String?,
    monthlyListenerCount: String?,
    isSubscribed: Boolean,
    artist: com.metrolist.innertube.models.ArtistItem? = null,
): String {
    val cached = CachedArtistPage(
        artist = artist?.let { CachedArtistItem(it.id, it.title, it.thumbnail, it.channelId, it.isProfile) },
        sections = sections.map { section ->
            CachedSection(
                title = section.title,
                items = section.items.map { item -> item.toCachedItem() },
                moreBrowseId = section.moreEndpoint?.browseId,
                moreParams = section.moreEndpoint?.params,
            )
        },
        description = description,
        subscriberCountText = subscriberCountText,
        monthlyListenerCount = monthlyListenerCount,
        isSubscribed = isSubscribed,
    )
    return json.encodeToString(cached)
}

fun deserializeArtistPage(jsonString: String): CachedArtistPage {
    return json.decodeFromString(jsonString)
}

fun CachedArtistPage.toArtistPage(): com.metrolist.innertube.pages.ArtistPage {
    return com.metrolist.innertube.pages.ArtistPage(
        artist = artist?.let { cached ->
            com.metrolist.innertube.models.ArtistItem(
                id = cached.id,
                title = cached.title,
                thumbnail = cached.thumbnail,
                channelId = cached.channelId,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
                isProfile = cached.isProfile,
            )
        } ?: com.metrolist.innertube.models.ArtistItem(
            id = "",
            title = "",
            thumbnail = null,
            shuffleEndpoint = null,
            radioEndpoint = null,
        ),
        sections = sections.map { section ->
            ArtistSection(
                title = section.title,
                items = section.items.map { it.toYTItem() },
                moreEndpoint = section.moreBrowseId?.let { browseId ->
                    com.metrolist.innertube.models.BrowseEndpoint(
                        browseId = browseId,
                        params = section.moreParams,
                    )
                },
            )
        },
        description = description,
        subscriberCountText = subscriberCountText,
        monthlyListenerCount = monthlyListenerCount,
        isSubscribed = isSubscribed,
    )
}

private fun CachedItem.toYTItem(): YTItem {
    return when (type) {
        "song" -> SongItem(
            id = id,
            title = title,
            thumbnail = thumbnail,
            explicit = explicit,
            artists = artists.map { com.metrolist.innertube.models.Artist(it.name, it.id) },
            album = album?.let { com.metrolist.innertube.models.Album(it.name, it.id) },
            duration = duration ?: 0,
            setVideoId = videoId,
        )
        "album" -> AlbumItem(
            browseId = browseId ?: id,
            playlistId = playlistId ?: id,
            id = id,
            title = title,
            thumbnail = thumbnail,
            explicit = explicit,
            artists = artists.map { com.metrolist.innertube.models.Artist(it.name, it.id) }.ifEmpty { null },
            year = year,
        )
        "playlist" -> PlaylistItem(
            id = id,
            title = title,
            thumbnail = thumbnail.takeIf { it.isNotBlank() },
            author = author?.let { com.metrolist.innertube.models.Artist(it.name, it.id) },
            songCountText = songCountText,
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null,
            authorAvatarUrl = authorAvatarUrl,
        )
        "artist" -> ArtistItem(
            id = id,
            title = title,
            thumbnail = thumbnail.takeIf { it.isNotBlank() },
            channelId = channelId,
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null,
        )
        "podcast" -> PodcastItem(
            id = id,
            title = title,
            thumbnail = thumbnail.takeIf { it.isNotBlank() },
            author = author?.let { com.metrolist.innertube.models.Artist(it.name, it.id) },
            episodeCountText = episodeCountText,
            playEndpoint = null,
            shuffleEndpoint = null,
            channelId = channelId,
        )
        "episode" -> EpisodeItem(
            id = id,
            title = title,
            thumbnail = thumbnail,
            explicit = explicit,
            author = author?.let { com.metrolist.innertube.models.Artist(it.name, it.id) },
            podcast = album?.let { com.metrolist.innertube.models.Album(it.name, it.id) },
            duration = duration,
            endpoint = null,
        )
        else -> SongItem(
            id = id,
            title = title,
            thumbnail = thumbnail,
            explicit = explicit,
            artists = artists.map { com.metrolist.innertube.models.Artist(it.name, it.id) },
            album = album?.let { com.metrolist.innertube.models.Album(it.name, it.id) },
            duration = duration ?: 0,
        )
    }
}

private fun YTItem.toCachedItem(): CachedItem {
    return when (this) {
        is SongItem -> CachedItem(
            id = id,
            title = title,
            thumbnail = thumbnail,
            type = "song",
            explicit = explicit,
            artists = artists.map { CachedArtist(it.name, it.id) },
            album = album?.let { CachedAlbum(it.name, it.id) },
            duration = duration,
            videoId = setVideoId,
        )
        is AlbumItem -> CachedItem(
            id = id,
            title = title,
            thumbnail = thumbnail,
            type = "album",
            explicit = explicit,
            artists = artists?.map { CachedArtist(it.name, it.id) } ?: emptyList(),
            year = year,
            browseId = browseId,
            playlistId = playlistId,
        )
        is PlaylistItem -> CachedItem(
            id = id,
            title = title,
            thumbnail = thumbnail ?: "",
            type = "playlist",
            author = author?.let { CachedArtist(it.name, it.id) },
            songCountText = songCountText,
            authorAvatarUrl = authorAvatarUrl,
        )
        is ArtistItem -> CachedItem(
            id = id,
            title = title,
            thumbnail = thumbnail ?: "",
            type = "artist",
            channelId = channelId,
        )
        is PodcastItem -> CachedItem(
            id = id,
            title = title,
            thumbnail = thumbnail ?: "",
            type = "podcast",
            author = author?.let { CachedArtist(it.name, it.id) },
            episodeCountText = episodeCountText,
            channelId = channelId,
        )
        is EpisodeItem -> CachedItem(
            id = id,
            title = title,
            thumbnail = thumbnail,
            type = "episode",
            explicit = explicit,
            author = author?.let { CachedArtist(it.name, it.id) },
            album = podcast?.let { CachedAlbum(it.name, it.id) },
            duration = duration,
        )
    }
}
