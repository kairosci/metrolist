package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.ui.components.YTItemCard
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import kotlinx.coroutines.launch

@Composable
fun NewReleaseScreen(modifier: Modifier = Modifier) {
    var albums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        val result = YouTube.newReleaseAlbums().getOrNull()
        albums = result.orEmpty()
        isLoading = false
        error = result == null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        item {
            Text("New Releases", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (error) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Could not load new releases", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { error = false; isLoading = true }) { Text("Retry") }
                    }
                }
            }
        } else if (albums.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("No new releases available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // Display albums in grid (4 columns)
            val chunkedAlbums = albums.chunked(4)
            items(chunkedAlbums) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { album ->
                        Box(Modifier.weight(1f)) {
                            YTItemCard(album)
                        }
                    }
                    // Fill remaining space
                    repeat(4 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}
