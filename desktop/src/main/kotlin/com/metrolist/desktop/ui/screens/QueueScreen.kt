package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.*
import com.metrolist.innertube.models.SongItem
import androidx.compose.foundation.Image as ComposeImage

@Composable
fun QueueScreen(modifier: Modifier = Modifier) {
    val playerState by DesktopPlayer.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Queue", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { DesktopPlayer.toggleQueue() }) {
                Icon(Icons.Default.Clear, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Now Playing", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        playerState.currentSong?.let { song ->
            QueueItem(song, isCurrent = true, onPlay = { }, onRemove = null)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        Text("Up Next", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (playerState.queue.isEmpty()) {
            Text("No items in queue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(playerState.queue) { index, song ->
                    QueueItem(
                        song, 
                        isCurrent = false, 
                        onPlay = {
                            DesktopPlayer.playQueue(playerState.queue, index)
                        },
                        onRemove = {
                            DesktopPlayer.removeFromQueue(index)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { DesktopPlayer.clearQueue() }) {
                Text("Clear All")
            }
        }
    }
}

@Composable
private fun QueueItem(song: SongItem, isCurrent: Boolean, onPlay: () -> Unit, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            song.artists?.let {
                Text(it.joinToString(", ") { a -> a.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
            }
        }
    }
}
