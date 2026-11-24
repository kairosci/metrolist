package com.metrolist.shared.playback

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

object IosPlaybackSessionOwner {
    private val lock = SynchronizedObject()
    private val windowCounts = mutableMapOf<MusicPlayer, Int>()

    fun acquireWindow(player: MusicPlayer) {
        synchronized(lock) {
            windowCounts[player] = (windowCounts[player] ?: 0) + 1
        }
    }

    fun releaseWindow(player: MusicPlayer) {
        val shouldRelease =
            synchronized(lock) {
                val remaining = (windowCounts[player] ?: 1) - 1
                if (remaining > 0) {
                    windowCounts[player] = remaining
                    false
                } else {
                    windowCounts.remove(player)
                    true
                }
            }
        if (shouldRelease) player.release()
    }
}
