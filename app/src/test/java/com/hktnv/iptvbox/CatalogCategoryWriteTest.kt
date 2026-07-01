package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.data.catalog.CategoryKindMovie
import com.hktnv.iptvbox.data.catalog.buildCategoryRows
import com.hktnv.iptvbox.data.catalog.catalogCategoryId
import com.hktnv.iptvbox.data.playlist.xtream.XtreamCategoryMapping
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogCategoryWriteTest {
    @Test
    fun categoryRowsCarryXtreamCategoryIdsByKindAndName() {
        val rows = buildCategoryRows(
            playlistId = "playlist",
            items = listOf(
                CatalogItem(
                    id = "movie-1",
                    sourceId = "playlist",
                    kind = ContentKind.MOVIE,
                    title = "Film",
                    streamUrl = "http://example.test/movie",
                    category = "Aksiyon",
                ),
            ),
            mappings = listOf(XtreamCategoryMapping(CategoryKindMovie, "Aksiyon", "42")),
        )

        val row = rows.getValue(catalogCategoryId("playlist", CategoryKindMovie, "Aksiyon"))
        assertEquals("Aksiyon", row.name)
        assertEquals(CategoryKindMovie, row.kind)
        assertEquals("42", row.xtreamCategoryId)
    }
}
