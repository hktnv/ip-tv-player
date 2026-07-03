package com.hktnv.iptvbox

import com.hktnv.iptvbox.state.planCatalogSnapshotLoad
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.PlaylistStats
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.state.isCatalogSnapshotReadyForView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSnapshotLoadPolicyTest {
    @Test
    fun keepsCurrentSnapshotDuringSamePlaylistRefresh() {
        val plan = planCatalogSnapshotLoad(
            currentSnapshotPlaylistId = "playlist-1",
            targetPlaylistId = "playlist-1",
            showPlaylistEntry = false,
        )

        assertFalse(plan.clearSnapshot)
        assertFalse(plan.showBlockingLoading)
    }

    @Test
    fun blocksEmptyStateWhileSelectedCategoryItemsAreStillLoading() {
        val snapshot = catalogSnapshot(
            categoryCounts = mapOf("Spor" to 12),
            categoryItemsLoaded = false,
        )

        assertFalse(
            isCatalogSnapshotReadyForView(
                snapshot = snapshot,
                playlistId = "playlist-1",
                showCategoryLanding = false,
                selectedTab = CatalogTab.LIVE,
                selectedCategory = "Spor",
                selectedSeriesTitle = null,
                selectedSeasonNumber = null,
            ),
        )
    }

    @Test
    fun allowsEmptyStateWhenSelectedCategoryIsActuallyEmpty() {
        val snapshot = catalogSnapshot(
            categoryCounts = mapOf("Spor" to 0),
            categoryItemsLoaded = false,
        )

        assertTrue(
            isCatalogSnapshotReadyForView(
                snapshot = snapshot,
                playlistId = "playlist-1",
                showCategoryLanding = false,
                selectedTab = CatalogTab.LIVE,
                selectedCategory = "Spor",
                selectedSeriesTitle = null,
                selectedSeasonNumber = null,
            ),
        )
    }

    @Test
    fun samePlaylistShowsBlockingLoadingWhenCurrentViewIsNotReady() {
        val plan = planCatalogSnapshotLoad(
            currentSnapshotPlaylistId = "playlist-1",
            targetPlaylistId = "playlist-1",
            showPlaylistEntry = false,
            currentSnapshotViewReady = false,
        )

        assertFalse(plan.clearSnapshot)
        assertTrue(plan.showBlockingLoading)
    }

    @Test
    fun clearsSnapshotAndShowsBlockingLoadingForDifferentPlaylist() {
        val plan = planCatalogSnapshotLoad(
            currentSnapshotPlaylistId = "playlist-1",
            targetPlaylistId = "playlist-2",
            showPlaylistEntry = false,
        )

        assertTrue(plan.clearSnapshot)
        assertTrue(plan.showBlockingLoading)
    }

    @Test
    fun clearsSnapshotWithoutLoadingOnPlaylistEntry() {
        val plan = planCatalogSnapshotLoad(
            currentSnapshotPlaylistId = "playlist-1",
            targetPlaylistId = "playlist-1",
            showPlaylistEntry = true,
        )

        assertTrue(plan.clearSnapshot)
        assertFalse(plan.showBlockingLoading)
    }

    private fun catalogSnapshot(
        categoryCounts: Map<String, Int>,
        categoryItemsLoaded: Boolean,
    ): CatalogSnapshot {
        val loadedItem = CatalogItem(
            id = "live-1",
            sourceId = "playlist-1",
            kind = ContentKind.LIVE_CHANNEL,
            title = "Spor Kanal",
            streamUrl = "http://example.test/live",
            category = "Spor",
        )
        val itemsByCategory: Map<CatalogTab, Map<String, List<CatalogItem>>> = if (categoryItemsLoaded) {
            mapOf(CatalogTab.LIVE to mapOf("Spor" to listOf(loadedItem)))
        } else {
            emptyMap()
        }
        return CatalogSnapshot(
            playlistId = "playlist-1",
            itemsById = emptyMap(),
            stats = PlaylistStats(live = 12, movies = 0, series = 0),
            tabItems = mapOf(
                CatalogTab.LIVE to emptyList(),
                CatalogTab.MOVIES to emptyList(),
                CatalogTab.SERIES to emptyList(),
            ),
            categoriesByTab = mapOf(
                CatalogTab.LIVE to categoryCounts.keys.toList(),
                CatalogTab.MOVIES to emptyList(),
                CatalogTab.SERIES to emptyList(),
            ),
            categoryCountsByTab = mapOf(
                CatalogTab.LIVE to categoryCounts,
                CatalogTab.MOVIES to emptyMap(),
                CatalogTab.SERIES to emptyMap(),
            ),
            itemsByCategory = itemsByCategory,
            seriesGroupsAll = emptyList(),
            episodesBySeries = emptyMap(),
            seasonsBySeries = emptyMap(),
            searchEntries = emptyList(),
        )
    }
}
