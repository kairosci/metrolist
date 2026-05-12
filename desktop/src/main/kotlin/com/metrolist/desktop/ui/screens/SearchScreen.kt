package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
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
import com.metrolist.innertube.pages.SearchResult
import kotlinx.coroutines.launch

private val MAX_HISTORY = 20

private data class SearchFilterOption(
    val label: String,
    val filter: YouTube.SearchFilter,
)

private val SEARCH_FILTERS = listOf(
    SearchFilterOption("All", YouTube.SearchFilter("all")),
    SearchFilterOption("Songs", YouTube.SearchFilter.FILTER_SONG),
    SearchFilterOption("Videos", YouTube.SearchFilter.FILTER_VIDEO),
    SearchFilterOption("Albums", YouTube.SearchFilter.FILTER_ALBUM),
    SearchFilterOption("Artists", YouTube.SearchFilter.FILTER_ARTIST),
    SearchFilterOption("Playlists", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST),
    SearchFilterOption("Podcasts", YouTube.SearchFilter.FILTER_PODCAST),
)

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<SearchResult?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchHistory by remember { mutableStateOf(mutableListOf<String>()) }
    var selectedFilterIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        item {
            Text("Search", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search YouTube Music") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Row {
                            IconButton(onClick = {
                                scope.launch {
                                    isSearching = true
                                    searchResults = YouTube.search(query, SEARCH_FILTERS[selectedFilterIndex].filter).getOrNull()
                                    isSearching = false
                                    if (query.isNotBlank() && !searchHistory.contains(query)) {
                                        searchHistory = (listOf(query) + searchHistory).take(MAX_HISTORY).toMutableList()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        scope.launch {
                            isSearching = true
                            searchResults = YouTube.search(query, SEARCH_FILTERS[selectedFilterIndex].filter).getOrNull()
                            isSearching = false
                            if (query.isNotBlank() && !searchHistory.contains(query)) {
                                searchHistory = (listOf(query) + searchHistory).take(MAX_HISTORY).toMutableList()
                            }
                        }
                    },
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SEARCH_FILTERS.size) { index ->
                    FilterChip(
                        selected = selectedFilterIndex == index,
                        onClick = { selectedFilterIndex = index },
                        label = { Text(SEARCH_FILTERS[index].label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        if (isSearching) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        val results = searchResults
        if (results != null && !isSearching) {
            if (results.items.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No results found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(results.items) { item ->
                when (item) {
                    is SongItem -> YTItemCard(item)
                    is AlbumItem -> YTItemCard(
                        item = item,
                        onClick = { NavigationManager.navigateToAlbum(item.browseId) },
                    )
                    is ArtistItem -> YTItemCard(
                        item = item,
                        onClick = { NavigationManager.navigateToArtist(item.id) },
                    )
                    is PlaylistItem -> YTItemCard(
                        item = item,
                        onClick = { NavigationManager.navigateToPlaylist(item.id) },
                    )
                    else -> YTItemCard(item)
                }
            }
        }

        if (query.isEmpty() && searchResults == null && !isSearching) {
            if (searchHistory.isNotEmpty()) {
                item {
                    Text("Recent searches", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }
                items(searchHistory) { historyItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            query = historyItem
                            scope.launch {
                                isSearching = true
                                searchResults = YouTube.search(historyItem, SEARCH_FILTERS[selectedFilterIndex].filter).getOrNull()
                                isSearching = false
                            }
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(historyItem, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
