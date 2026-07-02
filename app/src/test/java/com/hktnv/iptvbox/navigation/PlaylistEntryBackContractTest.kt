package com.hktnv.iptvbox.navigation

import com.hktnv.iptvbox.model.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistEntryBackContractTest {
    @Test
    fun settingsCanBeTheReturnAddressForPlaylistHub() {
        assertEquals(AppScreen.SETTINGS, playlistEntryReturnScreen(AppScreen.SETTINGS))
    }

    @Test
    fun playlistHubAndPlayerAreNotValidReturnAddresses() {
        assertNull(playlistEntryReturnScreen(AppScreen.PLAYLISTS))
        assertNull(playlistEntryReturnScreen(AppScreen.PLAYER))
    }

    @Test
    fun playlistHubBackIsHandledOnlyWhenReturnAddressExists() {
        assertTrue(
            shouldHandlePlaylistEntryBack(
                showPlaylistEntry = true,
                playlistDetailId = null,
                returnScreen = AppScreen.SETTINGS,
            ),
        )
        assertFalse(
            shouldHandlePlaylistEntryBack(
                showPlaylistEntry = true,
                playlistDetailId = "playlist-1",
                returnScreen = AppScreen.SETTINGS,
            ),
        )
        assertFalse(
            shouldHandlePlaylistEntryBack(
                showPlaylistEntry = true,
                playlistDetailId = null,
                returnScreen = null,
            ),
        )
    }
}
