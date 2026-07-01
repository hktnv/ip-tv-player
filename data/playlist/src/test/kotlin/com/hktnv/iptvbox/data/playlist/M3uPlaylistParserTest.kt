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
    fun keepsLiveSeriesNamedChannelsOutOfSeriesCatalog() {
        val playlist = M3uPlaylistParser().parse(
            sourceId = "source-1",
            text = """
                #EXTM3U
                #EXTINF:-1 tvg-name="TR: BEIN SERIES 1 HD" group-title="Türk Sinema",TR: BEIN SERIES 1 HD
                http://example.com/live/user/pass/channel.ts
            """.trimIndent(),
        )

        val item = playlist.items.first()
        assertEquals(ContentKind.LIVE_CHANNEL, item.kind)
        assertEquals(null, item.seriesTitle)
    }

    @Test
    fun keepsBeinSportsChannelsOutOfMovieCatalog() {
        val playlist = M3uPlaylistParser().parse(
            sourceId = "source-1",
            text = """
                #EXTM3U
                #EXTINF:-1 tvg-name="TR:BEINSPORTS 1 HQ (VODAFONE-TURKCELL-TURKIYE)" group-title="Türk Spor",TR:BEINSPORTS 1 HQ (VODAFONE-TURKCELL-TURKIYE)
                http://example.com/live/user/pass/beinsports-1.ts
            """.trimIndent(),
        )

        val item = playlist.items.first()
        assertEquals(ContentKind.LIVE_CHANNEL, item.kind)
        assertEquals("Türk Spor", item.category)
        assertEquals(null, item.seriesTitle)
    }

    @Test
    fun keepsMovieTitlesWithEpisodeWordsOutOfSeriesCatalog() {
        val playlist = M3uPlaylistParser().parse(
            sourceId = "source-1",
            text = """
                #EXTM3U
                #EXTINF:-1 tvg-name="DUNE: ÇÖL GEZEGENİ BÖLÜM İKİ 2024" group-title="Türk Bilim Kurgu & Fantastik",DUNE: ÇÖL GEZEGENİ BÖLÜM İKİ 2024
                http://example.com/movie/user/pass/dune-part-two.mp4
            """.trimIndent(),
        )

        val item = playlist.items.first()
        assertEquals(ContentKind.MOVIE, item.kind)
        assertEquals(null, item.seriesTitle)
    }

    @Test
    fun parsesRealLargePlaylistWhenProvided() {
        val path = System.getProperty("realM3uPath").orEmpty()
            .ifBlank { System.getenv("REAL_M3U_PATH").orEmpty() }
        val file = File(path)
        assumeTrue("Provide -DrealM3uPath to run the large playlist smoke test.", file.isFile)

        lateinit var playlist: ParsedM3uPlaylist
        val elapsedMs = measureTimeMillis {
            playlist = file.useLines { lines -> M3uPlaylistParser().parse("real-large", lines) }
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

    @Test
    fun parsesLargePlaylistInSingleSequencePass() {
        val source = SingleUseSequence(syntheticLargeM3uLines(live = 320, movies = 6_900, series = 240))

        val playlist = M3uPlaylistParser().parse("large-stream", source)

        assertEquals(1, source.iteratorCount)
        assertEquals(7_460, playlist.items.size)
        assertEquals(320, playlist.items.count { it.kind == ContentKind.LIVE_CHANNEL })
        assertEquals(6_900, playlist.items.count { it.kind == ContentKind.MOVIE })
        assertEquals(240, playlist.items.count { it.kind == ContentKind.EPISODE })
        assertTrue(playlist.parseMs >= playlist.parseOtherMs)
    }

    @Test
    fun reportsStreamingItemProgress() {
        val progress = mutableListOf<Int>()
        val source = SingleUseSequence(syntheticLargeM3uLines(live = 3, movies = 120, series = 2))

        val playlist = M3uPlaylistParser().parse("progress-stream", source) { count ->
            progress += count
        }

        assertEquals(125, playlist.items.size)
        assertEquals(listOf(1, 100, 125), progress)
        assertEquals(1, source.iteratorCount)
    }

    private fun syntheticLargeM3uLines(live: Int, movies: Int, series: Int): Sequence<String> = sequence {
        yield("#EXTM3U url-tvg=\"https://example.com/epg.xml\"")
        repeat(live) { index ->
            val number = index + 1
            yield("""#EXTINF:-1 tvg-name="TRT $number HD" group-title="Türk Ulusal",TRT $number HD""")
            yield("http://stream.example.com/live/user/pass/$number.ts")
        }
        repeat(movies) { index ->
            val number = index + 1
            yield("""#EXTINF:-1 tvg-name="MOVIE $number" group-title="Yeni Eklenen Filmler",MOVIE $number""")
            yield("http://stream.example.com/movie/user/pass/$number.mp4")
        }
        repeat(series) { index ->
            val number = index + 1
            yield("""#EXTINF:-1 tvg-name="SERIES $number S1 E1" group-title="Diziler",SERIES $number S1 E1""")
            yield("http://stream.example.com/series/user/pass/$number.mkv")
        }
    }

    private class SingleUseSequence<T>(
        private val delegate: Sequence<T>,
    ) : Sequence<T> {
        var iteratorCount: Int = 0
            private set

        override fun iterator(): Iterator<T> {
            iteratorCount += 1
            check(iteratorCount == 1) { "Parser must consume streaming lines once." }
            return delegate.iterator()
        }
    }
}
