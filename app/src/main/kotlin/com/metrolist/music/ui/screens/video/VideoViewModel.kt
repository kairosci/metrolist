package com.metrolist.music.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.video.VideoMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoHomeUiState(
    val sections: List<VideoSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class VideoSection(
    val title: String,
    val videos: List<VideoMetadata>,
)

data class VideoSearchUiState(
    val query: String = "",
    val results: List<VideoMetadata> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class VideoViewModel @Inject constructor() : ViewModel() {

    private val _homeState = MutableStateFlow(VideoHomeUiState())
    val homeState: StateFlow<VideoHomeUiState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(VideoSearchUiState())
    val searchState: StateFlow<VideoSearchUiState> = _searchState.asStateFlow()

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _homeState.value = _homeState.value.copy(isLoading = true, error = null)

            val sections = mutableListOf<VideoSection>()

            val browseResult = YouTube.browse("FEmusic_moods_and_genres", null)
            browseResult.onSuccess { response ->
                response.items.forEach { item ->
                    val videoItems = item.items.filterIsInstance<SongItem>().filter { it.isVideoSong }
                    if (videoItems.isNotEmpty()) {
                        sections.add(
                            VideoSection(
                                title = item.title ?: "Music Videos",
                                videos = videoItems.map { it.toVideoMetadata() }
                            )
                        )
                    }
                }
            }

            if (sections.isEmpty()) {
                searchTrendingVideos()
                return@launch
            }

            _homeState.value = VideoHomeUiState(sections = sections, isLoading = false)
        }
    }

    private suspend fun searchTrendingVideos() {
        val sections = mutableListOf<VideoSection>()
        val trendingSearches = listOf(
            "trending music videos",
            "popular music videos",
            "new music videos",
        )
        for (query in trendingSearches) {
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO)
            result.onSuccess { searchResult ->
                val videos = searchResult.items
                    .filterIsInstance<SongItem>()
                    .filter { it.isVideoSong }
                    .map { it.toVideoMetadata() }
                if (videos.isNotEmpty()) {
                    sections.add(VideoSection(title = query.replaceFirstChar { it.uppercase() }, videos = videos))
                }
            }
        }
        _homeState.value = VideoHomeUiState(sections = sections, isLoading = false)
    }

    fun searchVideos(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(query = query, isLoading = true)

            val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO)
            val results = searchResult.getOrNull()?.items
                ?.filterIsInstance<SongItem>()
                ?.filter { it.isVideoSong }
                ?.map { it.toVideoMetadata() }
                ?: emptyList()

            _searchState.value = _searchState.value.copy(results = results, isLoading = false)
        }
    }

    fun clearSearch() {
        _searchState.value = VideoSearchUiState()
    }

    private fun SongItem.toVideoMetadata() = VideoMetadata(
        videoId = id,
        title = title,
        author = artists.firstOrNull()?.name,
        channelId = artists.firstOrNull()?.id,
        durationSeconds = duration?.toLong() ?: 0,
        thumbnailUrl = thumbnail,
    )
}
