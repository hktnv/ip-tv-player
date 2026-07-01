package com.hktnv.iptvbox.data.catalog

import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.CatalogTab

internal data class CatalogCategoryRow(
    val id: String,
    val name: String,
    val kind: String,
    val xtreamCategoryId: String?,
)

internal data class CategoryXtreamMapping(
    val kind: String,
    val name: String,
    val xtreamCategoryId: String,
)

internal data class QueuedXtreamCategory(
    val name: String,
    val kind: String,
    val xtreamCategoryId: String,
)

internal fun CatalogItem.categoryNameForStore(): String = category?.takeIf { it.isNotBlank() } ?: "Genel"

internal fun CatalogItem.categoryKindForStore(): String = kind.categoryKindForStore()

internal fun ContentKind.categoryKindForStore(): String {
    return when (this) {
        ContentKind.LIVE_CHANNEL,
        ContentKind.RADIO -> CategoryKindLive
        ContentKind.MOVIE -> CategoryKindMovie
        ContentKind.SERIES,
        ContentKind.SEASON,
        ContentKind.EPISODE -> CategoryKindSeries
    }
}

internal fun CatalogTab.categoryKindForStore(): String {
    return when (this) {
        CatalogTab.LIVE -> CategoryKindLive
        CatalogTab.MOVIES -> CategoryKindMovie
        CatalogTab.SERIES -> CategoryKindSeries
    }
}

internal fun catalogCategoryId(playlistId: String, kind: String, name: String): String {
    return "$playlistId|$kind|$name"
}

internal fun categoryMappingKey(kind: String, name: String): String {
    return "$kind|${SearchNormalizer.normalize(name)}"
}

internal const val CategoryKindLive = "LIVE"
internal const val CategoryKindMovie = "MOVIE"
internal const val CategoryKindSeries = "SERIES"
