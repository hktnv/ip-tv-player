package com.hktnv.iptvbox.navigation

import com.hktnv.iptvbox.model.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationEntriesTest {
    @Test
    fun mobileBottomNavigationIncludesSearch() {
        val entries = bottomNavEntries(hasPlaylist = true, stats = null)

        assertTrue(entries.any { it.screen == AppScreen.SEARCH })
    }

    @Test
    fun mobileBottomNavigationKeepsCompactIconCount() {
        val entries = bottomNavEntries(hasPlaylist = true, stats = null)

        assertEquals(6, entries.size)
    }
}
