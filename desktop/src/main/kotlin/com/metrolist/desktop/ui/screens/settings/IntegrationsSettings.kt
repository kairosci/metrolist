package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.prefs.Preferences

private val prefs = Preferences.userNodeForPackage(IntegrationsSettings::class.java)

object IntegrationsSettings {
    var discordEnabled: Boolean
        get() = prefs.getBoolean("discordEnabled", false)
        set(value) { prefs.putBoolean("discordEnabled", value) }

    var lastfmEnabled: Boolean
        get() = prefs.getBoolean("lastfmEnabled", false)
        set(value) { prefs.putBoolean("lastfmEnabled", value) }

    var lastfmUsername: String
        get() = prefs.get("lastfmUsername", "")
        set(value) { prefs.put("lastfmUsername", value) }

    var lastfmPassword: String
        get() = prefs.get("lastfmPassword", "")
        set(value) { prefs.put("lastfmPassword", value) }
}

@Composable
fun IntegrationsSettingsDialog(onDismiss: () -> Unit) {
    var discordEnabled by remember { mutableStateOf(IntegrationsSettings.discordEnabled) }
    var lastfmEnabled by remember { mutableStateOf(IntegrationsSettings.lastfmEnabled) }
    var lastfmUsername by remember { mutableStateOf(IntegrationsSettings.lastfmUsername) }
    var lastfmPassword by remember { mutableStateOf(IntegrationsSettings.lastfmPassword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Extension, null) },
        title = { Text("Integrations") },
        text = {
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Gamepad, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Discord Rich Presence", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Show what you're listening to on Discord",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = discordEnabled,
                                onCheckedChange = { discordEnabled = it },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Last.fm Scrobbling", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = lastfmEnabled,
                                onCheckedChange = { lastfmEnabled = it },
                            )
                        }

                        if (lastfmEnabled) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = lastfmUsername,
                                onValueChange = { lastfmUsername = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = lastfmPassword,
                                onValueChange = { lastfmPassword = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Note: Integrations require active internet connection. Discord Rich Presence uses the Discord IPC protocol. Last.fm scrobbles tracks to your profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                IntegrationsSettings.discordEnabled = discordEnabled
                IntegrationsSettings.lastfmEnabled = lastfmEnabled
                IntegrationsSettings.lastfmUsername = lastfmUsername
                IntegrationsSettings.lastfmPassword = lastfmPassword
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
