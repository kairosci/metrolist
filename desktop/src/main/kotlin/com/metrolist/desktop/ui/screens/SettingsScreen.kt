package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.lyrics.DesktopLyricsHelper
import com.metrolist.desktop.lyrics.LyricsProviderRegistry
import com.metrolist.desktop.ui.screens.settings.AboutScreen
import com.metrolist.desktop.ui.screens.settings.AccountSettingsDialog
import com.metrolist.desktop.ui.screens.settings.AudioQualitySettingsDialog
import com.metrolist.desktop.ui.screens.settings.BackupRestoreDialog
import com.metrolist.desktop.ui.screens.settings.ChangelogDialog
import com.metrolist.desktop.ui.screens.settings.ContentSettingsDialog
import com.metrolist.desktop.ui.screens.settings.IntegrationsSettingsDialog
import com.metrolist.desktop.ui.screens.settings.PrivacySettingsDialog
import com.metrolist.desktop.ui.screens.settings.SleepTimerDialog
import com.metrolist.desktop.ui.screens.settings.StorageSettingsDialog
import com.metrolist.desktop.ui.screens.settings.ThemeSettingsDialog

private data class SettingsGroup(
    val title: String,
    val items: List<SettingsItem>,
)

private data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val onClick: String,
)

private val settingsGroups = listOf(
    SettingsGroup("Account", listOf(
        SettingsItem("Account & Login", Icons.Filled.AccountCircle, "Sign in, manage account", "account"),
        SettingsItem("Sleep Timer", Icons.Default.Timer, "Auto-stop playback", "sleep_timer"),
    )),
    SettingsGroup("Appearance", listOf(
        SettingsItem("Theme", Icons.Filled.Palette, "Dark mode, colors", "theme"),
    )),
    SettingsGroup("Playback", listOf(
        SettingsItem("Audio Quality", Icons.Filled.VolumeUp, "Streaming quality", "audio_quality"),
        SettingsItem("Lyrics", Icons.Outlined.Description, "Providers, animation", "lyrics"),
    )),
    SettingsGroup("Content & Privacy", listOf(
        SettingsItem("Content", Icons.Outlined.Language, "Language, region, explicit filter", "content"),
        SettingsItem("Privacy", Icons.Outlined.PrivacyTip, "History, screenshots", "privacy"),
        SettingsItem("Storage", Icons.Filled.Storage, "Cache management", "storage"),
        SettingsItem("Changelog", Icons.Outlined.NewReleases, "What's new", "changelog"),
    )),
    SettingsGroup("Integrations", listOf(
        SettingsItem("Integrations", Icons.Outlined.Extension, "Discord, Last.fm", "integrations"),
    )),
    SettingsGroup("Data", listOf(
        SettingsItem("Backup & Restore", Icons.Outlined.Restore, "Export/import settings", "backup"),
    )),
    SettingsGroup("About", listOf(
        SettingsItem("About", Icons.Filled.Info, "Version, licenses", "about"),
    )),
)

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var showAccountSettings by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showThemeSettings by remember { mutableStateOf(false) }
    var showAudioQualitySettings by remember { mutableStateOf(false) }
    var showLyricsSettings by remember { mutableStateOf(false) }
    var showContentSettings by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showStorageSettings by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showIntegrations by remember { mutableStateOf(false) }
    var showBackupRestore by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    if (showAccountSettings) {
        AccountSettingsDialog(onDismiss = { showAccountSettings = false })
    }
    if (showSleepTimer) {
        SleepTimerDialog(onDismiss = { showSleepTimer = false })
    }
    if (showThemeSettings) {
        ThemeSettingsDialog(onDismiss = { showThemeSettings = false })
    }
    if (showAudioQualitySettings) {
        AudioQualitySettingsDialog(onDismiss = { showAudioQualitySettings = false })
    }
    if (showLyricsSettings) {
        LyricsSettingsDialog(onDismiss = { showLyricsSettings = false })
    }
    if (showContentSettings) {
        ContentSettingsDialog(onDismiss = { showContentSettings = false })
    }
    if (showPrivacySettings) {
        PrivacySettingsDialog(onDismiss = { showPrivacySettings = false })
    }
    if (showStorageSettings) {
        StorageSettingsDialog(onDismiss = { showStorageSettings = false })
    }
    if (showChangelog) {
        ChangelogDialog(onDismiss = { showChangelog = false })
    }
    if (showIntegrations) {
        IntegrationsSettingsDialog(onDismiss = { showIntegrations = false })
    }
    if (showBackupRestore) {
        BackupRestoreDialog(onDismiss = { showBackupRestore = false })
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } },
            text = { AboutScreen() },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
        }

        items(settingsGroups) { group ->
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))

            group.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        when (item.onClick) {
                            "account" -> showAccountSettings = true
                            "sleep_timer" -> showSleepTimer = true
                            "theme" -> showThemeSettings = true
                            "audio_quality" -> showAudioQualitySettings = true
                            "lyrics" -> showLyricsSettings = true
                            "content" -> showContentSettings = true
                            "privacy" -> showPrivacySettings = true
                            "storage" -> showStorageSettings = true
                            "changelog" -> showChangelog = true
                            "integrations" -> showIntegrations = true
                            "backup" -> showBackupRestore = true
                            "about" -> showAbout = true
                        }
                    }.padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge)
                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LyricsSettingsDialog(onDismiss: () -> Unit) {
    var providerOrder by remember {
        mutableStateOf(DesktopLyricsHelper.getProviderOrder().ifEmpty {
            LyricsProviderRegistry.getDefaultProviderOrder()
        })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics Providers") },
        text = {
            Column {
                Text(
                    text = "Drag to reorder providers. The first enabled provider that returns lyrics will be used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                providerOrder.forEachIndexed { index, providerName ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(providerName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (index < providerOrder.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                DesktopLyricsHelper.setProviderOrder(providerOrder)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
