package com.hktnv.iptvbox.data.catalog

import android.content.ContentValues
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.model.CatalogTab

internal data class CatalogRemoteEntry(
    val xtreamId: Int,
    val title: String,
    val posterUrl: String?,
    val rating: String?,
    val tmdbId: Int?,
    val addedAtEpochSeconds: Long?,
)

internal data class SeriesEpisodeRemoteDetails(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String?,
    val plot: String?,
    val imageUrl: String?,
)

internal fun CatalogStore.queuedXtreamCategories(playlistId: String): List<QueuedXtreamCategory> {
    return readableDatabase.rawQuery(
        queuedXtreamCategoriesSql(),
        arrayOf(playlistId, CategoryKindMovie, CategoryKindSeries),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    QueuedXtreamCategory(
                        name = cursor.getString(cursor.column("name")),
                        kind = cursor.getString(cursor.column("kind")),
                        xtreamCategoryId = cursor.getString(cursor.column("xtream_category_id")),
                    ),
                )
            }
        }
    }
}

internal fun queuedXtreamCategoriesSql(): String {
    return """
        SELECT name, kind, xtream_category_id FROM categories
        WHERE playlist_id=?
            AND kind IN (?, ?)
            AND xtream_category_id IS NOT NULL
            AND xtream_category_id != ''
            AND EXISTS (
                SELECT 1 FROM items
                WHERE items.category_id = categories.id
                    AND items.xtream_id IS NULL
                LIMIT 1
            )
        ORDER BY kind ASC, name COLLATE NOCASE ASC
        """.trimIndent()
}

internal fun CatalogStore.updateItemsFromXtreamCategory(
    playlistId: String,
    tab: CatalogTab,
    category: String,
    entries: List<CatalogRemoteEntry>,
): Int {
    val remoteByTitle = entries.associateBy { SearchNormalizer.normalize(it.title) }
    if (remoteByTitle.isEmpty()) return 0
    val localItems = loadItems(playlistId, tab, category, limit = Int.MAX_VALUE)
    var updated = 0
    writableDatabase.transaction {
        localItems.forEach { item ->
            val key = if (tab == CatalogTab.SERIES) item.seriesTitle ?: item.title else item.title
            val remote = remoteByTitle[SearchNormalizer.normalize(key)] ?: return@forEach
            val values = ContentValues().apply {
                put("xtream_id", remote.xtreamId)
                put("rating", remote.rating)
                if (remote.tmdbId != null) put("tmdb_id", remote.tmdbId)
                if (remote.addedAtEpochSeconds != null) put("added", remote.addedAtEpochSeconds)
                if (!remote.posterUrl.isNullOrBlank()) put("logo_url", remote.posterUrl)
            }
            updated += update(
                "items",
                values,
                "playlist_id=? AND item_id=?",
                arrayOf(playlistId, item.id),
            )
        }
    }
    return updated
}

internal fun CatalogStore.updateSeriesEpisodeDetails(
    playlistId: String,
    seriesTitle: String,
    episodes: List<SeriesEpisodeRemoteDetails>,
): Int {
    var updated = 0
    writableDatabase.transaction {
        episodes.forEach { episode ->
            val values = ContentValues().apply {
                if (!episode.title.isNullOrBlank()) put("episode_title", episode.title)
                if (!episode.imageUrl.isNullOrBlank()) put("logo_url", episode.imageUrl)
            }
            updated += update(
                "items",
                values,
                """
                playlist_id=? AND series_title=?
                    AND COALESCE(season_number, 1)=?
                    AND COALESCE(episode_number, provider_order)=?
                """.trimIndent(),
                arrayOf(
                    playlistId,
                    seriesTitle,
                    episode.seasonNumber.toString(),
                    episode.episodeNumber.toString(),
                ),
            )
            saveEpisodePlot(seriesTitle, episode)
        }
    }
    return updated
}
