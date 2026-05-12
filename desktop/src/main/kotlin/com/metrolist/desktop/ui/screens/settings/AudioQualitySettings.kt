@file:OptIn(ExperimentalMaterial3Api::class)

package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.audio.AudioQuality
import com.metrolist.desktop.core.DesktopPlayer

@Composable
fun AudioQualitySettingsDialog(onDismiss: () -> Unit) {
    var selectedQuality by remember { mutableStateOf(DesktopPlayer.state.value.audioQuality) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.VolumeUp, null) },
        title = { Text("Audio Quality") },
        text = {
            Column {
                Text(
                    "Select the streaming quality. Higher quality uses more bandwidth.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedQuality = quality }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedQuality == quality,
                            onClick = { selectedQuality = quality },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(quality.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${quality.bitrate} kbps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selectedQuality == quality) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                DesktopPlayer.setAudioQuality(selectedQuality)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
