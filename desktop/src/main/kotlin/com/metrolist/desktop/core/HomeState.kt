package com.metrolist.desktop.core

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.HomePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class HomeSection(
    val title: String,
    val items: List<YTItem>,
)

data class HomeUiState(
    val sections: List<HomeSection> = emptyList(),
    val chips: List<HomePage.Chip> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

object HomeState {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    fun load() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            supervisorScope {
                YouTube.home()
                    .onSuccess { page ->
                        val sections = page.sections.map { section ->
                            HomeSection(
                                title = section.title,
                                items = section.items,
                            )
                        }
                        _state.value = HomeUiState(
                            sections = sections,
                            chips = page.chips ?: emptyList(),
                            isLoading = false,
                        )
                    }
                    .onFailure { e ->
                        _state.value = HomeUiState(isLoading = false, error = e.message)
                    }
            }
        }
    }
}
