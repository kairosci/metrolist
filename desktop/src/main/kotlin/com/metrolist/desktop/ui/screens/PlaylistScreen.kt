package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.*
import com.metrolist.desktop.ui.components.SongRow
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image as ComposeImage

data class PlaylistUiState(
    val isLoading: Boolean = true,
    val playlist: PlaylistItem? = null,
    val songs: List<SongItem> = emptyList(),
    val error: String? = null,
)

@Composable
fun PlaylistScreen(playlistId: String, modifier: Modifier = Modifier) {
    var uiState by remember { mutableStateOf(PlaylistUiState()) }

    LaunchedEffect(playlistId) {
        uiState = uiState.copy(isLoading = true, error = null)
        val fullId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        YouTube.playlist(fullId).onSuccess { page ->
            val allSongs = mutableListOf<SongItem>()
            allSongs.addAll(page.songs)
            var continuation = page.continuation
            while (continuation != null) {
                val contPage = YouTube.playlistContinuation(continuation).getOrNull() ?: break
                allSongs.addAll(contPage.songs)
                continuation = contPage.continuation
            }
            uiState = PlaylistUiState(isLoading = false, playlist = page.playlist, songs = allSongs)
        }.onFailure { e ->
            uiState = PlaylistUiState(isLoading = false, error = e.message)
        }
    }

    val playlist = uiState.playlist

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { NavigationManager.goBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(playlist?.title ?: "", style = MaterialTheme.typography.titleLarge)
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error!!, color = MaterialTheme.colorScheme.error) }
        } else if (playlist != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                val thumbBitmap = rememberUrlImageBitmap(playlist.thumbnail)
                Box(
                    Modifier.size(200.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (thumbBitmap != null) {
                        ComposeImage(thumbBitmap, null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(playlist.title, style = MaterialTheme.typography.headlineMedium)
                    playlist.author?.let {
                        Text(it.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${uiState.songs.size} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (uiState.songs.isNotEmpty()) DesktopPlayer.playQueue(uiState.songs, 0) }) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play")
                        }
                        OutlinedButton(onClick = {
                            DesktopPlayer.playQueue(uiState.songs, 0)
                            DesktopPlayer.toggleShuffle()
                        }) {
                            Icon(Icons.Default.Shuffle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(uiState.songs) { index, song ->
                    SongRow(index + 1, song, onClick = { DesktopPlayer.playQueue(uiState.songs, index) })
                }
            }
        }
    }
}
