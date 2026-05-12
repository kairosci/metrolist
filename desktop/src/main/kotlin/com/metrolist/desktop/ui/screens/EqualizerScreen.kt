package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.eq.EqualizerConfig
import com.metrolist.desktop.eq.EqualizerPresets

object EqualizerState {
    var config by mutableStateOf(EqualizerConfig())
}

private val bandLabels = listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

@Composable
fun EqualizerScreen(modifier: Modifier = Modifier) {
    var expandedPreset by remember { mutableStateOf(false) }
    val config = EqualizerState.config

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, null)
                Spacer(Modifier.width(12.dp))
                Text("Equalizer", style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enable Equalizer", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { EqualizerState.config = config.copy(enabled = it) },
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Preset: ${config.selectedPreset}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(12.dp))
                Box {
                    Button(onClick = { expandedPreset = true }) {
                        Icon(Icons.Default.ArrowDropDown, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Change")
                    }
                    DropdownMenu(expanded = expandedPreset, onDismissRequest = { expandedPreset = false }) {
                        EqualizerPresets.presets.keys.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    EqualizerState.config = config.copy(
                                        selectedPreset = name,
                                        bands = EqualizerPresets.presets[name] ?: config.bands,
                                    )
                                    expandedPreset = false
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            if (config.enabled) {
                Text("Frequency Response", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                EqGraph(bands = config.bands, modifier = Modifier.fillMaxWidth().height(200.dp))
                Spacer(Modifier.height(16.dp))
                Text("Band Gain (dB)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }
        }

        if (config.enabled) {
            items(config.bands.indices.toList()) { index ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text(bandLabels.getOrElse(index) { "$index" }, modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = config.bands[index],
                        onValueChange = { newVal ->
                            val newBands = config.bands.toMutableList()
                            newBands[index] = newVal
                            EqualizerState.config = config.copy(
                                bands = newBands,
                                selectedPreset = "Custom",
                            )
                        },
                        valueRange = -12f..12f,
                        modifier = Modifier.weight(1f),
                    )
                    Text("%+.1f".format(config.bands[index]), modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    EqualizerState.config = config.copy(bands = List(10) { 0f }, selectedPreset = "Flat")
                }) {
                    Text("Reset")
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun EqGraph(bands: List<Float>, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (bands.isEmpty()) return@Canvas
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val stepX = w / (bands.size - 1).coerceAtLeast(1)

            val path = Path()
            bands.forEachIndexed { i, gain ->
                val x = i * stepX
                val y = midY - (gain / 12f) * midY
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(path, color = Color(0xFFED5564), style = Stroke(width = 3f))

            bands.forEachIndexed { i, gain ->
                val x = i * stepX
                val y = midY - (gain / 12f) * midY
                drawCircle(Color(0xFFED5564), radius = 6f, center = Offset(x, y))
            }

            drawLine(Color.Gray.copy(alpha = 0.3f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)
        }
    }
}
