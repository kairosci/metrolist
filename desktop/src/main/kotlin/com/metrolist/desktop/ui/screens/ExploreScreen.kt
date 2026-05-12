package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.NavigationManager
import com.metrolist.desktop.ui.components.YTItemCard
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.MoodAndGenres

@Composable
fun ExploreScreen(modifier: Modifier = Modifier) {
    var page by remember { mutableStateOf<ExplorePage?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = YouTube.explore().getOrNull()
        page = result
        isLoading = false
        error = result == null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Explore", style = MaterialTheme.typography.headlineLarge)
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
                        Text("Could not load explore page", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            // Re-trigger by changing state
                        }) { Text("Retry") }
                    }
                }
            }
        } else {
            val explorePage = page
            if (explorePage != null) {
                if (explorePage.newReleaseAlbums.isNotEmpty()) {
                    item {
                        Column {
                            Text("New Releases", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(explorePage.newReleaseAlbums) { album ->
                                    YTItemCard(album)
                                }
                            }
                        }
                    }
                }

                item {
                    Button(onClick = { NavigationManager.navigateToNewReleases() }) {
                        Text("View all new releases")
                    }
                }

                if (explorePage.moodAndGenres.isNotEmpty()) {
                    item {
                        Column {
                            Text("Moods & Genres", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(explorePage.moodAndGenres) { mood ->
                                    Card(
                                        modifier = Modifier.width(140.dp),
                                        onClick = {
                                            val browseId = mood.endpoint.browseId
                                            NavigationManager.navigateToMoodAndGenres()
                                        },
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(mood.title, style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(onClick = { NavigationManager.navigateToMoodAndGenres() }) {
                        Text("View all moods & genres")
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}
