package com.hktnv.iptvbox.ui.search

import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.ui.media.cleanUiTitle
import com.hktnv.iptvbox.ui.media.displayTitle
import com.hktnv.iptvbox.ui.media.metaLine
import com.hktnv.iptvbox.ui.media.seriesDisplayTitle

internal fun List<CatalogItem>.collapseSeriesSearchResults(): List<CatalogItem> {
    val seenSeries = HashSet<String>()
    val collapsed = ArrayList<CatalogItem>(size)
    for (item in this) {
        val seriesTitle = item.searchSeriesTitleOrNull()
        if (seriesTitle == null) {
            collapsed += item
            continue
        }
        val key = SearchNormalizer.normalize(seriesTitle)
        if (seenSeries.add(key)) collapsed += item
    }
    return collapsed
}

internal fun CatalogItem.searchResultTitle(): String {
    return searchSeriesTitleOrNull() ?: displayTitle()
}

internal fun CatalogItem.searchResultKind(): ContentKind {
    return if (searchSeriesTitleOrNull() == null) kind else ContentKind.SERIES
}

internal fun CatalogItem.searchResultMetaLine(): String {
    if (searchSeriesTitleOrNull() == null) return metaLine()
    val category = category?.cleanUiTitle().orEmpty()
    return listOf("Dizi", category).filter { it.isNotBlank() }.joinToString(" · ")
}

internal fun CatalogItem.searchSeriesTitleOrNull(): String? {
    if (kind != ContentKind.EPISODE && seriesTitle.isNullOrBlank()) return null
    return seriesDisplayTitle().takeIf { it.isNotBlank() }
}
