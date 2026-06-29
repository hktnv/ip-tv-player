package com.hktnv.iptvbox.data.catalog

import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.CatalogTab

internal fun CatalogStore.categoryCounts(playlistId: String, tab: CatalogTab): Map<String, Int> {
    val kindPlaceholders = tab.kinds.joinToString(",") { "?" }
    return readableDatabase.rawQuery(
        """
        SELECT category, COUNT(*) AS item_count
        FROM items
        WHERE playlist_id=?
            AND kind IN ($kindPlaceholders)
            AND category IS NOT NULL
            AND category != ''
        GROUP BY category
        """.trimIndent(),
        (listOf(playlistId) + tab.kinds.map { it.name }).toTypedArray(),
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
        SELECT category, COUNT(DISTINCT series_title) AS series_count
        FROM items
        WHERE playlist_id=?
            AND kind IN (?,?,?)
            AND category IS NOT NULL
            AND category != ''
            AND series_title IS NOT NULL
            AND series_title != ''
        GROUP BY category
        """.trimIndent(),
        arrayOf(
            playlistId,
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
