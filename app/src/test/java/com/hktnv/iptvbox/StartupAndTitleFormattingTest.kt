package com.hktnv.iptvbox

import org.junit.Assert.assertEquals
import org.junit.Test
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.ui.media.readableMovieTitle
import com.hktnv.iptvbox.ui.media.restoredScreen

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
