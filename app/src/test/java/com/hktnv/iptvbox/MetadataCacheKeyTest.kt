package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.data.catalog.metadataCacheKey
import com.hktnv.iptvbox.data.catalog.normalizedTitleForStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataCacheKeyTest {
    @Test
    fun tmdbIdIsTheCanonicalMetadataKeyWhenPresent() {
        val item = catalogItem(title = "DUNE PART TWO", tmdbId = 693134)

        val key = item.metadataCacheKey()

        assertEquals("tmdb:693134", key?.cacheKey)
        assertEquals(693134, key?.tmdbId)
        assertEquals(item.normalizedTitleForStore(), key?.normalizedTitle)
    }

    @Test
    fun normalizedTitleIsSharedAcrossPlaylistsWhenTmdbIsMissing() {
        val first = catalogItem(id = "a", sourceId = "playlist-a", title = "HABER GLOBAL")
        val second = catalogItem(id = "b", sourceId = "playlist-b", title = "Haber Global")

        assertEquals("title:haber global", first.metadataCacheKey()?.cacheKey)
        assertEquals(first.metadataCacheKey(), second.metadataCacheKey())
    }

    @Test
    fun blankTitlesDoNotCreateFallbackCacheKeys() {
        assertNull(catalogItem(title = "   ").metadataCacheKey())
    }

    private fun catalogItem(
        id: String = "item",
        sourceId: String = "playlist",
        title: String,
        tmdbId: Int? = null,
    ): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = sourceId,
            kind = ContentKind.MOVIE,
            title = title,
            streamUrl = "http://example.test/$id",
            tmdbId = tmdbId,
        )
    }
}
