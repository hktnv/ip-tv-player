package com.hktnv.iptvbox.repository.catalog

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.PlaylistStats
import com.hktnv.iptvbox.model.SeasonGroup
import com.hktnv.iptvbox.model.SeriesGroup

internal data class SearchEntry(
    val item: CatalogItem,
    val normalizedText: String,
)

internal data class CatalogSnapshot(
    val playlistId: String,
    val itemsById: Map<String, CatalogItem>,
    val stats: PlaylistStats,
    val tabItems: Map<CatalogTab, List<CatalogItem>>,
    val categoriesByTab: Map<CatalogTab, List<String>>,
    val categoryCountsByTab: Map<CatalogTab, Map<String, Int>>,
    val itemsByCategory: Map<CatalogTab, Map<String, List<CatalogItem>>>,
    val seriesGroupsAll: List<SeriesGroup>,
    val episodesBySeries: Map<String, List<CatalogItem>>,
    val seasonsBySeries: Map<String, List<SeasonGroup>>,
    val searchEntries: List<SearchEntry>,
) {
    val allItems: Collection<CatalogItem> get() = itemsById.values

    fun items(tab: CatalogTab): List<CatalogItem> = tabItems[tab].orEmpty()

    fun categories(tab: CatalogTab): List<String> = categoriesByTab[tab].orEmpty()

    fun categoryCount(tab: CatalogTab, category: String?): Int {
        return if (category == null) stats.count(tab) else categoryCountsByTab[tab]?.get(category).orEmptyCount()
    }

    fun visibleItems(tab: CatalogTab, category: String?): List<CatalogItem> {
        return if (category == null) items(tab) else itemsByCategory[tab]?.get(category).orEmpty()
    }

    fun itemsByIds(ids: List<String>): List<CatalogItem> = ids.mapNotNull(itemsById::get)

    fun seriesGroups(category: String?): List<SeriesGroup> {
        if (category == null) return seriesGroupsAll
        return seriesGroupsAll.filter { it.category == category }
    }

    fun seasons(seriesTitle: String): List<SeasonGroup> = seasonsBySeries[seriesTitle].orEmpty()

    fun episodes(seriesTitle: String, seasonNumber: Int?): List<CatalogItem> {
        val episodes = episodesBySeries[seriesTitle].orEmpty()
        return if (seasonNumber == null) episodes else episodes.filter { (it.seasonNumber ?: 1) == seasonNumber }
    }
}

private fun Int?.orEmptyCount(): Int = this ?: 0
