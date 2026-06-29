package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.CatalogItem

data class PlaylistLoadResult(
    val playlistName: String,
    val items: List<CatalogItem>,
    val epgUrls: List<String>,
    val warnings: List<String>,
    val metrics: PlaylistLoadMetrics = PlaylistLoadMetrics(),
)

data class PlaylistLoadMetrics(
    val totalMs: Long = 0L,
    val urlNormalizeMs: Long = 0L,
    val connectionOpenMs: Long = 0L,
    val downloadMs: Long = 0L,
    val lineReadMs: Long = 0L,
    val parseMs: Long = 0L,
    val parseOtherMs: Long = 0L,
    val contentCleaningMs: Long = 0L,
    val kindSplitMs: Long = 0L,
    val categoryMs: Long = 0L,
    val seriesMs: Long = 0L,
    val classificationMs: Long = 0L,
    val directoryMs: Long = 0L,
)

internal operator fun PlaylistLoadMetrics.plus(other: PlaylistLoadMetrics): PlaylistLoadMetrics {
    return PlaylistLoadMetrics(
        totalMs = totalMs + other.totalMs,
        urlNormalizeMs = urlNormalizeMs + other.urlNormalizeMs,
        connectionOpenMs = connectionOpenMs + other.connectionOpenMs,
        downloadMs = downloadMs + other.downloadMs,
        lineReadMs = lineReadMs + other.lineReadMs,
        parseMs = parseMs + other.parseMs,
        parseOtherMs = parseOtherMs + other.parseOtherMs,
        contentCleaningMs = contentCleaningMs + other.contentCleaningMs,
        kindSplitMs = kindSplitMs + other.kindSplitMs,
        categoryMs = categoryMs + other.categoryMs,
        seriesMs = seriesMs + other.seriesMs,
        classificationMs = classificationMs + other.classificationMs,
        directoryMs = directoryMs + other.directoryMs,
    )
}
