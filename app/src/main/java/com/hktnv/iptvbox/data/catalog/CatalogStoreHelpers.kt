package com.hktnv.iptvbox.data.catalog
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistStats

internal inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

internal fun SQLiteDatabase.createCatalogIndexes() {
    execSQL("CREATE INDEX IF NOT EXISTS idx_categories_lookup ON categories(playlist_id, kind, name)")
    execSQL("CREATE INDEX IF NOT EXISTS idx_items_tab ON items(playlist_id, kind, category_id, provider_order)")
    execSQL("CREATE INDEX IF NOT EXISTS idx_items_series ON items(playlist_id, series_title, season_number, episode_number)")
    execSQL("CREATE INDEX IF NOT EXISTS idx_items_search ON items(playlist_id, search_text)")
    execSQL("CREATE INDEX IF NOT EXISTS idx_items_normalized_title ON items(normalized_title)")
}

internal fun SQLiteDatabase.dropCatalogIndexes() {
    execSQL("DROP INDEX IF EXISTS idx_categories_lookup")
    execSQL("DROP INDEX IF EXISTS idx_items_tab")
    execSQL("DROP INDEX IF EXISTS idx_items_series")
    execSQL("DROP INDEX IF EXISTS idx_items_search")
    execSQL("DROP INDEX IF EXISTS idx_items_normalized_title")
}

internal inline fun <T> measureDb(
    key: String,
    timings: MutableMap<String, Long>,
    block: () -> T,
): T {
    val startedNs = System.nanoTime()
    return try {
        block()
    } finally {
        timings[key] = elapsedMs(startedNs)
    }
}

internal fun elapsedMs(startedNs: Long): Long = (System.nanoTime() - startedNs) / 1_000_000L

internal inline fun SQLiteStatement.use(block: (SQLiteStatement) -> Unit) {
    try {
        block(this)
    } finally {
        close()
    }
}

internal fun SQLiteStatement.bindNullableString(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindString(index, value)
}

internal fun SQLiteStatement.bindNullableLong(index: Int, value: Long?) {
    if (value == null) bindNull(index) else bindLong(index, value)
}

internal fun LoadedPlaylist.withCachedStoreStats(): LoadedPlaylist {
    val stats = statsWithoutCacheForStore()
    return copy(
        cachedItemCount = items.size,
        cachedLiveCount = stats.live,
        cachedMovieCount = stats.movies,
        cachedSeriesCount = stats.series,
    )
}

internal fun LoadedPlaylist.statsWithoutCacheForStore(): PlaylistStats {
    val seriesTitles = HashSet<String>()
    var live = 0
    var movies = 0
    items.forEach { item ->
        when {
            item.kind in CatalogTab.LIVE.kinds -> live += 1
            item.kind == ContentKind.MOVIE -> movies += 1
            item.kind == ContentKind.EPISODE || item.seriesTitle != null -> {
                seriesTitles += item.seriesTitle?.takeIf { it.isNotBlank() } ?: item.title
            }
        }
    }
    return PlaylistStats(live = live, movies = movies, series = seriesTitles.size)
}

internal fun CatalogItem.searchTextForStore(): String {
    return SearchNormalizer.normalizeParts(
        title,
        category,
        tvgName,
        tvgId,
        seriesTitle,
        episodeTitle,
    )
}

internal fun CatalogItem.normalizedTitleForStore(): String = SearchNormalizer.normalize(title)

internal fun CatalogItem.addedSecondsForStore(): Long? {
    return addedAtEpochMillis
        .takeIf { it > 0L }
        ?.div(1_000L)
}
