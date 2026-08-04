package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LikedSongReconciliationTest {

    /**
     * Runs one reconcile round plus the push/clear bookkeeping done by
     * `executeSyncLikedSongs`: ids whose push succeeds (or that the plan marks
     * as confirmed) leave the pending sets, while failed pushes stay pending.
     */
    private fun round(
        pendingLiked: Set<String>,
        pendingUnliked: Set<String>,
        localLikedSongs: Map<String, Boolean>,
        remoteIds: Set<String>,
        pushSucceeds: (String) -> Boolean = { true },
    ): Triple<LikedSongReconciliationPlan, Set<String>, Set<String>> {
        val plan = reconcilePendingLikedSongs(
            pendingLikedIds = pendingLiked,
            pendingUnlikedIds = pendingUnliked,
            localLikedSongs = localLikedSongs,
            remoteIds = remoteIds,
        )
        val pushedLikes = plan.likeIds.filter { pushSucceeds(it) }
        val pushedUnlikes = plan.unlikeIds.filter { pushSucceeds(it) }
        val nextLikes = pendingLiked - (plan.clearPendingLikes + pushedLikes).toSet()
        val nextUnlikes = pendingUnliked - (plan.clearPendingUnlikes + pushedUnlikes).toSet()
        return Triple(plan, nextLikes, nextUnlikes)
    }

    @Test
    fun `pending offline local like is pushed when missing from remote`() {
        val (plan, nextLikes, nextUnlikes) = round(
            pendingLiked = setOf("a"),
            pendingUnliked = emptySet(),
            localLikedSongs = mapOf("a" to false),
            remoteIds = emptySet(),
        )

        assertEquals(setOf("a"), plan.likeIds.toSet())
        assertTrue(plan.unlikeIds.isEmpty())
        assertTrue(plan.clearPendingLikes.isEmpty())
        assertTrue("pushed like must be cleared from pending", nextLikes.isEmpty())
        assertTrue(nextUnlikes.isEmpty())
    }

    @Test
    fun `remote removal is never reversed for a non-pending local like`() {
        val (plan, _, _) = round(
            pendingLiked = emptySet(),
            pendingUnliked = emptySet(),
            localLikedSongs = mapOf("a" to false),
            remoteIds = emptySet(),
        )

        assertTrue("remote removal must not be re-pushed", plan.likeIds.isEmpty())
        assertTrue(plan.unlikeIds.isEmpty())
    }

    @Test
    fun `repeated sync after success does not re-push`() {
        var pendingLiked = setOf("a")
        for (iteration in 1..3) {
            val (plan, next, _) = round(
                pendingLiked = pendingLiked,
                pendingUnliked = emptySet(),
                localLikedSongs = mapOf("a" to false),
                remoteIds = emptySet(),
            )
            if (iteration == 1) {
                assertEquals(setOf("a"), plan.likeIds.toSet())
            } else {
                assertTrue("already-confirmed song must not be re-pushed", plan.likeIds.isEmpty())
            }
            pendingLiked = next
        }
    }

    @Test
    fun `failed push stays pending and is retried on the next sync`() {
        var pendingLiked = setOf("a")
        val (plan, next, _) = round(
            pendingLiked = pendingLiked,
            pendingUnliked = emptySet(),
            localLikedSongs = mapOf("a" to false),
            remoteIds = emptySet(),
            pushSucceeds = { false },
        )

        assertEquals(setOf("a"), plan.likeIds.toSet())
        assertEquals("failed push must remain pending", setOf("a"), next)
        pendingLiked = next

        val (retryPlan, retryNext, _) = round(
            pendingLiked = pendingLiked,
            pendingUnliked = emptySet(),
            localLikedSongs = mapOf("a" to false),
            remoteIds = emptySet(),
        )
        assertEquals(setOf("a"), retryPlan.likeIds.toSet())
        assertTrue(retryNext.isEmpty())
    }

    @Test
    fun `like already present on remote is confirmed without a push`() {
        val (plan, nextLikes, _) = round(
            pendingLiked = setOf("a"),
            pendingUnliked = emptySet(),
            localLikedSongs = mapOf("a" to false),
            remoteIds = setOf("a"),
        )

        assertTrue(plan.likeIds.isEmpty())
        assertEquals(setOf("a"), plan.clearPendingLikes.toSet())
        assertTrue("confirmed like must be cleared", nextLikes.isEmpty())
    }

    @Test
    fun `stale pending like with no local backing is cleared`() {
        val (plan, nextLikes, _) = round(
            pendingLiked = setOf("a"),
            pendingUnliked = emptySet(),
            localLikedSongs = emptyMap(),
            remoteIds = emptySet(),
        )

        assertTrue(plan.likeIds.isEmpty())
        assertEquals(setOf("a"), plan.clearPendingLikes.toSet())
        assertTrue(nextLikes.isEmpty())
    }

    @Test
    fun `local file likes are kept local and never pushed`() {
        val (plan, nextLikes, _) = round(
            pendingLiked = setOf("a"),
            pendingUnliked = emptySet(),
            localLikedSongs = mapOf("a" to true),
            remoteIds = emptySet(),
        )

        assertTrue("local files cannot be mirrored", plan.likeIds.isEmpty())
        assertEquals(setOf("a"), plan.clearPendingLikes.toSet())
        assertTrue(nextLikes.isEmpty())
    }

    @Test
    fun `pending unlike is pushed while the song is still on the remote`() {
        val (plan, _, nextUnlikes) = round(
            pendingLiked = emptySet(),
            pendingUnliked = setOf("a"),
            localLikedSongs = emptyMap(),
            remoteIds = setOf("a"),
        )

        assertTrue(plan.likeIds.isEmpty())
        assertEquals(setOf("a"), plan.unlikeIds.toSet())
        assertTrue("pushed unlike must be cleared", nextUnlikes.isEmpty())
    }

    @Test
    fun `pending unlike already gone from remote is confirmed without a push`() {
        val (plan, _, nextUnlikes) = round(
            pendingLiked = emptySet(),
            pendingUnliked = setOf("a"),
            localLikedSongs = emptyMap(),
            remoteIds = emptySet(),
        )

        assertTrue(plan.unlikeIds.isEmpty())
        assertEquals(setOf("a"), plan.clearPendingUnlikes.toSet())
        assertTrue(nextUnlikes.isEmpty())
    }

    @Test
    fun `pending unlike is cancelled when the song is liked locally again`() {
        val (plan, _, nextUnlikes) = round(
            pendingLiked = setOf("a"),
            pendingUnliked = setOf("a"),
            localLikedSongs = mapOf("a" to false),
            remoteIds = emptySet(),
        )

        assertTrue("re-liked song must not be unliked", plan.unlikeIds.isEmpty())
        assertEquals(setOf("a"), plan.clearPendingUnlikes.toSet())
        assertEquals("like intent still pending", setOf("a"), plan.likeIds.toSet())
        assertTrue(nextUnlikes.isEmpty())
    }

    @Test
    fun `mixed pending intents are reconciled independently`() {
        val (plan, nextLikes, nextUnlikes) = round(
            pendingLiked = setOf("a", "b", "c"),
            pendingUnliked = setOf("x", "y", "z"),
            localLikedSongs = mapOf(
                "a" to false,
                "b" to true,
                "c" to false,
            ),
            remoteIds = setOf("b", "x", "z"),
        )

        assertEquals(setOf("a", "c"), plan.likeIds.toSet())
        assertEquals(setOf("b"), plan.clearPendingLikes.toSet())
        assertEquals(setOf("x", "z"), plan.unlikeIds.toSet())
        assertEquals(setOf("y"), plan.clearPendingUnlikes.toSet())
        assertTrue(nextLikes.isEmpty())
        assertTrue(nextUnlikes.isEmpty())
    }
}
