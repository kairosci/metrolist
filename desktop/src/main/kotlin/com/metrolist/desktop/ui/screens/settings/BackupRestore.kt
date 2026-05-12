package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.BackupManager
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun BackupRestoreDialog(onDismiss: () -> Unit) {
    var exportMessage by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Restore, null) },
        title = { Text("Backup & Restore") },
        text = {
            Column {
                Text(
                    "Export your settings to a backup file or restore from a previous backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Export Settings", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Save your account, theme, and integration settings to a JSON file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    fileFilter = FileNameExtensionFilter("Backup files (*.json)", "json")
                                    selectedFile = File("metrolist-backup.json")
                                }
                                if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val file = fileChooser.selectedFile
                                    if (BackupManager.exportToFile(file)) {
                                        exportMessage = "Backup saved to ${file.name}"
                                    } else {
                                        exportMessage = "Failed to save backup"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.FileDownload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export")
                        }
                        if (exportMessage.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                exportMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (exportMessage.contains("Failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
                        Text("Restore Settings", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Load settings from a previously saved backup file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    fileFilter = FileNameExtensionFilter("Backup files (*.json)", "json")
                                }
                                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val file = fileChooser.selectedFile
                                    if (BackupManager.import(file)) {
                                        importMessage = "Settings restored from ${file.name}"
                                    } else {
                                        importMessage = "Failed to restore backup"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.FileUpload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Restore")
                        }
                        if (importMessage.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                importMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (importMessage.contains("Failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
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
