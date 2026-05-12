package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.*
import com.metrolist.desktop.ui.components.YTItemCard
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image as ComposeImage

data class ArtistUiState(
    val isLoading: Boolean = true,
    val artist: ArtistItem? = null,
    val sections: List<com.metrolist.innertube.pages.ArtistSection> = emptyList(),
    val description: String? = null,
    val error: String? = null,
)

@Composable
fun ArtistScreen(browseId: String, modifier: Modifier = Modifier) {
    var uiState by remember { mutableStateOf(ArtistUiState()) }

    LaunchedEffect(browseId) {
        uiState = uiState.copy(isLoading = true, error = null)
        YouTube.artist(browseId).onSuccess { page ->
            uiState = ArtistUiState(
                isLoading = false,
                artist = page.artist,
                sections = page.sections,
                description = page.description,
            )
        }.onFailure { e ->
            uiState = ArtistUiState(isLoading = false, error = e.message)
        }
    }

    val artist = uiState.artist

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { NavigationManager.goBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(artist?.title ?: "", style = MaterialTheme.typography.titleLarge)
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error!!, color = MaterialTheme.colorScheme.error) }
        } else if (artist != null) {
            val thumbBitmap = rememberUrlImageBitmap(artist.thumbnail)

            Box(
                Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbBitmap != null) {
                    ComposeImage(thumbBitmap, null, modifier = Modifier.fillMaxSize())
                }
                Text(artist.title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                val songs = uiState.sections.flatMap { section -> section.items.filterIsInstance<SongItem>() }
                if (songs.isNotEmpty()) DesktopPlayer.playQueue(songs, 0)
            }) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Play")
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                uiState.sections.forEach { section ->
                    if (section.items.isNotEmpty()) {
                        item {
                            Text(section.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(section.items) { item -> YTItemCard(item) }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}
