package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import java.awt.Desktop
import java.net.URI
import java.util.prefs.Preferences

private val prefs = Preferences.userNodeForPackage(AccountSettings::class.java)
private const val COOKIE_KEY = "innerTubeCookie"

object AccountSettings {
    var cookie: String
        get() = prefs.get(COOKIE_KEY, "")
        set(value) {
            prefs.put(COOKIE_KEY, value)
            YouTube.cookie = value
        }

    fun isLoggedIn(): Boolean = "SAPISID" in parseCookieString(cookie)

    fun logout() {
        prefs.remove(COOKIE_KEY)
        YouTube.cookie = ""
    }
}

@Composable
fun AccountSettingsDialog(onDismiss: () -> Unit) {
    val isLoggedIn = remember { AccountSettings.isLoggedIn() }
    var showCookieInput by remember { mutableStateOf(false) }
    var cookieInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountCircle, null) },
        title = { Text("Account") },
        text = {
            Column {
                if (isLoggedIn) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("Logged in", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Your YouTube Music account is connected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            AccountSettings.logout()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                } else if (showCookieInput) {
                    Text("Paste your YouTube Music cookie below:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("Paste cookie here...") },
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (cookieInput.isNotBlank() && "SAPISID" in parseCookieString(cookieInput)) {
                                AccountSettings.cookie = cookieInput
                                onDismiss()
                            }
                        },
                        enabled = cookieInput.isNotBlank(),
                    ) {
                        Text("Save Cookie")
                    }
                } else {
                    Text("Sign in to access your library, history, and playlists.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        try {
                            Desktop.getDesktop().browse(URI("https://music.youtube.com"))
                        } catch (_: Exception) { }
                    }) {
                        Icon(Icons.Default.OpenInBrowser, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open YouTube Music")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { showCookieInput = true }) {
                        Icon(Icons.Default.ContentPaste, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Paste Cookie")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Open YouTube Music in your browser\n2. Log in\n3. Copy your cookies (SAPISID=...)\n4. Paste them here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
