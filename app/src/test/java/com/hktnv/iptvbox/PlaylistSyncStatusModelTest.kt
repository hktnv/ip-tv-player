package com.hktnv.iptvbox

import com.hktnv.iptvbox.model.CatalogSyncCategoryKind
import com.hktnv.iptvbox.model.CatalogSyncPhase
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.ui.playlist.toPlaylistSyncStatusUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSyncStatusModelTest {
    @Test
    fun mapsMissingStatusToCurrentCatalogMessage() {
        val model = null.toPlaylistSyncStatusUiModel()

        assertFalse(model.active)
        assertFalse(model.error)
        assertEquals(R.string.playlist_sync_current_title, model.titleRes)
        assertEquals(R.string.playlist_sync_current_body, model.bodyRes)
    }

    @Test
    fun mapsActiveSeriesCategoryToSeriesArchiveMessage() {
        val model = CatalogSyncStatus(
            playlistId = "playlist-a",
            phase = CatalogSyncPhase.ACTIVE,
            categoryName = "Netflix Türkiye",
            categoryKind = CatalogSyncCategoryKind.SERIES,
        ).toPlaylistSyncStatusUiModel()

        assertTrue(model.active)
        assertFalse(model.error)
        assertEquals(R.string.playlist_sync_active_series, model.bodyRes)
        assertEquals("Netflix Türkiye", model.bodyArg)
    }

    @Test
    fun mapsFailedStatusToFriendlyErrorMessage() {
        val model = CatalogSyncStatus(
            playlistId = "playlist-a",
            phase = CatalogSyncPhase.FAILED,
            errorMessage = "timeout",
        ).toPlaylistSyncStatusUiModel()

        assertFalse(model.active)
        assertTrue(model.error)
        assertEquals(R.string.playlist_sync_failed_title, model.titleRes)
        assertEquals(R.string.playlist_sync_failed_body, model.bodyRes)
    }
}
