package com.hktnv.iptvbox.data.playlist.xtream

import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind

class XtreamPlaylistEnricher(
    private val client: XtreamApiClient,
) {
    fun enrich(rawM3uUrl: String, items: List<CatalogItem>): XtreamEnrichmentResult {
        val credentials = XtreamM3uUrlDetector.detect(rawM3uUrl) ?: return XtreamEnrichmentResult(items)
        return runCatching {
            if (!client.authenticate(credentials)) return XtreamEnrichmentResult(items)
            XtreamEnrichmentResult(
                items = items,
                supported = true,
                categoryMappings = categoryMappings(credentials, items),
            )
        }.getOrDefault(XtreamEnrichmentResult(items))
    }

    private fun categoryMappings(
        credentials: XtreamCredentials,
        items: List<CatalogItem>,
    ): List<XtreamCategoryMapping> {
        val localCategories = items.asSequence()
            .mapNotNull { item -> item.localCategoryKey() }
            .distinctBy { "${it.kind}|${it.normalizedName}" }
            .groupBy { it.kind }
        return buildList {
            addMatches(CategoryKindLive, localCategories, client.fetchLiveCategories(credentials))
            addMatches(CategoryKindMovie, localCategories, client.fetchVodCategories(credentials))
            addMatches(CategoryKindSeries, localCategories, client.fetchSeriesCategories(credentials))
        }
    }

    private fun MutableList<XtreamCategoryMapping>.addMatches(
        kind: String,
        localCategories: Map<String, List<LocalCategoryKey>>,
        remoteCategories: List<XtreamCategoryEntry>,
    ) {
        val localByName = localCategories[kind].orEmpty().associateBy { it.normalizedName }
        remoteCategories.forEach { remote ->
            val local = localByName[SearchNormalizer.normalize(remote.title)] ?: return@forEach
            add(XtreamCategoryMapping(kind, local.name, remote.categoryId))
        }
    }

    private fun CatalogItem.localCategoryKey(): LocalCategoryKey? {
        val name = category?.takeIf { it.isNotBlank() } ?: "Genel"
        val kindKey = when (kind) {
            ContentKind.LIVE_CHANNEL,
            ContentKind.RADIO -> CategoryKindLive
            ContentKind.MOVIE -> CategoryKindMovie
            ContentKind.SERIES,
            ContentKind.SEASON,
            ContentKind.EPISODE -> CategoryKindSeries
        }
        val normalized = SearchNormalizer.normalize(name)
        if (normalized.isBlank()) return null
        return LocalCategoryKey(kind = kindKey, name = name, normalizedName = normalized)
    }

    private data class LocalCategoryKey(
        val kind: String,
        val name: String,
        val normalizedName: String,
    )

    private companion object {
        const val CategoryKindLive = "LIVE"
        const val CategoryKindMovie = "MOVIE"
        const val CategoryKindSeries = "SERIES"
    }
}

data class XtreamEnrichmentResult(
    val items: List<CatalogItem>,
    val supported: Boolean = false,
    val categoryMappings: List<XtreamCategoryMapping> = emptyList(),
)
