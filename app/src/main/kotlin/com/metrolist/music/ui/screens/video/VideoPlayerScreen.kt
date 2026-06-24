package com.metrolist.music.ui.screens.video

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.R
import com.metrolist.music.playback.video.VideoPlayerConnection
import com.metrolist.music.playback.video.rememberVideoPlayerConnection
import com.metrolist.music.video.VideoMetadata
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    onNavigateBack: () -> Unit,
    viewModel: VideoViewModel = hiltViewModel(),
) {
    val connection = rememberVideoPlayerConnection()

    val isPlaying by connection.isPlaying.collectAsStateWithLifecycle(false)
    val playbackState by connection.playbackState.collectAsStateWithLifecycle(Player.STATE_IDLE)
    val currentVideo by connection.currentVideo.collectAsStateWithLifecycle(null)

    var isControlsVisible by rememberSaveable { mutableStateOf(true) }

    // Auto-hide controls after 3.5s when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            delay(3500)
            isControlsVisible = false
        }
    }

    // Start playback when screen opens
    LaunchedEffect(videoId) {
        connection.playVideo(VideoMetadata(videoId = videoId, title = ""))
    }

    // Full black background — completely separate from the music player UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                isControlsVisible = !isControlsVisible
            },
        contentAlignment = Alignment.Center,
    ) {
        // ── Video surface ── always 16:9, centered in the black frame
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { surfaceView ->
                    surfaceView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    connection.getPlayer().setVideoSurfaceView(surfaceView)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )

        // ── Buffering spinner ──
        if (playbackState == Player.STATE_BUFFERING) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }

        // ── Overlay controls ──
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            VideoControlsOverlay(
                currentVideo = currentVideo,
                isPlaying = isPlaying,
                connection = connection,
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

@Composable
private fun VideoControlsOverlay(
    currentVideo: VideoMetadata?,
    isPlaying: Boolean,
    connection: VideoPlayerConnection,
    onNavigateBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
    ) {
        // ── Top bar: back + title ──
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentVideo?.title ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                    )
                    val authorText = currentVideo?.author
                    if (!authorText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = authorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // ── Center playback controls ──
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { connection.skipPrevious() }) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            IconButton(onClick = {
                connection.seekTo(connection.getPlayer().currentPosition - 10_000L)
            }) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.replay),
                    contentDescription = "Rewind 10s",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            // Main play/pause button — larger, with circle background
            IconButton(
                onClick = { connection.togglePlayPause() },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.pause else R.drawable.play,
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }

            IconButton(onClick = {
                connection.seekTo(connection.getPlayer().currentPosition + 10_000L)
            }) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.fast_forward),
                    contentDescription = "Forward 10s",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            IconButton(onClick = { connection.skipNext() }) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        // ── Bottom seek bar ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            VideoSeekBar(connection = connection)
        }
    }
}

@Composable
private fun VideoSeekBar(connection: VideoPlayerConnection) {
    val player = remember { connection.getPlayer() }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!isSeeking && player.isPlaying) {
                currentPosition = player.currentPosition
            }
            delay(250)
        }
    }

    val duration = if (player.duration > 0) player.duration else 1L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Slider(
            value = if (isSeeking) seekPosition else (currentPosition.toFloat() / duration),
            onValueChange = { value ->
                isSeeking = true
                seekPosition = value
            },
            onValueChangeFinished = {
                connection.seekTo((seekPosition * duration).toLong())
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(if (isSeeking) (seekPosition * duration).toLong() else currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
