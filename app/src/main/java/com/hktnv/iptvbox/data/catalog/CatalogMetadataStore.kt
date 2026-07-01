package com.hktnv.iptvbox.data.catalog

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentMetadata

internal fun CatalogStore.loadMetadata(item: CatalogItem): ContentMetadata? {
    val key = item.metadataCacheKey() ?: return null
    return readableDatabase.rawQuery(
        "SELECT * FROM metadata_cache WHERE cache_key=?",
        arrayOf(key.cacheKey),
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        ContentMetadata(
            cacheKey = cursor.getString(cursor.column("cache_key")),
            tmdbId = cursor.nullableInt("tmdb_id"),
            normalizedTitle = cursor.getString(cursor.column("normalized_title")),
            plot = cursor.nullableString("plot"),
            cast = cursor.nullableString("cast"),
            director = cursor.nullableString("director"),
            youtubeTrailer = cursor.nullableString("youtube_trailer"),
            duration = cursor.nullableString("duration"),
            backdropUrl = cursor.nullableString("backdrop_url"),
        )
    }
}

internal fun CatalogStore.saveMetadata(item: CatalogItem, metadata: ContentMetadata) {
    val key = item.metadataCacheKey() ?: return
    saveMetadataByKey(
        cacheKey = key.cacheKey,
        tmdbId = key.tmdbId,
        normalizedTitle = key.normalizedTitle,
        metadata = metadata,
    )
}

internal fun CatalogStore.saveEpisodePlot(
    seriesTitle: String,
    episode: SeriesEpisodeRemoteDetails,
) {
    val normalizedSeries = SearchNormalizer.normalize(seriesTitle)
    if (normalizedSeries.isBlank() || episode.plot.isNullOrBlank()) return
    saveMetadataByKey(
        cacheKey = "title:$normalizedSeries:s${episode.seasonNumber}:e${episode.episodeNumber}",
        tmdbId = null,
        normalizedTitle = normalizedSeries,
        metadata = ContentMetadata(
            cacheKey = "",
            normalizedTitle = normalizedSeries,
            plot = episode.plot,
            backdropUrl = episode.imageUrl,
        ),
    )
}

private fun CatalogStore.saveMetadataByKey(
    cacheKey: String,
    tmdbId: Int?,
    normalizedTitle: String,
    metadata: ContentMetadata,
) {
    writableDatabase.insertWithOnConflict(
        "metadata_cache",
        null,
        ContentValues().apply {
            put("cache_key", cacheKey)
            put("tmdb_id", tmdbId)
            put("normalized_title", normalizedTitle)
            put("plot", metadata.plot)
            put("cast", metadata.cast)
            put("director", metadata.director)
            put("youtube_trailer", metadata.youtubeTrailer)
            put("duration", metadata.duration)
            put("backdrop_url", metadata.backdropUrl)
            put("updated_at", System.currentTimeMillis())
        },
        SQLiteDatabase.CONFLICT_REPLACE,
    )
}

internal fun CatalogStore.pruneOrphanMetadata(limit: Int = 50): Int {
    return writableDatabase.pruneOrphanMetadataRows(limit.coerceAtLeast(1))
}

internal data class MetadataCacheKey(
    val cacheKey: String,
    val tmdbId: Int?,
    val normalizedTitle: String,
)

internal fun CatalogItem.metadataCacheKey(): MetadataCacheKey? {
    val normalizedTitle = SearchNormalizer.normalize(title)
    if (tmdbId != null) {
        return MetadataCacheKey(
            cacheKey = "tmdb:$tmdbId",
            tmdbId = tmdbId,
            normalizedTitle = normalizedTitle,
        )
    }
    if (normalizedTitle.isBlank()) return null
    return MetadataCacheKey(
        cacheKey = "title:$normalizedTitle",
        tmdbId = null,
        normalizedTitle = normalizedTitle,
    )
}

internal fun SQLiteDatabase.pruneOrphanMetadataRows(limit: Int): Int {
    return delete(
        "metadata_cache",
        """
        cache_key IN (
            SELECT cache_key FROM metadata_cache
            WHERE cache_key NOT LIKE 'title:%:s%:e%'
            AND NOT EXISTS (
                SELECT 1 FROM items
                WHERE (
                    metadata_cache.tmdb_id IS NOT NULL
                    AND items.tmdb_id = metadata_cache.tmdb_id
                ) OR (
                    metadata_cache.tmdb_id IS NULL
                    AND items.normalized_title = metadata_cache.normalized_title
                )
            )
            LIMIT ?
        )
        """.trimIndent(),
        arrayOf(limit.toString()),
    )
}
