package com.hktnv.iptvbox

import com.hktnv.iptvbox.state.planCatalogSnapshotLoad
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
}
