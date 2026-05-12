package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.MoodAndGenres

@Composable
fun MoodAndGenresScreen(modifier: Modifier = Modifier) {
    var sections by remember { mutableStateOf<List<MoodAndGenres>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = YouTube.moodAndGenres().getOrNull()
        sections = result.orEmpty()
        isLoading = false
        error = result == null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Moods & Genres", style = MaterialTheme.typography.headlineLarge)
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
                        Text("Could not load moods & genres", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { error = false; isLoading = true }) { Text("Retry") }
                    }
                }
            }
        } else {
            sections.forEach { section ->
                item {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    ) {
                        items(section.items) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(
                                        ((item.stripeColor shr 24) and 0xFF).toInt(),
                                        ((item.stripeColor shr 16) and 0xFF).toInt(),
                                        ((item.stripeColor shr 8) and 0xFF).toInt(),
                                        0xFF,
                                    ),
                                ),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    contentAlignment = Alignment.BottomStart,
                                ) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}
