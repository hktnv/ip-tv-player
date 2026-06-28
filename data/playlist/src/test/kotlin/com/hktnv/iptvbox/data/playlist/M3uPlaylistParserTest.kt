package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.ContentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis

class M3uPlaylistParserTest {
    @Test
    fun parsesHeaderEpgAndItems() {
        val playlist = M3uPlaylistParser().parse(
            sourceId = "source-1",
            text = """
                #EXTM3U url-tvg="https://example.com/epg.xml.gz"
                #EXTINF:-1 tvg-id="trt1" tvg-name="TRT 1" group-title="Canli",TRT 1 HD
                https://cdn.example.com/trt1/index.m3u8
                #EXTINF:-1 group-title="Movies",Example Film
                https://cdn.example.com/vod/movie.mp4
            """.trimIndent(),
        )

        assertEquals(listOf("https://example.com/epg.xml.gz"), playlist.epgUrls)
        assertEquals(2, playlist.items.size)
        assertEquals("TRT 1 HD", playlist.items[0].title)
        assertEquals(ContentKind.LIVE_CHANNEL, playlist.items[0].kind)
        assertEquals(ContentKind.MOVIE, playlist.items[1].kind)
    }

    @Test
    fun keepsCommasInsideQuotedAttributesOutOfTheVisibleTitle() {
        val playlist = M3uPlaylistParser().parse(
            sourceId = "source-1",
            text = """
                #EXTM3U
                #EXTINF:-1 tvg-name="TR: My Father, Die (2016)" tvg-logo="https://image.example/poster.jpg" group-title="Yeni Eklenen Filmler",TR: My Father, Die (2016)
                http://example.com/movie/user/pass/movie.mp4
            """.trimIndent(),
        )

        assertEquals(1, playlist.items.size)
        assertEquals("My Father, Die (2016)", playlist.items.first().title)
        assertEquals("Yeni Eklenen Filmler", playlist.items.first().category)
        assertEquals(ContentKind.MOVIE, playlist.items.first().kind)
    }

    @Test
    fun extractsSeriesSeasonAndEpisodeFromCommonM3uTitles() {
        val playlist = M3uPlaylistParser().parse(
            sourceId = "source-1",
            text = """
                #EXTM3U
                #EXTINF:-1 tvg-name="DUNE: PROPHECY (2024) S1 E2" tvg-logo="https://image.example/poster.jpg" group-title="BluTV",DUNE: PROPHECY (2024) S1 E2
                http://example.com/series/user/pass/episode.mkv
            """.trimIndent(),
        )

        val item = playlist.items.first()
        assertEquals(ContentKind.EPISODE, item.kind)
        assertEquals("DUNE: PROPHECY (2024)", item.seriesTitle)
        assertEquals(1, item.seasonNumber)
        assertEquals(2, item.episodeNumber)
    }

    @Test
    fun parsesRealLargePlaylistWhenProvided() {
        val path = System.getProperty("realM3uPath").orEmpty()
            .ifBlank { System.getenv("REAL_M3U_PATH").orEmpty() }
        val file = File(path)
        assumeTrue("Provide -DrealM3uPath to run the large playlist smoke test.", file.isFile)

        lateinit var playlist: ParsedM3uPlaylist
        val elapsedMs = measureTimeMillis {
            playlist = M3uPlaylistParser().parse("real-large", file.readText())
        }

        val liveCount = playlist.items.count { it.kind == ContentKind.LIVE_CHANNEL }
        val movieCount = playlist.items.count { it.kind == ContentKind.MOVIE }
        val episodeCount = playlist.items.count { it.kind == ContentKind.EPISODE }
        println("Parsed ${playlist.items.size} items in ${elapsedMs}ms: live=$liveCount movie=$movieCount episode=$episodeCount")
        assertTrue("large playlist should contain thousands of entries", playlist.items.size >= 8_000)
        assertTrue("live channels should be detected", liveCount >= 200)
        assertTrue("movies should be detected", movieCount >= 6_000)
        assertTrue("series episodes should be detected", episodeCount >= 1_000)
    }
}
