package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.DesktopPlayer
import com.metrolist.desktop.ui.components.SongRow
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.HistoryPage

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    var page by remember { mutableStateOf<HistoryPage?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = YouTube.musicHistory().getOrNull()
        page = result
        isLoading = false
        error = result == null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        item {
            Text("History", style = MaterialTheme.typography.headlineLarge)
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
                        Text("Could not load history", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text("Make sure you are logged in.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            val historyPage = page
            val sections = historyPage?.sections
            if (!sections.isNullOrEmpty()) {
                sections.forEach { section ->
                    item {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(section.songs) { song ->
                        SongRow(
                            index = section.songs.indexOf(song) + 1,
                            song = song,
                            onClick = { DesktopPlayer.play(song) },
                        )
                    }
                }
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No history found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("Listen to some music to see your history here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}
