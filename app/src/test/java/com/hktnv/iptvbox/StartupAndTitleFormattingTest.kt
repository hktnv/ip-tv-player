package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.playlistAutoUpdateLabel
import com.hktnv.iptvbox.ui.media.readableContentTitle
import com.hktnv.iptvbox.ui.media.readableMovieTitle
import com.hktnv.iptvbox.ui.media.restoredScreen
import com.hktnv.iptvbox.ui.media.shouldOpenDrawerFromHorizontalMediaGrid
import com.hktnv.iptvbox.ui.search.collapseSeriesSearchResults
import com.hktnv.iptvbox.ui.search.searchResultKind
import com.hktnv.iptvbox.ui.search.searchResultTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun movieTitleFormattingKeepsOriginalSourceCasing() {
        assertEquals(
            "S\u0130H\u0130RBAZLAR \u00C7ETES\u0130: DAHA",
            "S\u0130H\u0130RBAZLAR \u00C7ETES\u0130: DAHA".readableMovieTitle(),
        )
        assertEquals("THE ELECTRIC STATE - 2025", "THE ELECTRIC STATE - 2025".readableMovieTitle())
        assertEquals("The Electric State - 2025", "The Electric State - 2025".readableMovieTitle())
    }

    @Test
    fun contentTitleFormattingKeepsOriginalSourceCasingAcrossKinds() {
        assertEquals("TRT 1 HD", "TRT 1 HD".readableContentTitle())
        assertEquals("FOX TV", "FOX TV".readableContentTitle())
        assertEquals("HABER GLOBAL", "HABER GLOBAL".readableContentTitle())
        assertEquals("AVENGERS ENDGAME", "AVENGERS ENDGAME".readableContentTitle())
        assertEquals("THE ELECTRIC STATE - 2025", "THE ELECTRIC STATE - 2025".readableContentTitle())
        assertEquals("\u00C7\u00D6L GEZEGEN\u0130 B\u00D6L\u00DCM \u0130K\u0130", "\u00C7\u00D6L GEZEGEN\u0130 B\u00D6L\u00DCM \u0130K\u0130".readableContentTitle())
    }

    @Test
    fun searchDrawerOpensOnlyFromLeftColumn() {
        assertTrue(shouldOpenDrawerFromHorizontalMediaGrid(index = 0))
        assertFalse(shouldOpenDrawerFromHorizontalMediaGrid(index = 1))
        assertTrue(shouldOpenDrawerFromHorizontalMediaGrid(index = 2))
        assertFalse(shouldOpenDrawerFromHorizontalMediaGrid(index = 3))
    }

    @Test
    fun mobileSingleColumnSearchTreatsEveryResultAsLeftBoundary() {
        assertTrue(shouldOpenDrawerFromHorizontalMediaGrid(index = 0, columnCount = 1))
        assertTrue(shouldOpenDrawerFromHorizontalMediaGrid(index = 1, columnCount = 1))
        assertTrue(shouldOpenDrawerFromHorizontalMediaGrid(index = 2, columnCount = 1))
    }

    @Test
    fun playlistAutoUpdateLabelsAreUserReadable() {
        assertEquals("Kapal\u0131", playlistAutoUpdateLabel(0))
        assertEquals("6 saatte bir", playlistAutoUpdateLabel(6))
        assertEquals("Her g\u00FCn", playlistAutoUpdateLabel(24))
    }

    @Test
    fun searchCollapsesEpisodesIntoSingleSeriesResult() {
        val results = listOf(
            testEpisode("episode-1", 1),
            testEpisode("episode-2", 2),
            testMovie(),
        ).collapseSeriesSearchResults()

        assertEquals(2, results.size)
        assertEquals("PRENS", results.first().searchResultTitle())
        assertEquals(ContentKind.SERIES, results.first().searchResultKind())
        assertEquals(ContentKind.MOVIE, results.last().searchResultKind())
    }

    private fun testEpisode(id: String, episode: Int): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "source",
            kind = ContentKind.EPISODE,
            title = "PRENS S1 E$episode",
            streamUrl = "http://example.com/series/prens/$episode.mkv",
            category = "Diziler",
            seriesTitle = "PRENS",
            seasonNumber = 1,
            episodeNumber = episode,
            providerOrder = episode,
        )
    }

    private fun testMovie(): CatalogItem {
        return CatalogItem(
            id = "movie-1",
            sourceId = "source",
            kind = ContentKind.MOVIE,
            title = "THE ELECTRIC STATE - 2025",
            streamUrl = "http://example.com/movie/1.mp4",
            category = "Filmler",
        )
    }
}
