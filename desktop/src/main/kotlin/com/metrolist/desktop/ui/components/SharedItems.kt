package com.metrolist.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.DesktopPlayer
import com.metrolist.desktop.core.NavigationManager
import com.metrolist.desktop.core.rememberUrlImageBitmap
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image as ComposeImage

@Composable
fun YTItemCard(item: YTItem, onClick: (() -> Unit)? = null) {
    Column(modifier = Modifier.width(160.dp).clickable {
        if (onClick != null) {
            onClick()
        } else {
            when (item) {
                is AlbumItem -> NavigationManager.navigateToAlbum(item.browseId)
                is ArtistItem -> NavigationManager.navigateToArtist(item.id)
                is PlaylistItem -> NavigationManager.navigateToPlaylist(item.id)
                is SongItem -> DesktopPlayer.play(item)
                else -> {}
            }
        }
    }) {
        val bitmap = rememberUrlImageBitmap(item.thumbnail)

        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                ComposeImage(
                    bitmap = bitmap,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)

        val subtitle = when (item) {
            is SongItem -> item.artists?.joinToString(", ") { it.name }
            is AlbumItem -> item.artists?.joinToString(", ") { it.name }
            is ArtistItem -> null
            is PlaylistItem -> item.author?.name
            else -> null
        }
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SongRow(index: Int, song: SongItem, onClick: () -> Unit) {
    var isLiked by remember { mutableStateOf(false) }
    var isLikingInProgress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$index", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(32.dp))
        val thumbBitmap = rememberUrlImageBitmap(song.thumbnail)
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (thumbBitmap != null) {
                ComposeImage(thumbBitmap, null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            song.artists?.let {
                Text(it.joinToString(", ") { a -> a.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        song.duration?.let {
            val m = it / 60
            val s = it % 60
            Text("%d:%02d".format(m, s), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = {
                isLikingInProgress = true
                scope.launch {
                    YouTube.likeVideo(song.id, !isLiked).onSuccess {
                        isLiked = !isLiked
                    }
                    isLikingInProgress = false
                }
            },
            enabled = !isLikingInProgress,
        ) {
            Icon(
                if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    HorizontalDivider()
}
