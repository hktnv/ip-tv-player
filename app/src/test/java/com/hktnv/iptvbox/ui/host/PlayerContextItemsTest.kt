package com.hktnv.iptvbox.ui.host

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.PlaylistStats
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerContextItemsTest {
    @Test
    fun liveItemUsesItsOwnCategoryContext() {
        val newsOne = item("news-1", ContentKind.LIVE_CHANNEL, "Haber 1", "Haber")
        val newsTwo = item("news-2", ContentKind.LIVE_CHANNEL, "Haber 2", "Haber")
        val sport = item("sport-1", ContentKind.LIVE_CHANNEL, "Spor 1", "Spor")
        val snapshot = snapshot(listOf(newsOne, newsTwo, sport))

        val context = snapshot.playbackContextItemsFor(newsOne)

        assertEquals(listOf(newsOne, newsTwo), context)
    }

    @Test
    fun episodeUsesSeriesAndSeasonContext() {
        val first = episode("dune-s1e1", "Dune", 1)
        val second = episode("dune-s1e2", "Dune", 1)
        val nextSeason = episode("dune-s2e1", "Dune", 2)
        val other = episode("prens-s1e1", "Prens", 1)
        val snapshot = snapshot(listOf(first, second, nextSeason, other))

        val context = snapshot.playbackContextItemsFor(first)

        assertEquals(listOf(first, second), context)
    }

    private fun snapshot(items: List<CatalogItem>): CatalogSnapshot {
        val tabItems = CatalogTab.entries.associateWith { tab ->
            items.filter { item -> item.tab() == tab }
        }
        val itemsByCategory = CatalogTab.entries.associateWith { tab ->
            tabItems.getValue(tab)
                .groupBy { it.category.orEmpty() }
                .filterKeys { it.isNotBlank() }
        }
        return CatalogSnapshot(
            playlistId = "playlist",
            itemsById = items.associateBy { it.id },
            stats = PlaylistStats(
                live = tabItems.getValue(CatalogTab.LIVE).size,
                movies = tabItems.getValue(CatalogTab.MOVIES).size,
                series = tabItems.getValue(CatalogTab.SERIES).size,
            ),
            tabItems = tabItems,
            categoriesByTab = itemsByCategory.mapValues { it.value.keys.toList() },
            categoryCountsByTab = itemsByCategory.mapValues { (_, values) -> values.mapValues { it.value.size } },
            itemsByCategory = itemsByCategory,
            seriesGroupsAll = emptyList(),
            episodesBySeries = items.filter { it.kind == ContentKind.EPISODE }.groupBy { it.seriesTitle.orEmpty() },
            seasonsBySeries = emptyMap(),
            searchEntries = emptyList(),
        )
    }

    private fun item(id: String, kind: ContentKind, title: String, category: String): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = kind,
            title = title,
            streamUrl = "https://example.test/$id",
            category = category,
        )
    }

    private fun episode(id: String, seriesTitle: String, season: Int): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.EPISODE,
            title = "$seriesTitle S$season",
            streamUrl = "https://example.test/$id",
            category = "Dizi",
            seriesTitle = seriesTitle,
            seasonNumber = season,
        )
    }

    private fun CatalogItem.tab(): CatalogTab {
        return when (kind) {
            ContentKind.LIVE_CHANNEL,
            ContentKind.RADIO -> CatalogTab.LIVE
            ContentKind.MOVIE -> CatalogTab.MOVIES
            ContentKind.SERIES,
            ContentKind.SEASON,
            ContentKind.EPISODE -> CatalogTab.SERIES
        }
    }
}
