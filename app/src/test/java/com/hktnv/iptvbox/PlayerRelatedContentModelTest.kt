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
        assertTrue(model.options.first { it.label == "Komedi" }.selected)
    }

    private fun live(
        id: String,
        category: String,
    ): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.LIVE_CHANNEL,
            title = "Kanal $id",
            streamUrl = "https://example.test/$id",
            category = category,
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
