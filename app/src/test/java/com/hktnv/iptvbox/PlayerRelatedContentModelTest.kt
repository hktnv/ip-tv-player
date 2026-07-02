package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.player.buildPlayerRelatedContentModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRelatedContentModelTest {
    @Test
    fun seriesDefaultsToCurrentSeasonAndExcludesCurrentEpisode() {
        val current = episode("s1e2", season = 1, episode = 2)
        val model = buildPlayerRelatedContentModel(
            currentItem = current,
            contextItems = listOf(
                episode("s1e1", season = 1, episode = 1),
                current,
                episode("s2e1", season = 2, episode = 1),
            ),
            selectedOptionId = null,
        )

        assertEquals(listOf("s1e1"), model.items.map { it.id })
        assertTrue(model.options.first { it.seasonNumber == 1 }.selected)
        assertFalse(model.items.any { it.id == current.id })
    }

    @Test
    fun selectedSeasonUpdatesEpisodeCardsInSameModel() {
        val current = episode("s1e1", season = 1, episode = 1)
        val model = buildPlayerRelatedContentModel(
            currentItem = current,
            contextItems = listOf(
                current,
                episode("s1e2", season = 1, episode = 2),
                episode("s2e1", season = 2, episode = 1),
                episode("s2e2", season = 2, episode = 2),
            ),
            selectedOptionId = "season:2",
        )

        assertEquals(listOf("s2e1", "s2e2"), model.items.map { it.id })
        assertTrue(model.options.first { it.seasonNumber == 2 }.selected)
    }

    @Test
    fun categoryModelDefaultsToCurrentCategoryAndExcludesCurrentItem() {
        val current = live("live-2", category = "Haber")
        val model = buildPlayerRelatedContentModel(
            currentItem = current,
            contextItems = listOf(
                live("live-1", category = "Haber"),
                current,
                live("live-3", category = "Spor"),
            ),
            selectedOptionId = null,
        )

        assertEquals(listOf("live-1"), model.items.map { it.id })
        assertTrue(model.options.first { it.label == "Haber" }.selected)
    }

    @Test
    fun selectedCategoryUpdatesCardsAndKeepsListCapped() {
        val current = movie("movie-current", category = "Aksiyon")
        val comedyItems = (1..18).map { movie("comedy-$it", category = "Komedi", order = it) }
        val model = buildPlayerRelatedContentModel(
            currentItem = current,
            contextItems = listOf(current) + comedyItems,
            selectedOptionId = "category:komedi",
        )

        assertEquals((1..16).map { "comedy-$it" }, model.items.map { it.id })
        assertEquals(18, model.totalItemCount)
        assertTrue(model.hasMoreItems)
        assertTrue(model.options.first { it.label == "Komedi" }.selected)
    }

    @Test
    fun categoryModelLoadsSecondBatchWhenLimitIncreases() {
        val current = movie("movie-current", category = "Yeni Eklenen Filmler")
        val categoryItems = (1..45).map {
            movie("movie-$it", category = "Yeni Eklenen Filmler", order = it)
        }
        val model = buildPlayerRelatedContentModel(
            currentItem = current,
            contextItems = listOf(current) + categoryItems,
            selectedOptionId = null,
            itemLimit = 32,
        )

        assertEquals(45, model.totalItemCount)
        assertEquals(32, model.items.size)
        assertEquals("movie-32", model.items.last().id)
        assertTrue(model.hasMoreItems)
    }

    @Test
    fun normalizedCategoryMatchingKeepsForeignLiveCategoriesPopulated() {
        val current = live("canada-current", category = " Canada ")
        val canadaChannels = (1..118).map { live("canada-$it", category = "Canada", order = it) }
        val model = buildPlayerRelatedContentModel(
            currentItem = current,
            contextItems = listOf(current) + canadaChannels + listOf(live("news-1", category = "News")),
            selectedOptionId = "category:canada",
        )

        assertEquals(118, model.totalItemCount)
        assertEquals(16, model.items.size)
        assertFalse(model.items.any { it.id == current.id })
        assertTrue(model.items.all { it.category?.trim() == "Canada" })
    }

    @Test
    fun showsDiscoveryWhenCurrentCategoryOnlyContainsCurrentItem() {
        val current = movie("movie-current", category = "Aksiyon")
        val model = buildPlayerRelatedContentModel(
            currentItem = current,
            contextItems = listOf(
                current,
                movie("comedy-1", category = "Komedi"),
                movie("drama-1", category = "Dram"),
            ),
            selectedOptionId = null,
        )

        assertTrue(model.hasContent)
        assertEquals(emptyList<CatalogItem>(), model.items)
        assertEquals(listOf("Aksiyon", "Komedi", "Dram"), model.options.map { it.label })
    }

    private fun live(
        id: String,
        category: String,
        order: Int = 0,
    ): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.LIVE_CHANNEL,
            title = "Kanal $id",
            streamUrl = "https://example.test/$id",
            category = category,
            providerOrder = order,
        )
    }

    private fun movie(
        id: String,
        category: String,
        order: Int = 0,
    ): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.MOVIE,
            title = "Film $id",
            streamUrl = "https://example.test/$id",
            category = category,
            providerOrder = order,
        )
    }

    private fun episode(
        id: String,
        season: Int,
        episode: Int,
    ): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.EPISODE,
            title = "Dizi S$season E$episode",
            streamUrl = "https://example.test/$id",
            category = "Dizi",
            seriesTitle = "Dizi",
            seasonNumber = season,
            episodeNumber = episode,
        )
    }
}
