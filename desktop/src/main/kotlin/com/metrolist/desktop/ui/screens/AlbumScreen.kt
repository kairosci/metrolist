package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.*
import com.metrolist.desktop.ui.components.SongRow
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image as ComposeImage

data class AlbumUiState(
    val isLoading: Boolean = true,
    val album: AlbumItem? = null,
    val songs: List<SongItem> = emptyList(),
    val error: String? = null,
)

@Composable
fun AlbumScreen(browseId: String, modifier: Modifier = Modifier) {
    var uiState by remember { mutableStateOf(AlbumUiState()) }

    LaunchedEffect(browseId) {
        uiState = uiState.copy(isLoading = true, error = null)
        YouTube.album(browseId).onSuccess { page ->
            val songs = YouTube.albumSongs(page.album.playlistId).getOrDefault(page.songs)
            uiState = AlbumUiState(isLoading = false, album = page.album, songs = songs)
        }.onFailure { e ->
            uiState = AlbumUiState(isLoading = false, error = e.message)
        }
    }

    val album = uiState.album

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { NavigationManager.goBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(album?.title ?: "", style = MaterialTheme.typography.titleLarge)
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (album != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                val thumbBitmap = rememberUrlImageBitmap(album.thumbnail)
                Box(
                    Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (thumbBitmap != null) {
                        ComposeImage(thumbBitmap, null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(album.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    album.artists?.forEach { artist ->
                        Text(artist.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    album.year?.let { Text("$it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        if (uiState.songs.isNotEmpty()) DesktopPlayer.playQueue(uiState.songs, 0)
                    }) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(uiState.songs) { index, song ->
                    SongRow(index = index + 1, song = song, onClick = {
                        DesktopPlayer.playQueue(uiState.songs, index)
                    })
                }
            }
        }
    }
}
