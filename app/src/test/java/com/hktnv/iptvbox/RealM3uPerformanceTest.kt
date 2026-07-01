package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.data.playlist.M3uPlaylistParser
import com.hktnv.iptvbox.data.playlist.ParsedM3uPlaylist
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.ui.media.normalizedForUi
import java.io.File
import kotlin.system.measureTimeMillis
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RealM3uPerformanceTest {
    @Test
    fun parsesNormalizesAndIndexesDesktopM3u() {
        val path = System.getProperty("iptv.performance.m3u")
            ?: System.getenv("REAL_M3U_PATH")
        assumeTrue("Local performance M3U path is not configured", !path.isNullOrBlank())
        val file = File(path.orEmpty())
        assumeTrue("Local performance M3U not found: ${file.absolutePath}", file.isFile)

        val parser = M3uPlaylistParser()
        lateinit var parsedPlaylist: ParsedM3uPlaylist
        lateinit var playlist: LoadedPlaylist
        lateinit var snapshot: CatalogSnapshot

        val parseMs = measureTimeMillis {
            parsedPlaylist = file.useLines { lines ->
                parser.parse(sourceId = "real-m3u", lines = lines, measureStages = true)
            }
            playlist = LoadedPlaylist(
                id = "real-m3u",
                name = "Desktop M3U",
                type = PlaylistSourceType.M3U_URL,
                endpoint = file.absolutePath,
                headers = emptyMap(),
                items = parsedPlaylist.items,
                epgUrls = parsedPlaylist.epgUrls,
                warnings = emptyList(),
            )
        }
        val normalizeMs = measureTimeMillis {
            playlist = playlist.normalizedForUi()
        }
        val indexMs = measureTimeMillis {
            snapshot = AppCatalogRepository().buildSnapshot(playlist)
        }
        val searchMs = measureTimeMillis {
            AppCatalogRepository().search(snapshot, "film", 300)
        }
        val ramMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L)

        println(
            "PERF realM3u fileBytes=${file.length()} items=${playlist.cachedItemCount} " +
                "live=${playlist.cachedLiveCount} movies=${playlist.cachedMovieCount} series=${playlist.cachedSeriesCount} " +
                "parseMs=$parseMs parserReportedMs=${parsedPlaylist.parseMs} " +
                "contentCleanMs=${parsedPlaylist.contentCleaningMs} seriesDetectMs=${parsedPlaylist.seriesMs} " +
                "kindClassifyMs=${parsedPlaylist.classificationMs} categoryMs=${parsedPlaylist.categoryMs} " +
                "parseOtherMs=${parsedPlaylist.parseOtherMs} normalizeMs=$normalizeMs indexMs=$indexMs " +
                "searchMs=$searchMs ramMb=$ramMb",
        )

        assertTrue("Expected a large playlist", (playlist.cachedItemCount ?: 0) >= 8_000)
        assertTrue("Parser should stay below multi-minute device-failure territory in local code", parseMs < 10_000)
        assertTrue("Repository indexing should stay off the UI path", indexMs < 5_000)
    }
}
