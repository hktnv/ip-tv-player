package com.hktnv.iptvbox.repository.catalog

import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.data.catalog.CatalogStore
import com.hktnv.iptvbox.data.catalog.categories
import com.hktnv.iptvbox.data.catalog.categoryCounts
import com.hktnv.iptvbox.data.catalog.episodes
import com.hktnv.iptvbox.data.catalog.seasons
import com.hktnv.iptvbox.data.catalog.seriesCategoryCounts
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.SeasonGroup
import com.hktnv.iptvbox.model.SeriesGroup

internal class AppCatalogRepository(
    private val catalogStore: CatalogStore? = null,
) {
    fun buildUiSnapshot(
        playlist: LoadedPlaylist,
        selectedTab: CatalogTab,
        selectedCategory: String?,
        selectedSeriesTitle: String?,
        selectedSeasonNumber: Int?,
        favoriteIds: List<String>,
        recentIds: List<String>,
        previewLimit: Int,
        visibleLimit: Int,
    ): CatalogSnapshot {
        val store = catalogStore ?: return buildSnapshot(playlist)
        if (playlist.items.isNotEmpty()) return buildSnapshot(playlist)

        val categoryCounts = CatalogTab.entries.associateWith { tab ->
            if (tab == CatalogTab.SERIES) store.seriesCategoryCounts(playlist.id) else store.categoryCounts(playlist.id, tab)
        }
        val categories = CatalogTab.entries.associateWith { tab ->
            if (tab == CatalogTab.SERIES) {
                categoryCounts.getValue(tab).keys.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
            } else {
                store.categories(playlist.id, tab)
            }
        }
        val currentItems = if (selectedTab == CatalogTab.SERIES) {
            emptyList()
        } else {
            store.loadItems(playlist.id, selectedTab, selectedCategory, visibleLimit)
        }
        val livePreview = loadPreview(store, playlist, currentItems, selectedTab, selectedCategory, CatalogTab.LIVE, previewLimit)
        val moviePreview = loadPreview(store, playlist, currentItems, selectedTab, selectedCategory, CatalogTab.MOVIES, previewLimit)
        val seriesGroups = store.seriesGroups(
            playlistId = playlist.id,
            category = selectedCategory.takeIf { selectedTab == CatalogTab.SERIES },
            limit = visibleLimit,
        )
        val seasons = selectedSeriesTitle?.let { store.seasons(playlist.id, it) }.orEmpty()
        val episodes = selectedSeriesTitle?.let {
            store.episodes(playlist.id, it, selectedSeasonNumber, visibleLimit)
        }.orEmpty()
        val savedItems = store.itemsByIds(
            playlist.id,
            (favoriteIds.asSequence() + recentIds.asSequence()).distinct().take(SAVED_LOOKUP_LIMIT).toList(),
        )

        val byId = linkedMapOf<String, CatalogItem>()
        (livePreview + moviePreview + currentItems + episodes + savedItems).forEach { byId[it.id] = it }
        return CatalogSnapshot(
            playlistId = playlist.id,
            itemsById = byId,
            stats = playlist.cachedStats(),
            tabItems = mapOf(
                CatalogTab.LIVE to if (selectedTab == CatalogTab.LIVE && selectedCategory == null) currentItems else livePreview,
                CatalogTab.MOVIES to if (selectedTab == CatalogTab.MOVIES && selectedCategory == null) currentItems else moviePreview,
                CatalogTab.SERIES to emptyList(),
            ),
            categoriesByTab = categories,
            categoryCountsByTab = categoryCounts,
            itemsByCategory = buildSelectedCategoryMap(selectedTab, selectedCategory, currentItems),
            seriesGroupsAll = seriesGroups,
            episodesBySeries = selectedSeriesTitle?.let { mapOf(it to episodes) }.orEmpty(),
            seasonsBySeries = selectedSeriesTitle?.let { mapOf(it to seasons) }.orEmpty(),
            searchEntries = emptyList(),
        )
    }

    fun buildSnapshot(playlist: LoadedPlaylist): CatalogSnapshot {
        val source = if (playlist.items.isEmpty()) {
            catalogStore?.loadPlaylistWithItems(playlist.id) ?: playlist
        } else {
            playlist
        }
        val index = MutableCatalogIndex(source.items.size)
        source.items.forEach(index::add)
        return index.toSnapshot(source.id)
    }

    fun search(snapshot: CatalogSnapshot, query: String, limit: Int): List<CatalogItem> {
        catalogStore?.let { store ->
            return store.search(snapshot.playlistId, query, limit)
        }
        val normalizedQuery = SearchNormalizer.normalize(query)
        if (normalizedQuery.isBlank() || limit <= 0) return emptyList()
        return snapshot.searchEntries.asSequence()
            .filter { normalizedQuery in it.normalizedText }
            .map { it.item }
            .take(limit)
            .toList()
    }

    private fun loadPreview(
        store: CatalogStore,
        playlist: LoadedPlaylist,
        currentItems: List<CatalogItem>,
        selectedTab: CatalogTab,
        selectedCategory: String?,
        previewTab: CatalogTab,
        previewLimit: Int,
    ): List<CatalogItem> {
        return if (selectedTab == previewTab && selectedCategory == null) {
            currentItems.take(previewLimit)
        } else {
            store.loadItems(playlist.id, previewTab, null, previewLimit)
        }
    }

    private fun buildSelectedCategoryMap(
        selectedTab: CatalogTab,
        selectedCategory: String?,
        currentItems: List<CatalogItem>,
    ): Map<CatalogTab, Map<String, List<CatalogItem>>> {
        return CatalogTab.entries.associateWith { tab ->
            if (tab == selectedTab && selectedCategory != null && currentItems.isNotEmpty()) {
                mapOf(selectedCategory to currentItems)
            } else {
                emptyMap()
            }
        }
    }

    private class MutableCatalogIndex(initialSize: Int) {
        private val byId = HashMap<String, CatalogItem>(initialSize)
        private val searchEntries = ArrayList<SearchEntry>(initialSize)
        private val tabItems = CatalogTab.entries.associateWith { mutableListOf<CatalogItem>() }
        private val categories = CatalogTab.entries.associateWith { linkedSetOf<String>() }
        private val byCategory = CatalogTab.entries.associateWith { linkedMapOf<String, MutableList<CatalogItem>>() }
        private val seriesBuckets = linkedMapOf<String, MutableList<CatalogItem>>()
        private var liveCount = 0
        private var movieCount = 0

        fun add(item: CatalogItem) {
            byId[item.id] = item
            searchEntries += SearchEntry(item, item.searchTextForIndex())
            addToTab(item)
            if (item.kind == ContentKind.EPISODE || item.seriesTitle != null) {
                seriesBuckets.getOrPut(item.seriesDisplayTitleForIndex()) { mutableListOf() } += item
            }
        }

        fun toSnapshot(playlistId: String): CatalogSnapshot {
            val seriesGroups = buildSeriesGroups(seriesBuckets)
            val episodesBySeries = seriesBuckets.mapValues { (_, episodes) ->
                episodes.sortedWith(compareBy<CatalogItem> { it.seasonNumber ?: 1 }.thenBy { it.episodeNumber ?: it.providerOrder })
            }
            return CatalogSnapshot(
                playlistId = playlistId,
                itemsById = byId,
                stats = com.hktnv.iptvbox.model.PlaylistStats(live = liveCount, movies = movieCount, series = seriesGroups.size),
                tabItems = tabItems.mapValues { it.value.toList() },
                categoriesByTab = categories.mapValues { it.value.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { value -> value }) },
                categoryCountsByTab = categoryCounts(seriesGroups),
                itemsByCategory = byCategory.mapValues { (_, map) -> map.mapValues { it.value.toList() } },
                seriesGroupsAll = seriesGroups,
                episodesBySeries = episodesBySeries,
                seasonsBySeries = episodesBySeries.mapValues(::buildSeasons),
                searchEntries = searchEntries,
            )
        }

        private fun addToTab(item: CatalogItem) {
            val tab = when {
                item.kind in CatalogTab.LIVE.kinds -> {
                    liveCount += 1
                    CatalogTab.LIVE
                }
                item.kind == ContentKind.MOVIE -> {
                    movieCount += 1
                    CatalogTab.MOVIES
                }
                item.kind in CatalogTab.SERIES.kinds -> CatalogTab.SERIES
                else -> null
            } ?: return
            val category = item.category?.ifBlank { null } ?: "Genel"
            tabItems.getValue(tab) += item
            categories.getValue(tab) += category
            byCategory.getValue(tab).getOrPut(category) { mutableListOf() } += item
        }

        private fun categoryCounts(seriesGroups: List<SeriesGroup>): Map<CatalogTab, Map<String, Int>> {
            val itemCounts = byCategory.mapValues { (_, map) -> map.mapValues { it.value.size } }
            val seriesCounts = seriesGroups
                .mapNotNull { it.category?.takeIf(String::isNotBlank) }
                .groupingBy { it }
                .eachCount()
            return itemCounts + mapOf(CatalogTab.SERIES to seriesCounts)
        }
    }

    private companion object {
        const val SAVED_LOOKUP_LIMIT = 120
    }
}

