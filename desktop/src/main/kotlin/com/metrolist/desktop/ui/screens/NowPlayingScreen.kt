package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.*
import com.metrolist.desktop.lyrics.DesktopLyricsHelper
import com.metrolist.desktop.ui.components.LyricsDisplay
import androidx.compose.foundation.Image as ComposeImage

@Composable
fun NowPlayingScreen(modifier: Modifier = Modifier) {
    val playerState by DesktopPlayer.state.collectAsState()
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
    ) {
        val song = playerState.currentSong
        if (song == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No song playing", style = MaterialTheme.typography.headlineMedium)
            }
            return
        }

        Column(
            modifier = Modifier.width(360.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val thumbBitmap = rememberUrlImageBitmap(song.thumbnail)
            Box(
                Modifier.size(300.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (thumbBitmap != null) {
                    ComposeImage(thumbBitmap, null, modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(song.artists.joinToString(", ") { a -> a.name }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Slider(
                    value = if (playerState.duration > 0) playerState.position.toFloat() / playerState.duration.toFloat() else 0f,
                    onValueChange = { DesktopPlayer.seekTo((it * playerState.duration).toLong()) },
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatTime(playerState.position), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(playerState.duration), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { DesktopPlayer.toggleShuffle() }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Toggle shuffle", tint = if (playerState.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { DesktopPlayer.previous() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.width(16.dp))
                FilledIconButton(onClick = { DesktopPlayer.playPause() }, modifier = Modifier.size(64.dp)) {
                    Icon(if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playerState.isPlaying) "Pause" else "Play", modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { DesktopPlayer.next() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { DesktopPlayer.toggleRepeat() }) {
                    val icon = when (playerState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    Icon(icon, contentDescription = "Toggle repeat", tint = if (playerState.repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.VolumeUp, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = playerState.volume,
                    onValueChange = { DesktopPlayer.setVolume(it) },
                    modifier = Modifier.width(120.dp),
                )
            }
        }

        Spacer(Modifier.width(32.dp))

        VerticalDivider()

        Spacer(Modifier.width(32.dp))

        LyricsDisplay(
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
