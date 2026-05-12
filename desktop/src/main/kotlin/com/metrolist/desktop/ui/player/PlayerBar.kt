package com.metrolist.desktop.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.DesktopPlayer
import com.metrolist.desktop.core.RepeatMode
import com.metrolist.desktop.core.rememberUrlImageBitmap
import androidx.compose.foundation.Image as ComposeImage

@Composable
fun PlayerBar(modifier: Modifier = Modifier) {
    val playerState by DesktopPlayer.state.collectAsState()
    val song = playerState.currentSong

    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        LinearProgressIndicator(
            progress = {
                if (playerState.duration > 0) playerState.position.toFloat() / playerState.duration.toFloat() else 0f
            },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            trackColor = MaterialTheme.colorScheme.outline,
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (song != null) {
                val thumbBitmap = rememberUrlImageBitmap(song.thumbnail)
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant).clickable {
                            DesktopPlayer.toggleNowPlaying()
                        },
                ) {
                    if (thumbBitmap != null) {
                        ComposeImage(thumbBitmap, null, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).clickable { DesktopPlayer.toggleNowPlaying() }) {
                    Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artists.joinToString(", ") { a -> a.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.outlineVariant))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("No song playing", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Select a song to play", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { DesktopPlayer.toggleShuffle() }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Toggle shuffle", tint = if (playerState.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { DesktopPlayer.previous() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track")
                }
                FilledIconButton(onClick = { DesktopPlayer.playPause() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = { DesktopPlayer.next() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next track")
                }
                IconButton(onClick = { DesktopPlayer.toggleRepeat() }) {
                    Icon(
                        if (playerState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Toggle repeat",
                        tint = if (playerState.repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { DesktopPlayer.toggleQueue() }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue")
                }
                IconButton(onClick = { DesktopPlayer.toggleNowPlaying() }) {
                    Icon(Icons.Default.Lyrics, contentDescription = "Lyrics")
                }
                var showVolumeSlider by remember { mutableStateOf(false) }
                IconButton(onClick = { showVolumeSlider = !showVolumeSlider }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume")
                }
                if (showVolumeSlider) {
                    Slider(
                        value = playerState.volume,
                        onValueChange = { DesktopPlayer.setVolume(it) },
                        modifier = Modifier.width(100.dp),
                    )
                }
            }
        }
    }
}