private fun buildSeriesGroups(seriesBuckets: Map<String, List<CatalogItem>>): List<SeriesGroup> {
    return seriesBuckets.map { (title, episodes) ->
        SeriesGroup(
            id = SearchNormalizer.normalize(title).ifBlank { title },
            title = title,
            category = episodes.firstNotNullOfOrNull { it.category },
            logoUrl = episodes.firstNotNullOfOrNull { it.logoUrl },
            seasonCount = episodes.asSequence().map { it.seasonNumber ?: 1 }.distinct().count(),
            episodeCount = episodes.size,
            firstOrder = episodes.minOfOrNull { it.providerOrder } ?: 0,
        )
    }.sortedWith(compareBy<SeriesGroup> { it.firstOrder }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
}

private fun buildSeasons(entry: Map.Entry<String, List<CatalogItem>>): List<SeasonGroup> {
    val (title, episodes) = entry
    return episodes
        .groupBy { it.seasonNumber ?: 1 }
        .map { (season, seasonEpisodes) ->
            SeasonGroup(
                id = "$title-$season",
                title = "Sezon $season",
                seasonNumber = season,
                episodeCount = seasonEpisodes.size,
                logoUrl = seasonEpisodes.firstNotNullOfOrNull { it.logoUrl },
                firstOrder = seasonEpisodes.minOfOrNull { it.providerOrder } ?: 0,
            )
        }
        .sortedWith(compareBy<SeasonGroup> { it.seasonNumber }.thenBy { it.firstOrder })
}
