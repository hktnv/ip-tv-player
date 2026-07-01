package com.hktnv.iptvbox.data.catalog
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.SeasonGroup

internal fun CatalogStore.seasons(playlistId: String, seriesTitle: String): List<SeasonGroup> {
    return readableDatabase.rawQuery(
        """
        SELECT
            COALESCE(items.season_number, 1) AS season_number,
            COUNT(*) AS episode_count,
            MIN(items.logo_url) AS logo_url,
            MIN(items.provider_order) AS first_order
        FROM items
        WHERE items.playlist_id=? AND items.series_title=?
        GROUP BY COALESCE(items.season_number, 1)
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
    val seasonClause = seasonFilterClause(seasonNumber)
    val args = mutableListOf(playlistId, seriesTitle).apply {
        if (seasonNumber != null) add(seasonNumber.toString())
        add(limit.toString())
    }
    return readableDatabase.rawQuery(
        itemSelect(
            """
            WHERE items.playlist_id=? AND items.series_title=?
                $seasonClause
            ORDER BY COALESCE(items.season_number, 1) ASC,
                COALESCE(items.episode_number, items.provider_order) ASC,
                items.provider_order ASC
            LIMIT ?
            """.trimIndent(),
        ),
        args.toTypedArray(),
    ).use { cursor -> cursor.toItems() }
}

internal fun seasonFilterClause(seasonNumber: Int?): String {
    return if (seasonNumber == null) {
        ""
    } else {
        " AND COALESCE(items.season_number, 1)=CAST(? AS INTEGER)"
    }
}

internal fun CatalogStore.categories(playlistId: String, tab: CatalogTab): List<String> {
    return readableDatabase.rawQuery(
        """
        SELECT categories.name FROM categories
        WHERE categories.playlist_id=? AND categories.kind=?
        ORDER BY categories.name COLLATE NOCASE ASC
        """.trimIndent(),
        arrayOf(playlistId, tab.categoryKindForStore()),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }
}
