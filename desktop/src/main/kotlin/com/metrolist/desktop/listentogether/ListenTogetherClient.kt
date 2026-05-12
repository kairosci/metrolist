package com.metrolist.desktop.listentogether

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR,
}

enum class RoomRole {
    HOST,
    GUEST,
}

data class UserInfo(
    val userId: String,
    val username: String,
    val isHost: Boolean = false,
)

data class ListenTogetherState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val role: RoomRole? = null,
    val roomCode: String? = null,
    val users: List<UserInfo> = emptyList(),
    val isHost: Boolean = false,
)

data class TrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0L,
    val thumbnail: String = "",
)

object ListenTogetherClient {
    private val _state = MutableStateFlow(ListenTogetherState())
    val state: StateFlow<ListenTogetherState> = _state.asStateFlow()

    private var session: WebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private var userId: String = UUID.randomUUID().toString().take(8)
    private var username: String = "DesktopUser"
    private var reconnecting = false

    fun setUserInfo(name: String) {
        username = name
    }

    fun connect(serverUrl: String, roomCode: String? = null) {
        scope.launch {
            _state.value = _state.value.copy(connectionState = ConnectionState.CONNECTING)
            try {
                val wsUrl = if (roomCode != null) "$serverUrl?room=$roomCode&userId=$userId&username=$username"
                else "$serverUrl?userId=$userId&username=$username"

                val client = HttpClient {
                    install(WebSockets)
                }

                client.webSocket(wsUrl) {
                    session = this
                    _state.value = _state.value.copy(
                        connectionState = ConnectionState.CONNECTED,
                        roomCode = roomCode,
                    )

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                handleMessage(text)
                            }
                            else -> {}
                        }
                    }
                }

                _state.value = _state.value.copy(
                    connectionState = ConnectionState.DISCONNECTED,
                    role = null,
                    users = emptyList(),
                )
                session = null
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    connectionState = ConnectionState.ERROR,
                )
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            @Suppress("UNUSED")
            val msg = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(text)
            val type = msg["type"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            val payload = msg["payload"]?.toString() ?: "{}"

            when (type) {
                "room_created", "join_approved" -> {
                    val data = json.decodeFromString<Map<String, String>>(payload)
                    val code = data["roomCode"] ?: data["room_code"] ?: return
                    val role = if (data["isHost"]?.toBoolean() == true) RoomRole.HOST else RoomRole.GUEST
                    _state.value = _state.value.copy(
                        roomCode = code,
                        role = role,
                        isHost = role == RoomRole.HOST,
                    )
                }
                "user_joined" -> {
                    val data = json.decodeFromString<Map<String, String>>(payload)
                    val user = UserInfo(
                        userId = data["userId"] ?: "",
                        username = data["username"] ?: "",
                    )
                    val users = _state.value.users + user
                    _state.value = _state.value.copy(users = users)
                }
                "user_left" -> {
                    val data = json.decodeFromString<Map<String, String>>(payload)
                    val users = _state.value.users.filter { it.userId != data["userId"] }
                    _state.value = _state.value.copy(users = users)
                }
                "error" -> {
                    _state.value = _state.value.copy(connectionState = ConnectionState.ERROR)
                }
            }
        } catch (_: Exception) { }
    }

    suspend fun sendMessage(message: String) {
        try {
            session?.send(Frame.Text(message))
        } catch (_: Exception) { }
    }

    fun disconnect() {
        scope.launch {
            try {
                session?.close()
            } catch (_: Exception) { }
            session = null
            _state.value = ListenTogetherState()
        }
    }
}
