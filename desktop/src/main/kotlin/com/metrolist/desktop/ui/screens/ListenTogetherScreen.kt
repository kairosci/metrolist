package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.core.DesktopPlayer
import com.metrolist.desktop.listentogether.*

@Composable
fun ListenTogetherScreen(modifier: Modifier = Modifier) {
    val ltState by ListenTogetherClient.state.collectAsState()
    val devices by LanDiscovery.devices.collectAsState()
    var serverUrl by remember { mutableStateOf("wss://metroserverx.meowery.eu/ws") }
    var username by remember { mutableStateOf("DesktopUser") }
    var showCreateRoom by remember { mutableStateOf(false) }
    var showJoinRoom by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        LanDiscovery.start(username)
    }

    DisposableEffect(Unit) {
        onDispose { LanDiscovery.stop() }
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text("Listen Together", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        if (ltState.connectionState == ConnectionState.CONNECTED) {
            ConnectedSession(ltState, onDisconnect = {
                ListenTogetherManager.stopSync()
                ListenTogetherClient.disconnect()
            })
        } else {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; ListenTogetherClient.setUserInfo(it) },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    ListenTogetherClient.connect(serverUrl)
                    ListenTogetherManager.startSync()
                    showCreateRoom = true
                }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Room")
                }
                OutlinedButton(onClick = { showJoinRoom = true }) {
                    Icon(Icons.Default.GroupAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Join Room")
                }
            }

            Spacer(Modifier.height(24.dp))

            if (devices.isNotEmpty()) {
                Text("Devices on Network", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(devices) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = {
                                ListenTogetherClient.connect("ws://${device.host}:${device.port}")
                                ListenTogetherManager.startSync()
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Devices, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(device.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("${device.host}:${device.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.CastConnected, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            if (showJoinRoom) {
                JoinRoomDialog(
                    onDismiss = { showJoinRoom = false },
                    onJoin = { code ->
                        ListenTogetherClient.connect(serverUrl, code)
                        ListenTogetherManager.startSync()
                        showJoinRoom = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ConnectedSession(state: ListenTogetherState, onDisconnect: () -> Unit) {
    val playerState by DesktopPlayer.state.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CastConnected, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.isHost) "Hosting Room: ${state.roomCode}" else "Joined Room: ${state.roomCode}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(12.dp))

            if (state.users.isNotEmpty()) {
                Text("Users (${state.users.size})", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                state.users.forEach { user ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Default.Person, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("${user.username}${if (user.isHost) " (Host)" else ""}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (playerState.currentSong != null) {
                Text("Now Playing: ${playerState.currentSong?.title}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onDisconnect) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
private fun JoinRoomDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var roomCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Room") },
        text = {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it },
                label = { Text("Room Code") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onJoin(roomCode) }, enabled = roomCode.isNotBlank()) {
                Text("Join")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
