package com.metrolist.music.video

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.metrolist.innertube.models.Thumbnail

data class VideoMetadata(
    val videoId: String,
    val title: String,
    val author: String? = null,
    val channelId: String? = null,
    val durationSeconds: Long = 0,
    val thumbnailUrl: String? = null,
    val viewCount: Long? = null,
    val streamUrl: String? = null,
    val streamExpiresInSeconds: Int? = null,
) {
    fun toMediaItem(): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(videoId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(author)
                    .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
                    .build()
            )
        streamUrl?.let { builder.setUri(it) }
        return builder.build()
    }

    companion object {
        fun fromMediaItem(mediaItem: MediaItem): VideoMetadata {
            val metadata = mediaItem.mediaMetadata
            return VideoMetadata(
                videoId = mediaItem.mediaId,
                title = metadata.title?.toString() ?: "Unknown",
                author = metadata.artist?.toString(),
                thumbnailUrl = metadata.artworkUri?.toString(),
            )
        }
    }
}
