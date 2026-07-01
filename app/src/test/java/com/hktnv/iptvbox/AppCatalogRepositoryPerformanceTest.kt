package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import kotlin.system.measureTimeMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.data.catalog.episodes
import com.hktnv.iptvbox.data.catalog.seasons
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.ui.media.stats

class AppCatalogRepositoryPerformanceTest {
    @Test
    fun indexesAndSearchesLargeCatalog() {
        val playlist = LoadedPlaylist(
            id = "large",
            name = "Large M3U",
            type = PlaylistSourceType.M3U_URL,
            endpoint = "https://example.com/list.m3u",
            headers = emptyMap(),
            items = generateLargeCatalog(),
            epgUrls = emptyList(),
            warnings = emptyList(),
        )
        val repository = AppCatalogRepository()
        lateinit var snapshot: CatalogSnapshot

        val indexMs = measureTimeMillis {
            snapshot = repository.buildSnapshot(playlist)
        }
        val movieCategoryMs = measureTimeMillis {
            assertTrue(snapshot.visibleItems(CatalogTab.MOVIES, "Movies Action").isNotEmpty())
        }
        val seriesSeasonMs = measureTimeMillis {
            assertEquals(2, snapshot.seasons("Series 001").size)
            assertTrue(snapshot.episodes("Series 001", 1).isNotEmpty())
        }
        val searchMs = measureTimeMillis {
            val results = repository.search(snapshot, "movie 123", limit = 50)
            assertTrue(results.isNotEmpty())
            assertTrue(results.size <= 50)
        }

        println(
            "PERF largeCatalog items=${playlist.items.size} " +
                "indexMs=$indexMs movieCategoryMs=$movieCategoryMs " +
                "seriesSeasonMs=$seriesSeasonMs searchMs=$searchMs",
        )
        assertEquals(8_399, playlist.items.size)
        assertEquals(269, snapshot.stats.live)
        assertEquals(6_714, snapshot.stats.movies)
        assertEquals(102, snapshot.stats.series)
        assertTrue("Large catalog indexing should stay off the UI path", indexMs < 5_000)
        assertTrue("Indexed category lookup should be effectively instant", movieCategoryMs < 250)
        assertTrue("Indexed series lookup should be effectively instant", seriesSeasonMs < 250)
        assertTrue("Search should use the precomputed index", searchMs < 1_000)
    }

    @Test
    fun categoriesAreSortedByContentCount() {
        val playlist = LoadedPlaylist(
            id = "category-order",
            name = "Category order",
            type = PlaylistSourceType.M3U_URL,
            endpoint = "https://example.com/list.m3u",
            headers = emptyMap(),
            items = listOf(
                categoryItem("news-1", "News", 0),
                categoryItem("sports-1", "Sports", 1),
                categoryItem("sports-2", "Sports", 2),
                categoryItem("kids-1", "Kids", 3),
                categoryItem("kids-2", "Kids", 4),
                categoryItem("kids-3", "Kids", 5),
            ),
            epgUrls = emptyList(),
            warnings = emptyList(),
        )

        val snapshot = AppCatalogRepository().buildSnapshot(playlist)

        assertEquals(listOf("Kids", "Sports", "News"), snapshot.categories(CatalogTab.LIVE))
    }

    private fun generateLargeCatalog(): List<CatalogItem> {
        val items = ArrayList<CatalogItem>(8_399)
        repeat(269) { index ->
            val number = index + 1
            items += CatalogItem(
                id = "live-$number",
                sourceId = "large",
                kind = ContentKind.LIVE_CHANNEL,
                title = "Live Channel $number",
                streamUrl = "http://example.com/live/$number.ts",
                category = when {
                    number % 5 == 0 -> "Kids"
                    number % 3 == 0 -> "Sports"
                    else -> "National"
                },
                logoUrl = "https://img.example.com/live/$number.png",
                providerOrder = items.size,
            )
        }
        repeat(6_714) { index ->
            val number = index + 1
            items += CatalogItem(
                id = "movie-$number",
                sourceId = "large",
                kind = ContentKind.MOVIE,
                title = "Movie $number",
                streamUrl = "http://example.com/movie/$number.mp4",
                category = when {
                    number % 4 == 0 -> "Movies Family"
                    number % 2 == 0 -> "Movies Action"
                    else -> "Movies Drama"
                },
                logoUrl = "https://img.example.com/movie/$number.jpg",
                providerOrder = items.size,
            )
        }
        repeat(102) { seriesIndex ->
            val seriesNumber = seriesIndex + 1
            val episodeCount = if (seriesNumber <= 96) 14 else 12
            repeat(episodeCount) { episodeIndex ->
                val episodeNumber = episodeIndex + 1
                val seasonNumber = if (episodeNumber <= 7) 1 else 2
                val episodeInSeason = if (seasonNumber == 1) episodeNumber else episodeNumber - 7
                items += CatalogItem(
                    id = "series-$seriesNumber-episode-$episodeNumber",
                    sourceId = "large",
                    kind = ContentKind.EPISODE,
                    title = "Series ${seriesNumber.toString().padStart(3, '0')} S${seasonNumber.toString().padStart(2, '0')} E${episodeInSeason.toString().padStart(2, '0')}",
                    streamUrl = "http://example.com/series/$seriesNumber/$episodeNumber.mkv",
                    category = if (seriesNumber % 2 == 0) "Series Comedy" else "Series Drama",
                    logoUrl = "https://img.example.com/series/$seriesNumber.jpg",
                    seriesTitle = "Series ${seriesNumber.toString().padStart(3, '0')}",
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeInSeason,
                    providerOrder = items.size,
                )
            }
        }
        return items
    }

    private fun categoryItem(id: String, category: String, order: Int): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "category-order",
            kind = ContentKind.LIVE_CHANNEL,
            title = id,
            streamUrl = "http://example.com/live/$id.ts",
            category = category,
            providerOrder = order,
        )
    }
}
