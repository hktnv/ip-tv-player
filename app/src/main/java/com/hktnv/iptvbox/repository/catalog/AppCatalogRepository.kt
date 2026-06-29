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
import com.hktnv.iptvbox.model.PlaylistStats
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
            if (tab == CatalogTab.SERIES) {
                store.seriesCategoryCounts(playlist.id)
            } else {
                store.categoryCounts(playlist.id, tab)
            }
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
        val livePreview = if (selectedTab == CatalogTab.LIVE && selectedCategory == null) {
            currentItems.take(previewLimit)
        } else {
            store.loadItems(playlist.id, CatalogTab.LIVE, null, previewLimit)
        }
        val moviePreview = if (selectedTab == CatalogTab.MOVIES && selectedCategory == null) {
            currentItems.take(previewLimit)
        } else {
            store.loadItems(playlist.id, CatalogTab.MOVIES, null, previewLimit)
        }
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
            (favoriteIds.asSequence() + recentIds.asSequence()).distinct().take(120).toList(),
        )

        val byId = LinkedHashMap<String, CatalogItem>()
        (livePreview + moviePreview + currentItems + episodes + savedItems).forEach { byId[it.id] = it }
        val tabItems = mapOf(
            CatalogTab.LIVE to if (selectedTab == CatalogTab.LIVE && selectedCategory == null) currentItems else livePreview,
            CatalogTab.MOVIES to if (selectedTab == CatalogTab.MOVIES && selectedCategory == null) currentItems else moviePreview,
            CatalogTab.SERIES to emptyList(),
        )
        val itemsByCategory = CatalogTab.entries.associateWith { tab ->
            if (tab == selectedTab && selectedCategory != null && currentItems.isNotEmpty()) {
                mapOf(selectedCategory to currentItems)
            } else {
                emptyMap()
            }
        }
        val seasonsBySeries = selectedSeriesTitle?.let { mapOf(it to seasons) }.orEmpty()
        val episodesBySeries = selectedSeriesTitle?.let { mapOf(it to episodes) }.orEmpty()

        return CatalogSnapshot(
            playlistId = playlist.id,
            itemsById = byId,
            stats = playlist.cachedStats(),
            tabItems = tabItems,
            categoriesByTab = categories,
            categoryCountsByTab = categoryCounts,
            itemsByCategory = itemsByCategory,
            seriesGroupsAll = seriesGroups,
            episodesBySeries = episodesBySeries,
            seasonsBySeries = seasonsBySeries,
            searchEntries = emptyList(),
        )
    }

    fun buildSnapshot(playlist: LoadedPlaylist): CatalogSnapshot {
        val source = if (playlist.items.isEmpty()) {
            catalogStore?.loadPlaylistWithItems(playlist.id) ?: playlist
        } else {
            playlist
        }
        val byId = HashMap<String, CatalogItem>(source.items.size)
        val searchEntries = ArrayList<SearchEntry>(source.items.size)
        val tabItems = CatalogTab.entries.associateWith { mutableListOf<CatalogItem>() }
        val categories = CatalogTab.entries.associateWith { linkedSetOf<String>() }
        val byCategory = CatalogTab.entries.associateWith { linkedMapOf<String, MutableList<CatalogItem>>() }
        val seriesBuckets = linkedMapOf<String, MutableList<CatalogItem>>()

        var liveCount = 0
        var movieCount = 0

        source.items.forEach { item ->
            byId[item.id] = item
            searchEntries += SearchEntry(item, item.searchTextForIndex())
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
            }
            if (tab != null) {
                tabItems.getValue(tab) += item
                val category = item.category?.ifBlank { null } ?: "Genel"
                categories.getValue(tab) += category
                byCategory.getValue(tab).getOrPut(category) { mutableListOf() } += item
            }
            if (item.kind == ContentKind.EPISODE || item.seriesTitle != null) {
                seriesBuckets.getOrPut(item.seriesDisplayTitleForIndex()) { mutableListOf() } += item
            }
        }

        val seriesGroups = seriesBuckets.map { (title, episodes) ->
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

        val seriesCategoryCounts = seriesGroups
            .mapNotNull { group -> group.category?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
        val itemCategoryCounts = byCategory.mapValues { (_, map) -> map.mapValues { it.value.size } }
        val categoryCounts = itemCategoryCounts + mapOf(CatalogTab.SERIES to seriesCategoryCounts)
        val episodesBySeries = seriesBuckets.mapValues { (_, episodes) ->
            episodes.sortedWith(compareBy<CatalogItem> { it.seasonNumber ?: 1 }.thenBy { it.episodeNumber ?: it.providerOrder })
        }
        val seasonsBySeries = episodesBySeries.mapValues { (title, episodes) ->
            episodes
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

        return CatalogSnapshot(
            playlistId = source.id,
            itemsById = byId,
            stats = PlaylistStats(live = liveCount, movies = movieCount, series = seriesGroups.size),
            tabItems = tabItems.mapValues { it.value.toList() },
            categoriesByTab = categories.mapValues { it.value.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { value -> value }) },
            categoryCountsByTab = categoryCounts,
            itemsByCategory = byCategory.mapValues { (_, map) -> map.mapValues { it.value.toList() } },
            seriesGroupsAll = seriesGroups,
            episodesBySeries = episodesBySeries,
            seasonsBySeries = seasonsBySeries,
            searchEntries = searchEntries,
        )
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
}

private fun LoadedPlaylist.cachedStats(): PlaylistStats {
    if (cachedLiveCount != null && cachedMovieCount != null && cachedSeriesCount != null) {
        return PlaylistStats(cachedLiveCount, cachedMovieCount, cachedSeriesCount)
    }
    return PlaylistStats(
        live = items.count { it.kind in CatalogTab.LIVE.kinds },
        movies = items.count { it.kind == ContentKind.MOVIE },
        series = items.asSequence()
            .filter { it.kind in CatalogTab.SERIES.kinds || it.seriesTitle != null }
            .map { it.seriesTitle?.takeIf(String::isNotBlank) ?: it.title }
            .distinct()
            .count(),
    )
}

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

private fun CatalogItem.searchTextForIndex(): String {
    return SearchNormalizer.normalize(
        listOfNotNull(
            title,
            category,
            tvgName,
            tvgId,
            seriesTitle,
            episodeTitle,
        ).joinToString(" "),
    )
}

private fun CatalogItem.seriesDisplayTitleForIndex(): String {
    val explicit = seriesTitle?.cleanIndexTitle().orEmpty()
    if (explicit.isNotBlank()) return explicit
    val marker = Regex("""(?i)\bS\d{1,2}\s*E\d{1,3}\b""").find(title)
    val inferred = if (marker == null) title else title.substring(0, marker.range.first)
    return inferred.cleanIndexTitle().ifBlank { title.cleanIndexTitle() }
}

private fun String.cleanIndexTitle(): String {
    return replace(Regex("""#EXTINF[^,]*,?""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("[\\w-]+=\"[^\"]*\"", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""https?://\S+""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\b(output|type|username|password|token)=\S+""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""^[A-Z]{2,3}\s*[:|\-]\s*"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim(' ', ',', '-', '|', '»', '•')
}
