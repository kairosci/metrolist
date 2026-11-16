package com.metrolist.shared.viewmodels

import com.metrolist.shared.model.SongItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeClassicQuickPicksTest {
    private fun song(id: String) = SongItem(id, id, null, emptyList(), null)

    @Test
    fun classicQuickPicksUseLocalHistoryAndFallBackToApiWhenEmpty() {
        val local = listOf(song("related"))
        val forgotten = listOf(song("forgotten"))
        val api = listOf(song("api"))

        val classic = selectQuickPicks(true, api, emptyList(), local, forgotten)
        assertEquals(setOf("related", "forgotten"), classic.map(SongItem::id).toSet())
        assertTrue(classic.none { it.id == "api" })
        assertEquals(api, selectQuickPicks(true, api, emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun normalModePrefersApiAndUsesCurrentPicksOnlyAsFallback() {
        val current = listOf(song("current"))
        val api = listOf(song("api"))

        assertEquals(api, selectQuickPicks(false, api, current, emptyList(), emptyList()))
        assertEquals(current, selectQuickPicks(false, emptyList(), current, emptyList(), emptyList()))
    }

    @Test
    fun filteredQuickPicksUseMergedApiResultsEvenWhenClassicModeIsEnabled() {
        val current = listOf(song("current"))
        val mergedApi = listOf(song("current"), song("next"))

        assertEquals(mergedApi, selectQuickPicks(true, mergedApi, current, emptyList(), emptyList(), isFiltered = true))
    }
}
