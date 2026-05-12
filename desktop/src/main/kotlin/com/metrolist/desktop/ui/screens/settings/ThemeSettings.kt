@file:OptIn(ExperimentalMaterial3Api::class)

package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.DefaultAccentColor
import com.metrolist.desktop.core.ThemeManager
import com.metrolist.desktop.core.ThemeOption

private val accentColors = listOf(
    Color(0xFFED5564), Color(0xFFFF6B6B), Color(0xFFE74C3C), Color(0xFFE67E22),
    Color(0xFFF39C12), Color(0xFFFFD93D), Color(0xFF6BCB77), Color(0xFF2ECC71),
    Color(0xFF1ABC9C), Color(0xFF00B894), Color(0xFF00CEC9), Color(0xFF0984E3),
    Color(0xFF6C5CE7), Color(0xFF8E44AD), Color(0xFFE056A0), Color(0xFFFF6B81),
)

private data class ThemeOptionUi(
    val option: ThemeOption,
    val label: String,
    val icon: @Composable () -> Unit,
)

private val themeOptions = listOf(
    ThemeOptionUi(ThemeOption.SYSTEM, "System", { Icon(Icons.Default.SettingsBrightness, null) }),
    ThemeOptionUi(ThemeOption.LIGHT, "Light", { Icon(Icons.Default.LightMode, null) }),
    ThemeOptionUi(ThemeOption.DARK, "Dark", { Icon(Icons.Default.DarkMode, null) }),
)

@Composable
fun ThemeSettingsDialog(onDismiss: () -> Unit) {
    var selectedTheme by remember { mutableStateOf(ThemeManager.currentTheme) }
    var selectedAccent by remember { mutableStateOf(ThemeManager.config.accentColor) }
    var pureBlack by remember { mutableStateOf(ThemeManager.config.pureBlack) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Palette, null) },
        title = { Text("Theme") },
        text = {
            Column(Modifier.heightIn(max = 500.dp)) {
                Text(
                    "Choose your preferred appearance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                themeOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTheme = option.option }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        option.icon()
                        Spacer(Modifier.width(16.dp))
                        Text(option.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (selectedTheme == option.option) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Accent Color", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Column {
                    accentColors.chunked(8).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { color ->
                                val isSelected = selectedAccent == color
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier.border(1.dp, Color.Transparent, CircleShape)
                                        )
                                        .clickable { selectedAccent = color },
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { pureBlack = !pureBlack }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = pureBlack, onCheckedChange = { pureBlack = it })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Pure black background (dark mode)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Uses #000000 background in dark mode for AMOLED screens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                ThemeManager.setTheme(selectedTheme)
                ThemeManager.setAccentColor(selectedAccent)
                ThemeManager.setPureBlack(pureBlack)
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
