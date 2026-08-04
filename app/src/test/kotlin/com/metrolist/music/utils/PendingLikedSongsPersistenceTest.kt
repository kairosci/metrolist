package com.metrolist.music.utils

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.PendingLikedSongIdsKey
import com.metrolist.music.constants.PendingUnlikedSongIdsKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class PendingLikedSongsPersistenceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `pending intents survive a DataStore restart`() = runBlocking {
        val file = temporaryFolder.newFile("settings.preferences_pb")

        val firstScope = CoroutineScope(Dispatchers.IO)
        val first = PreferenceDataStoreFactory.create(scope = firstScope) { file }
        first.edit { prefs ->
            prefs[PendingLikedSongIdsKey] = setOf("a", "b")
            prefs[PendingUnlikedSongIdsKey] = setOf("c")
        }

        // Cancelling the scope releases the file so a fresh instance (a restart)
        // can reopen it and read the persisted intents.
        firstScope.cancel()

        val restartedScope = CoroutineScope(Dispatchers.IO)
        val restarted = PreferenceDataStoreFactory.create(scope = restartedScope) { file }
        val pendingLiked = restarted.data
            .map { it[PendingLikedSongIdsKey] ?: emptySet<String>() }
            .first()
        val pendingUnliked = restarted.data
            .map { it[PendingUnlikedSongIdsKey] ?: emptySet<String>() }
            .first()
        restartedScope.cancel()

        assertEquals(setOf("a", "b"), pendingLiked)
        assertEquals(setOf("c"), pendingUnliked)
    }

    @Test
    fun `restarted process reconciles persisted intents against the remote`() = runBlocking {
        val file = temporaryFolder.newFile("settings.preferences_pb")

        val firstScope = CoroutineScope(Dispatchers.IO)
        val first = PreferenceDataStoreFactory.create(scope = firstScope) { file }
        first.edit { prefs ->
            prefs[PendingLikedSongIdsKey] = setOf("a", "b")
        }
        firstScope.cancel()

        val restartedScope = CoroutineScope(Dispatchers.IO)
        val restarted = PreferenceDataStoreFactory.create(scope = restartedScope) { file }
        val pendingLiked = restarted.data
            .map { it[PendingLikedSongIdsKey] ?: emptySet<String>() }
            .first()
        restartedScope.cancel()
        val plan = reconcilePendingLikedSongs(
            pendingLikedIds = pendingLiked,
            pendingUnlikedIds = emptySet(),
            localLikedSongs = mapOf("a" to false, "b" to false),
            remoteIds = setOf("b"),
        )

        assertEquals(setOf("a"), plan.likeIds.toSet())
        assertEquals(setOf("b"), plan.clearPendingLikes.toSet())

        val nextLikes = pendingLiked - (plan.clearPendingLikes + plan.likeIds).toSet()
        assertTrue(nextLikes.isEmpty())
    }
}
