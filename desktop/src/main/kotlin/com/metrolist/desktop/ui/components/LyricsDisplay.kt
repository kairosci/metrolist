package com.metrolist.desktop.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.desktop.core.DesktopPlayer
import com.metrolist.desktop.lyrics.DesktopLyricsHelper
import com.metrolist.desktop.lyrics.LyricsEntry
import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.lyrics.LyricsUtils
import com.metrolist.desktop.lyrics.lyricsTextLooksSynced

@Composable
fun LyricsDisplay(
    modifier: Modifier = Modifier,
) {
    val playerState by DesktopPlayer.state.collectAsState()
    val lyricsState by DesktopLyricsHelper.lyricsState.collectAsState()

    val currentSong = playerState.currentSong

    LaunchedEffect(currentSong) {
        if (currentSong != null) {
            DesktopLyricsHelper.fetchLyrics(
                songId = currentSong.id,
                title = currentSong.title,
                artist = currentSong.artists.joinToString(", ") { it.name },
                duration = currentSong.duration ?: -1,
                album = null,
            )
        }
    }

    when {
        lyricsState.isLoading -> LoadingView(modifier)
        lyricsState.error != null -> ErrorView(lyricsState.error ?: "Unknown error", modifier)
        lyricsState.lyrics != null -> {
            val rawLyrics = lyricsState.lyrics ?: ""
            if (lyricsTextLooksSynced(rawLyrics)) {
                SyncedLyricsView(
                    rawLyrics = rawLyrics,
                    provider = lyricsState.provider,
                    currentPosition = playerState.position,
                    onSeek = { timeMs -> DesktopPlayer.seekTo(timeMs) },
                    modifier = modifier,
                )
            } else {
                UnsyncedLyricsView(
                    text = rawLyrics,
                    provider = lyricsState.provider,
                    modifier = modifier,
                )
            }
        }
        currentSong != null && lyricsState.lyrics == null && !lyricsState.isLoading -> {
            ErrorView("No lyrics available", modifier)
        }
        else -> LoadingView(modifier)
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Loading lyrics...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorView(error: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UnsyncedLyricsView(
    text: String,
    provider: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (provider != null) {
            Text(
                text = "Lyrics from $provider",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SyncedLyricsView(
    rawLyrics: String,
    provider: String?,
    currentPosition: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsedLines = remember(rawLyrics) { LyricsUtils.parseLyrics(rawLyrics) }
    val listState = rememberLazyListState()

    val activeLines = remember(parsedLines, currentPosition) {
        LyricsUtils.findActiveLineIndices(parsedLines, currentPosition)
    }

    val currentLineIndex = remember(parsedLines, currentPosition) {
        LyricsUtils.findCurrentLineIndex(parsedLines, currentPosition)
    }

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && currentLineIndex < parsedLines.size) {
            listState.animateScrollToItem(currentLineIndex.coerceAtLeast(0))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (provider != null) {
            Text(
                text = "Lyrics from $provider",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (parsedLines.isEmpty()) {
            UnsyncedLyricsView(text = rawLyrics, provider = provider, modifier = modifier)
            return
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(parsedLines) { index, entry ->
                if (entry == LyricsEntry.HEAD_LYRICS_ENTRY) return@itemsIndexed
                SyncedLyricLine(
                    entry = entry,
                    isActive = index in activeLines,
                    isCurrent = index == currentLineIndex,
                    onClick = { onSeek(entry.time) },
                )
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun SyncedLyricLine(
    entry: LyricsEntry,
    isActive: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val targetAlpha = if (isActive) 1f else 0.45f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "lineAlpha",
    )

    val targetTextSize = if (isCurrent) 20f else 16f
    val textSize by animateFloatAsState(
        targetValue = targetTextSize,
        animationSpec = tween(durationMillis = 300),
        label = "lineTextSize",
    )

    val targetWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

    val textColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry.words != null && isActive) {
            WordKaraokeView(
                words = entry.words,
                position = DesktopPlayer.state.value.position,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = entry.text,
                fontSize = textSize.sp,
                fontWeight = targetWeight,
                color = textColor.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WordKaraokeView(
    words: List<com.metrolist.desktop.lyrics.WordTimestamp>,
    position: Long,
    modifier: Modifier = Modifier,
) {
    val positionSec = position / 1000.0

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        words.forEach { word ->
            val wordStartSec = word.startTime
            val wordEndSec = word.endTime
            val isWordActive = positionSec >= wordStartSec && positionSec < wordEndSec
            val isWordPast = positionSec >= wordEndSec

            val color = when {
                isWordActive -> MaterialTheme.colorScheme.primary
                isWordPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            }

            val fontWeight = if (isWordActive) FontWeight.Bold else FontWeight.Normal

            Text(
                text = word.text + if (word.hasTrailingSpace) " " else "",
                fontSize = 20.sp,
                fontWeight = fontWeight,
                color = color,
            )
        }
    }
}
