package com.evomrdm.iptvbox

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupAndTitleFormattingTest {
    @Test
    fun restoredScreenAlwaysStartsAtHomeInsteadOfSettingsWhenPlaylistExists() {
        assertEquals(AppScreen.HOME, restoredScreen(AppScreen.SETTINGS.name, hasPlaylist = true))
        assertEquals(AppScreen.HOME, restoredScreen(AppScreen.CATALOG.name, hasPlaylist = true))
    }

    @Test
    fun restoredScreenUsesHomeEntryFlowWhenPlaylistDoesNotExist() {
        assertEquals(AppScreen.HOME, restoredScreen(AppScreen.SETTINGS.name, hasPlaylist = false))
        assertEquals(AppScreen.HOME, restoredScreen(AppScreen.CATALOG.name, hasPlaylist = false))
    }

    @Test
    fun movieTitleCaseKeepsReadableMixedCaseAndFormatsAllCaps() {
        assertEquals("Sihirbazlar Çetesi: Daha", "SİHİRBAZLAR ÇETESİ: DAHA".readableMovieTitle())
        assertEquals("The Electric State - 2025", "THE ELECTRIC STATE - 2025".readableMovieTitle())
        assertEquals("The Electric State - 2025", "The Electric State - 2025".readableMovieTitle())
    }
}
