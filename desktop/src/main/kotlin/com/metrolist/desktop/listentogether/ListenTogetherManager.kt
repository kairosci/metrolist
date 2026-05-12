package com.metrolist.desktop.listentogether

import com.metrolist.desktop.core.DesktopPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

object ListenTogetherManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json = Json { encodeDefaults = true }
    private var syncJob: Job? = null

    private var lastSyncPosition: Long = 0L
    private var lastSyncSongId: String? = null

    fun startSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                val state = ListenTogetherClient.state.first()
                if (state.connectionState == ConnectionState.CONNECTED) {
                    val playerState = DesktopPlayer.state.value
                    val currentSong = playerState.currentSong

                    val songChanged = currentSong?.id != lastSyncSongId
                    val positionDrift = kotlin.math.abs(playerState.position - lastSyncPosition)

                    if (songChanged || positionDrift > 2000) {
                        lastSyncSongId = currentSong?.id
                        lastSyncPosition = playerState.position

                        val msg = buildJsonObject {
                            put("type", JsonPrimitive("playback_update"))
                            put("payload", buildJsonObject {
                                put("trackId", JsonPrimitive(currentSong?.id ?: ""))
                                put("title", JsonPrimitive(currentSong?.title ?: ""))
                                put("artist", JsonPrimitive(currentSong?.artists?.joinToString(", ") { it.name } ?: ""))
                                put("isPlaying", JsonPrimitive(playerState.isPlaying))
                                put("position", JsonPrimitive(playerState.position))
                                put("duration", JsonPrimitive(playerState.duration))
                            })
                        }
                        ListenTogetherClient.sendMessage(msg.toString())
                    }
                }
                delay(1000)
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }
}
