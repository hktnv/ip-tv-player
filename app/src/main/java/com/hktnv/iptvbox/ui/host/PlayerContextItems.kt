package com.hktnv.iptvbox.ui.host

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot

internal fun contextWindowForPlayer(items: List<CatalogItem>, item: CatalogItem): List<CatalogItem> {
    val uniqueItems = LinkedHashMap<String, CatalogItem>()
    items.filter { it.streamUrl.isNotBlank() }.forEach { uniqueItems[it.id] = it }
    if (item.streamUrl.isNotBlank() && item.id !in uniqueItems) uniqueItems[item.id] = item

    val contextItems = uniqueItems.values.toList()
    if (contextItems.size <= MaxPlayerContextItems) return contextItems

    val index = contextItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
    val start = (index - PlayerContextItemsBeforeCurrent).coerceAtLeast(0)
    val end = (start + MaxPlayerContextItems).coerceAtMost(contextItems.size)
    val adjustedStart = (end - MaxPlayerContextItems).coerceAtLeast(0)
    return contextItems.subList(adjustedStart, end)
}

internal fun latestItemsForPlayer(snapshot: CatalogSnapshot?): List<CatalogItem> {
    return snapshot?.allItems.orEmpty()
        .filter { it.streamUrl.isNotBlank() }
        .sortedByDescending { it.addedAtEpochMillis }
}

internal fun CatalogSnapshot.playbackContextItemsFor(item: CatalogItem): List<CatalogItem> {
    val tab = item.tabForPlayerContext()
    return when (item.kind) {
        ContentKind.EPISODE -> {
            val seriesTitle = item.seriesTitle?.takeIf { it.isNotBlank() }
            if (seriesTitle != null) {
                episodes(seriesTitle, item.seasonNumber)
            } else {
                visibleItems(tab, item.category.takeIf { !it.isNullOrBlank() })
            }
        }
        else -> visibleItems(tab, item.category.takeIf { !it.isNullOrBlank() })
    }.ifEmpty { items(tab) }
}

internal fun CatalogItem.tabForPlayerContext(): CatalogTab {
    return when (kind) {
        ContentKind.LIVE_CHANNEL,
        ContentKind.RADIO -> CatalogTab.LIVE
        ContentKind.MOVIE -> CatalogTab.MOVIES
        ContentKind.SERIES,
        ContentKind.SEASON,
        ContentKind.EPISODE -> CatalogTab.SERIES
    }
}

private const val MaxPlayerContextItems = 240
private const val PlayerContextItemsBeforeCurrent = 90
