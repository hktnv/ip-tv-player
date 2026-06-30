package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.state.playlistStateAfterDeletion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistDeletionStateTest {
    @Test
    fun `deleting active playlist selects next playlist and returns home`() {
        val state = playlistStateAfterDeletion(
            remainingPlaylists = listOf(playlist("next")),
            deletedPlaylistId = "active",
            selectedPlaylistId = "active",
            currentScreen = AppScreen.CATALOG,
        )

        assertEquals("next", state.selectedPlaylistId)
        assertEquals(AppScreen.HOME, state.screen)
        assertFalse(state.showPlaylistEntry)
        assertTrue(state.deletedActivePlaylist)
    }

    @Test
    fun `deleting inactive playlist keeps current context`() {
        val state = playlistStateAfterDeletion(
            remainingPlaylists = listOf(playlist("active")),
            deletedPlaylistId = "old",
            selectedPlaylistId = "active",
            currentScreen = AppScreen.SEARCH,
        )

        assertEquals("active", state.selectedPlaylistId)
        assertEquals(AppScreen.SEARCH, state.screen)
        assertFalse(state.showPlaylistEntry)
        assertFalse(state.deletedActivePlaylist)
    }

    @Test
    fun `deleting last playlist returns to playlist entry`() {
        val state = playlistStateAfterDeletion(
            remainingPlaylists = emptyList(),
            deletedPlaylistId = "last",
            selectedPlaylistId = "last",
            currentScreen = AppScreen.HOME,
        )

        assertNull(state.selectedPlaylistId)
        assertEquals(AppScreen.PLAYLISTS, state.screen)
        assertTrue(state.showPlaylistEntry)
        assertTrue(state.deletedActivePlaylist)
    }

    private fun playlist(id: String): LoadedPlaylist = LoadedPlaylist(
        id = id,
        name = "Liste $id",
        type = PlaylistSourceType.M3U_URL,
        endpoint = "https://example.invalid/$id.m3u",
        headers = emptyMap(),
        items = emptyList(),
        epgUrls = emptyList(),
        warnings = emptyList(),
    )
}
