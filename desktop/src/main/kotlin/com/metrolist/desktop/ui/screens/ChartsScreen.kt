package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.ui.components.SongRow
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.innertube.models.SongItem

@Composable
fun ChartsScreen(modifier: Modifier = Modifier) {
    var page by remember { mutableStateOf<ChartsPage?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = YouTube.getChartsPage().getOrNull()
        page = result
        isLoading = false
        error = result == null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Charts", style = MaterialTheme.typography.headlineLarge)
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
                        Text("Could not load charts", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { error = false; isLoading = true }) { Text("Retry") }
                    }
                }
            }
        } else {
            val chartsPage = page
            if (chartsPage != null) {
                chartsPage.sections.forEach { section ->
                    item {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                    }

                    section.items.filterIsInstance<SongItem>().forEachIndexed { index, song ->
                        item {
                            SongRow(index = index + 1, song = song, onClick = {
                                com.metrolist.desktop.core.DesktopPlayer.playQueue(
                                    section.items.filterIsInstance<SongItem>(),
                                    index,
                                )
                            })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}
