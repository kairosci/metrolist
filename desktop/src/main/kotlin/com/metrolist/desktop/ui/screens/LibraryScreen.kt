package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.NavigationManager
import com.metrolist.desktop.ui.components.YTItemCard
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.LibraryPage
import kotlinx.coroutines.launch

private data class LibraryTab(
    val title: String,
    val browseId: String,
    val icon: @Composable () -> Unit,
    var page: LibraryPage? = null,
    var isLoading: Boolean = false,
)

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    val tabs = remember {
        mutableStateListOf(
            LibraryTab("Songs", "FEmusic_library_corpus_track", { Icon(Icons.Default.MusicNote, null) }),
            LibraryTab("Albums", "FEmusic_library_corpus_album", { Icon(Icons.Default.Album, null) }),
            LibraryTab("Artists", "FEmusic_library_corpus_artist", { Icon(Icons.Default.Person, null) }),
            LibraryTab("Playlists", "FEmusic_liked_playlists", { Icon(Icons.Default.PlaylistPlay, null) }),
            LibraryTab("Mix", "FEmusic_liked_playlists_mixes", { Icon(Icons.Default.QueueMusic, null) }),
            LibraryTab("Podcasts", "FEmusic_library_corpus_podcast", { Icon(Icons.Default.Podcasts, null) }),
        )
    }

    var selectedTabIndex by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTabIndex) {
        val tab = tabs.getOrNull(selectedTabIndex) ?: return@LaunchedEffect
        if (tab.page == null && !tab.isLoading) {
            tabs[selectedTabIndex] = tab.copy(isLoading = true)
            val result = YouTube.library(tab.browseId).getOrNull()
            tabs[selectedTabIndex] = tab.copy(page = result, isLoading = false)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        Text("Library", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs.size) { index ->
                FilterChip(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    label = { Text(tabs[index].title) },
                    leadingIcon = tabs[index].icon,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val currentTab = tabs.getOrNull(selectedTabIndex)

        if (currentTab == null) return

        if (currentTab.isLoading) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (currentTab.page == null) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Could not load library", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            tabs[selectedTabIndex] = currentTab.copy(isLoading = true)
                            val result = YouTube.library(currentTab.browseId).getOrNull()
                            tabs[selectedTabIndex] = currentTab.copy(page = result, isLoading = false)
                        }
                    }) { Text("Retry") }
                }
            }
        } else {
            val page = currentTab.page
            if (page == null) return
            if (page.items.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("No items found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(page.items) { item ->
                        when (item) {
                            is SongItem -> YTItemCard(item)
                            is AlbumItem -> YTItemCard(item, onClick = { NavigationManager.navigateToAlbum(item.browseId) })
                            is ArtistItem -> YTItemCard(item, onClick = { NavigationManager.navigateToArtist(item.id) })
                            is PlaylistItem -> YTItemCard(item, onClick = { NavigationManager.navigateToPlaylist(item.id) })
                            else -> YTItemCard(item)
                        }
                    }
                }
            }
        }
    }
}
