package com.metrolist.desktop.listentogether

import com.metrolist.innertube.InternalLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.*

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val deviceType: String,
    val lastSeen: Long,
)

object LanDiscovery {
    private const val MULTICAST_GROUP = "239.255.27.27"
    private const val MULTICAST_PORT = 27270
    private const val HEARTBEAT_INTERVAL_MS = 3000L
    private const val DEVICE_TIMEOUT_MS = 10000L

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var listenerJob: Job? = null
    private var cleanupJob: Job? = null
    private var running = false

    private var deviceName: String = "Metrolist Desktop"
    private var deviceId: String = ""

    fun start(name: String) {
        if (running) return
        running = true
        deviceName = name
        deviceId = "${InetAddress.getLocalHost().hostAddress}-${MULTICAST_PORT}"

        val group = InetAddress.getByName(MULTICAST_GROUP)
        val socket = MulticastSocket(MULTICAST_PORT)
        socket.joinGroup(group)
        socket.soTimeout = 1000

        listenerJob = scope.launch {
            val buffer = ByteArray(1024)
            while (isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val parts = msg.split("|")
                    if (parts.size >= 4 && parts[0] != deviceId) {
                        val device = DiscoveredDevice(
                            id = parts[0],
                            name = parts[1],
                            host = packet.address.hostAddress ?: parts[2],
                            port = parts[3].toIntOrNull() ?: MULTICAST_PORT,
                            deviceType = parts.getOrElse(4) { "desktop" },
                            lastSeen = System.currentTimeMillis(),
                        )
                        val current = _devices.value.toMutableList()
                        current.removeAll { it.id == device.id }
                        current.add(device)
                        _devices.value = current.sortedByDescending { it.lastSeen }
                    }
                } catch (_: SocketTimeoutException) { }
            }
        }

        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    val msg = "$deviceId|$deviceName|${InetAddress.getLocalHost().hostAddress}|$MULTICAST_PORT|desktop"
                    val data = msg.toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(data, data.size, group, MULTICAST_PORT)
                    socket.send(packet)
                } catch (_: Exception) { }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }

        cleanupJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val current = _devices.value.toMutableList()
                current.removeAll { now - it.lastSeen > DEVICE_TIMEOUT_MS }
                _devices.value = current
                delay(5000)
            }
        }
    }

    fun stop() {
        running = false
        heartbeatJob?.cancel()
        listenerJob?.cancel()
        cleanupJob?.cancel()
        _devices.value = emptyList()
    }
}
