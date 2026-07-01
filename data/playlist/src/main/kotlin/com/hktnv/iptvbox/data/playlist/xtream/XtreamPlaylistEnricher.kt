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
            val movies = client.fetchVodStreams(credentials).byNormalizedTitle()
            val series = client.fetchSeries(credentials).byNormalizedTitle()
            XtreamEnrichmentResult(
                items = items.map { item -> item.enrichedBy(movies, series) },
                supported = true,
            )
        }.getOrDefault(XtreamEnrichmentResult(items))
    }

    private fun CatalogItem.enrichedBy(
        movies: Map<String, XtreamBulkEntry>,
        series: Map<String, XtreamBulkEntry>,
    ): CatalogItem {
        val entry = when {
            kind == ContentKind.MOVIE -> movies[SearchNormalizer.normalize(title)]
            kind == ContentKind.EPISODE || !seriesTitle.isNullOrBlank() -> {
                series[SearchNormalizer.normalize(seriesTitle ?: title)]
            }
            else -> null
        } ?: return this
        return copy(
            logoUrl = entry.posterUrl ?: logoUrl,
            xtreamId = entry.xtreamId,
            rating = entry.rating,
            tmdbId = entry.tmdbId,
        )
    }

    private fun List<XtreamBulkEntry>.byNormalizedTitle(): Map<String, XtreamBulkEntry> {
        return buildMap(size) {
            this@byNormalizedTitle.forEach { entry ->
                val key = SearchNormalizer.normalize(entry.title)
                if (key.isNotBlank()) putIfAbsent(key, entry)
            }
        }
    }
}

data class XtreamEnrichmentResult(
    val items: List<CatalogItem>,
    val supported: Boolean = false,
)
