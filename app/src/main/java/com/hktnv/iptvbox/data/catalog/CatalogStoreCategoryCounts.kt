package com.hktnv.iptvbox.data.catalog

import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.CatalogTab

internal fun CatalogStore.categoryCounts(playlistId: String, tab: CatalogTab): Map<String, Int> {
    return readableDatabase.rawQuery(
        """
        SELECT categories.name, COUNT(items.item_id) AS item_count
        FROM categories
        LEFT JOIN items ON items.category_id = categories.id
        WHERE categories.playlist_id=? AND categories.kind=?
        GROUP BY categories.id, categories.name
        """.trimIndent(),
        arrayOf(playlistId, tab.categoryKindForStore()),
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                put(cursor.getString(0), cursor.getInt(cursor.column("item_count")))
            }
        }
    }
}

internal fun CatalogStore.seriesCategoryCounts(playlistId: String): Map<String, Int> {
    return readableDatabase.rawQuery(
        """
        SELECT categories.name, COUNT(DISTINCT items.series_title) AS series_count
        FROM categories
        LEFT JOIN items ON items.category_id = categories.id
        WHERE categories.playlist_id=?
            AND categories.kind=?
            AND items.kind IN (?,?,?)
            AND items.series_title IS NOT NULL
            AND items.series_title != ''
        GROUP BY categories.id, categories.name
        """.trimIndent(),
        arrayOf(
            playlistId,
            CategoryKindSeries,
            ContentKind.SERIES.name,
            ContentKind.SEASON.name,
            ContentKind.EPISODE.name,
        ),
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                put(cursor.getString(0), cursor.getInt(cursor.column("series_count")))
            }
        }
    }
}
