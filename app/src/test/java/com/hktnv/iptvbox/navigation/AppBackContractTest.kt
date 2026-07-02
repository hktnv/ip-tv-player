package com.hktnv.iptvbox.navigation

import com.hktnv.iptvbox.model.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Test

class AppBackContractTest {
    @Test
    fun playlistHubWithoutReturnAddressOpensExitConfirmation() {
        assertEquals(
            RootBackAction.ShowExitConfirmation,
            resolveRootBackAction(
                wide = true,
                contentBackBlocked = false,
                showPlaylistEntry = true,
                playlistDetailId = null,
                playlistEntryReturnScreen = null,
                screen = AppScreen.PLAYLISTS,
                sideMenuExpanded = false,
            ),
        )
    }

    @Test
    fun playlistHubWithReturnAddressReturnsToOrigin() {
        assertEquals(
            RootBackAction.ReturnToPlaylistEntryOrigin,
            resolveRootBackAction(
                wide = true,
                contentBackBlocked = false,
                showPlaylistEntry = true,
                playlistDetailId = null,
                playlistEntryReturnScreen = AppScreen.SETTINGS,
                screen = AppScreen.PLAYLISTS,
                sideMenuExpanded = false,
            ),
        )
    }

    @Test
    fun wideContentBackOpensDrawerBeforeExitConfirmation() {
        assertEquals(
            RootBackAction.OpenNavigationDrawer,
            resolveRootBackAction(
                wide = true,
                contentBackBlocked = false,
                showPlaylistEntry = false,
                playlistDetailId = null,
                playlistEntryReturnScreen = null,
                screen = AppScreen.HOME,
                sideMenuExpanded = false,
            ),
        )
        assertEquals(
            RootBackAction.ShowExitConfirmation,
            resolveRootBackAction(
                wide = true,
                contentBackBlocked = false,
                showPlaylistEntry = false,
                playlistDetailId = null,
                playlistEntryReturnScreen = null,
                screen = AppScreen.HOME,
                sideMenuExpanded = true,
            ),
        )
    }

    @Test
    fun compactContentBackOpensExitConfirmation() {
        assertEquals(
            RootBackAction.ShowExitConfirmation,
            resolveRootBackAction(
                wide = false,
                contentBackBlocked = false,
                showPlaylistEntry = false,
                playlistDetailId = null,
                playlistEntryReturnScreen = null,
                screen = AppScreen.SETTINGS,
                sideMenuExpanded = false,
            ),
        )
    }

    @Test
    fun nestedContentBackKeepsPriority() {
        assertEquals(
            RootBackAction.None,
            resolveRootBackAction(
                wide = true,
                contentBackBlocked = true,
                showPlaylistEntry = false,
                playlistDetailId = null,
                playlistEntryReturnScreen = null,
                screen = AppScreen.CATALOG,
                sideMenuExpanded = false,
            ),
        )
    }
}
