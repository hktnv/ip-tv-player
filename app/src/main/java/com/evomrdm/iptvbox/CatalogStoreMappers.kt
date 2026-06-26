package com.evomrdm.iptvbox

import android.content.ContentValues
import android.database.Cursor
import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.core.model.ContentKind
import com.evomrdm.iptvbox.core.model.PlaylistSourceType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val catalogStoreJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun LoadedPlaylist.toPlaylistValues(): ContentValues = ContentValues().apply {
    val stats = statsWithoutCacheForStore()
    put("id", id)
    put("name", name)
    put("type", type.name)
    put("endpoint", endpoint)
    put("headers_json", catalogStoreJson.encodeToString(headers))
    put("epg_json", catalogStoreJson.encodeToString(epgUrls))
    put("warnings_json", catalogStoreJson.encodeToString(warnings))
    put("item_count", items.size.takeIf { it > 0 } ?: cachedItemCount ?: 0)
    put("live_count", stats.live)
    put("movie_count", stats.movies)
    put("series_count", stats.series)
    put("updated_at", System.currentTimeMillis())
}

internal fun Cursor.toPlaylist(): LoadedPlaylist {
    return LoadedPlaylist(
        id = getString(column("id")),
        name = getString(column("name")),
        type = PlaylistSourceType.valueOf(getString(column("type"))),
        endpoint = getString(column("endpoint")),
        headers = catalogStoreJson.decodeFromString(getString(column("headers_json"))),
        items = emptyList(),
        epgUrls = catalogStoreJson.decodeFromString(getString(column("epg_json"))),
        warnings = catalogStoreJson.decodeFromString(getString(column("warnings_json"))),
        cachedItemCount = getInt(column("item_count")),
        cachedLiveCount = getInt(column("live_count")),
        cachedMovieCount = getInt(column("movie_count")),
        cachedSeriesCount = getInt(column("series_count")),
    )
}

internal fun Cursor.toItems(): List<CatalogItem> = buildList {
    while (moveToNext()) {
        add(
            CatalogItem(
                id = getString(column("item_id")),
                sourceId = getString(column("playlist_id")),
                kind = ContentKind.valueOf(getString(column("kind"))),
                title = getString(column("title")),
                streamUrl = getString(column("stream_url")),
                category = nullableString("category"),
                logoUrl = nullableString("logo_url"),
                tvgId = nullableString("tvg_id"),
                tvgName = nullableString("tvg_name"),
                seriesTitle = nullableString("series_title"),
                seasonNumber = nullableInt("season_number"),
                episodeNumber = nullableInt("episode_number"),
                episodeTitle = nullableString("episode_title"),
                providerOrder = getInt(column("provider_order")),
            ),
        )
    }
}

internal fun Cursor.column(name: String): Int = getColumnIndexOrThrow(name)

internal fun Cursor.nullableString(name: String): String? {
    val index = column(name)
    return if (isNull(index)) null else getString(index)
}

internal fun Cursor.nullableInt(name: String): Int? {
    val index = column(name)
    return if (isNull(index)) null else getInt(index)
}
