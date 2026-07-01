package com.hktnv.iptvbox

import com.hktnv.iptvbox.data.playlist.PlaylistLoadProgress
import com.hktnv.iptvbox.data.playlist.PlaylistLoadStage
import com.hktnv.iptvbox.state.contentProgressLabel
import com.hktnv.iptvbox.state.toPlaylistImportProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistProgressMapperTest {
    @Test
    fun formatsProcessedAndTotalItemProgress() {
        assertEquals("125 / 8822 içerik", contentProgressLabel(125, 8_822))
        assertEquals("125 içerik", contentProgressLabel(125, null))
    }

    @Test
    fun capsProcessedItemProgressAtTotalCount() {
        assertEquals("8822 / 8822 içerik", contentProgressLabel(9_000, 8_822))
    }

    @Test
    fun mapsCompletedLoaderProgressToInactiveUiState() {
        val progress = PlaylistLoadProgress(
            stage = PlaylistLoadStage.COMPLETED,
            processedItems = 8_822,
            totalItems = 8_822,
        ).toPlaylistImportProgress("playlist-1")

        assertEquals("playlist-1", progress.playlistId)
        assertEquals(8_822, progress.processedItems)
        assertTrue(progress.complete)
        assertFalse(progress.active)
    }
}
