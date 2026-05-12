package com.metrolist.desktop.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.LinkedHashMap
import java.util.prefs.Preferences

private const val MAX_LYRICS_FETCH_MS = 25000L
private const val PER_PROVIDER_TIMEOUT_MS = 8000L
private const val MAX_CACHE_SIZE = 10
private const val LYRICS_NOT_FOUND = "LYRICS_NOT_FOUND"
private const val PREFS_KEY_ORDER = "lyrics_provider_order"

data class LyricsState(
    val lyrics: String? = null,
    val provider: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

object DesktopLyricsHelper {
    private val prefs = Preferences.userNodeForPackage(DesktopLyricsHelper::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _lyricsState = MutableStateFlow(LyricsState())
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private var currentFetchJob: Job? = null
    private val cache = object : LinkedHashMap<String, String>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
            this.size > MAX_CACHE_SIZE
    }

    fun getProviderOrder(): List<String> {
        val saved = prefs.get(PREFS_KEY_ORDER, "")
        return LyricsProviderRegistry.deserializeProviderOrder(saved)
    }

    fun setProviderOrder(order: List<String>) {
        prefs.put(PREFS_KEY_ORDER, LyricsProviderRegistry.serializeProviderOrder(order))
    }

    fun fetchLyrics(
        songId: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ) {
        currentFetchJob?.cancel()
        currentFetchJob = scope.launch {
            _lyricsState.value = LyricsState(isLoading = true)

            val cacheKey = "$songId-$title-$artist"
            synchronized(cache) {
                cache[cacheKey]?.let { cached ->
                    _lyricsState.value = LyricsState(lyrics = cached, provider = "cache")
                    return@launch
                }
            }

            val cleanedTitle = LyricsUtils.cleanTitleForSearch(title)
            val orderedProviders = getProviderOrder()
            val enabledProviders = orderedProviders.mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
                .filter { it.isEnabled() }

            val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
                for (provider in enabledProviders) {
                    val providerResult = try {
                        withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                            provider.getLyrics(songId, cleanedTitle, artist, duration, album)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }

                    if (providerResult != null && providerResult.isSuccess) {
                        val filtered = LyricsUtils.filterLyricsCreditLines(providerResult.getOrThrow())
                        synchronized(cache) { cache[cacheKey] = filtered }
                        _lyricsState.value = LyricsState(lyrics = filtered, provider = provider.name)
                        return@withTimeoutOrNull
                    }
                }
                _lyricsState.value = LyricsState(error = "No lyrics found")
            }

            if (result == null) {
                _lyricsState.value = LyricsState(error = "Request timed out")
            }
        }
    }
}
