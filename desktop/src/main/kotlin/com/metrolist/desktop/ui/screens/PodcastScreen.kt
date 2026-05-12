package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.*
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
import com.metrolist.innertube.models.SongItem
import androidx.compose.foundation.Image as ComposeImage

@Composable
fun PodcastScreen(
    browseId: String,
    modifier: Modifier = Modifier,
) {
    var podcastTitle by remember { mutableStateOf("") }
    var episodes by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(browseId) {
        isLoading = true
        val result = YouTube.browse(browseId, null).getOrNull()
        if (result != null) {
            podcastTitle = result.items.firstOrNull()?.title ?: ""
            episodes = result.items.filterIsInstance<SongItem>().filter { it.isEpisode }
        }
        isLoading = false
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(24.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { NavigationManager.goBack() }) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text("Podcast", style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                Text(podcastTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text("Episodes", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (episodes.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Podcasts, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No episodes found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(episodes) { episode ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        DesktopPlayer.play(episode)
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val thumb = rememberUrlImageBitmap(episode.thumbnail)
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (thumb != null) {
                                ComposeImage(thumb, null, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(episode.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            episode.album?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        episode.duration?.let {
                            val m = it / 60; val s = it % 60
                            Text("%d:%02d".format(m, s), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}
