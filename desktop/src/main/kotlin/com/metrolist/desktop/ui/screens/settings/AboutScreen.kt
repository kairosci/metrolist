package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("About", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text("Metrolist Desktop", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text("Version 13.4.2", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A 3rd party YouTube Music client",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Links", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        listOf(
            "GitHub Repository" to "https://github.com/metrolist/metrolist",
            "Report an Issue" to "https://github.com/metrolist/metrolist/issues",
            "License" to "https://github.com/metrolist/metrolist/blob/main/LICENSE",
        ).forEach { (label, url) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.OpenInNew, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = {
                    try {
                        Desktop.getDesktop().browse(URI(url))
                    } catch (_: Exception) { }
                }) {
                    Text(label)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Libraries", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf(
                    "Compose Multiplatform",
                    "Kotlin Coroutines",
                    "Ktor Client",
                    "kotlinx.serialization",
                    "Java Sound API",
                    "Innertube",
                ).forEach { library ->
                    Text("• $library", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Metrolist is not affiliated with Google LLC or YouTube Music.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
