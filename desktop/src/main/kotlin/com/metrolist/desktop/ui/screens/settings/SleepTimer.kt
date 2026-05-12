package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.DesktopPlayer
import kotlinx.coroutines.*

object SleepTimerState {
    var remainingSeconds by mutableStateOf(0)
    var isActive by mutableStateOf(false)
    private var job: Job? = null

    fun start(minutes: Int, scope: CoroutineScope) {
        remainingSeconds = minutes * 60
        isActive = true
        job?.cancel()
        job = scope.launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            this@SleepTimerState.isActive = false
            DesktopPlayer.playPause()
        }
    }

    fun stop() {
        job?.cancel()
        isActive = false
        remainingSeconds = 0
    }
}

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timer, null) },
        title = { Text("Sleep Timer") },
        text = {
            Column {
                if (SleepTimerState.isActive) {
                    val m = SleepTimerState.remainingSeconds / 60
                    val s = SleepTimerState.remainingSeconds % 60
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Timer Active", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("%d:%02d".format(m, s), style = MaterialTheme.typography.displaySmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { SleepTimerState.stop() }) {
                        Text("Stop Timer")
                    }
                } else {
                    Text("Stop playback after:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    listOf(5, 15, 30, 60).forEach { minutes ->
                        Button(
                            onClick = {
                                SleepTimerState.start(minutes, scope)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text("$minutes minutes")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
