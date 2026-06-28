package com.hktnv.iptvbox.data.catalog
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.SeasonGroup

internal fun CatalogStore.seasons(playlistId: String, seriesTitle: String): List<SeasonGroup> {
    return readableDatabase.rawQuery(
        """
        SELECT
            COALESCE(season_number, 1) AS season_number,
            COUNT(*) AS episode_count,
            MIN(logo_url) AS logo_url,
            MIN(provider_order) AS first_order
        FROM items
        WHERE playlist_id=? AND series_title=?
        GROUP BY COALESCE(season_number, 1)
        ORDER BY season_number ASC, first_order ASC
        """.trimIndent(),
        arrayOf(playlistId, seriesTitle),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val season = cursor.getInt(cursor.column("season_number"))
                add(
                    SeasonGroup(
                        id = "$seriesTitle-$season",
                        title = "Sezon $season",
                        seasonNumber = season,
                        episodeCount = cursor.getInt(cursor.column("episode_count")),
                        logoUrl = cursor.nullableString("logo_url"),
                        firstOrder = cursor.getInt(cursor.column("first_order")),
                    ),
                )
            }
        }
    }
}

internal fun CatalogStore.episodes(
    playlistId: String,
    seriesTitle: String,
    seasonNumber: Int?,
    limit: Int,
): List<CatalogItem> {
    val seasonClause = if (seasonNumber == null) "" else " AND COALESCE(season_number, 1)=?"
    val args = mutableListOf(playlistId, seriesTitle).apply {
        if (seasonNumber != null) add(seasonNumber.toString())
        add(limit.toString())
    }
    return readableDatabase.rawQuery(
        """
        SELECT * FROM items
        WHERE playlist_id=? AND series_title=?
            $seasonClause
        ORDER BY COALESCE(season_number, 1) ASC, COALESCE(episode_number, provider_order) ASC, provider_order ASC
        LIMIT ?
        """.trimIndent(),
        args.toTypedArray(),
    ).use { cursor -> cursor.toItems() }
}

internal fun CatalogStore.categories(playlistId: String, tab: CatalogTab): List<String> {
    val kindPlaceholders = tab.kinds.joinToString(",") { "?" }
    return readableDatabase.rawQuery(
        """
        SELECT DISTINCT category FROM items
        WHERE playlist_id=? AND kind IN ($kindPlaceholders) AND category IS NOT NULL AND category != ''
        ORDER BY category COLLATE NOCASE ASC
        """.trimIndent(),
        (listOf(playlistId) + tab.kinds.map { it.name }).toTypedArray(),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }
}
