package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.DesktopPlayer

@Composable
fun ContentSettingsDialog(onDismiss: () -> Unit) {
    var hideExplicit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Language, null) },
        title = { Text("Content") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { hideExplicit = !hideExplicit }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = hideExplicit, onCheckedChange = { hideExplicit = it })
                    Spacer(Modifier.width(12.dp))
                    Text("Hide explicit content", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Language and region settings are managed by your YouTube Music account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
fun PrivacySettingsDialog(onDismiss: () -> Unit) {
    var saveHistory by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PrivacyTip, null) },
        title = { Text("Privacy") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { saveHistory = !saveHistory }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = saveHistory, onCheckedChange = { saveHistory = it })
                    Spacer(Modifier.width(12.dp))
                    Text("Save listening history", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "History can be cleared from YouTube Music settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
fun StorageSettingsDialog(onDismiss: () -> Unit) {
    var cacheSize by remember { mutableStateOf("calculating...") }

    LaunchedEffect(Unit) {
        cacheSize = try {
            val dir = java.io.File(System.getProperty("java.io.tmpdir"))
            val bytes = dir.walkTopDown().filter { it.name.startsWith("metrolist") || it.name.startsWith("ktor") }.fold(0L) { acc, f -> acc + f.length() }
            if (bytes > 1_000_000) "${bytes / 1_000_000} MB" else "${bytes / 1_000} KB"
        } catch (_: Exception) { "0 KB" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Storage, null) },
        title = { Text("Storage") },
        text = {
            Column {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cache Size", style = MaterialTheme.typography.labelLarge)
                        Text(cacheSize, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        cacheSize = "0 KB"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Clear Cache")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.NewReleases, null) },
        title = { Text("What's New") },
        text = {
            Column(Modifier.heightIn(max = 400.dp)) {
                Text("Version 13.4.2", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    """
                    • Desktop multi-platform support
                    • Listen Together with LAN discovery
                    • Equalizer with 10-band EQ + 12 presets
                    • Podcast playback support
                    • Lyrics system with 4 providers
                    • Audio quality selection
                    • Theme customization (dark/light/system)
                    • History screen
                    • Browse screens (Explore, Charts, New Releases)
                    • Mood & Genres browser
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
