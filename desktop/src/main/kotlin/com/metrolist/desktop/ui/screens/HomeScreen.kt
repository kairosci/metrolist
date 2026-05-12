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
import com.metrolist.desktop.core.HomeState
import com.metrolist.desktop.core.NavigationManager
import com.metrolist.desktop.ui.components.YTItemCard

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val uiState by HomeState.state.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.sections.isEmpty() && !uiState.isLoading) {
            HomeState.load()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text("Home", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("Welcome to Metrolist Desktop", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (uiState.error != null) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Could not load home content", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { HomeState.load() }) { Text("Retry") }
                    }
                }
            }
        }

        if (!uiState.isLoading && uiState.error == null && uiState.sections.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("No content available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        uiState.sections.forEach { section ->
            item {
                Column {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(section.items) { item -> YTItemCard(item) }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Discover", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { NavigationManager.navigateToExplore() }, label = { Text("Explore") })
                FilterChip(selected = false, onClick = { NavigationManager.navigateToCharts() }, label = { Text("Charts") })
                FilterChip(selected = false, onClick = { NavigationManager.navigateToNewReleases() }, label = { Text("New Releases") })
                FilterChip(selected = false, onClick = { NavigationManager.navigateToMoodAndGenres() }, label = { Text("Moods & Genres") })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { NavigationManager.navigateToHistory() }, label = { Text("History") })
                FilterChip(selected = false, onClick = { NavigationManager.navigateToListenTogether() }, label = { Text("Listen Together") })
                FilterChip(selected = false, onClick = { NavigationManager.navigateToEqualizer() }, label = { Text("Equalizer") })
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}
