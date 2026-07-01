package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.KindGuess

class M3uPlaylistParser {
    fun parse(
        sourceId: String,
        lines: Sequence<String>,
        measureStages: Boolean = false,
        onItemParsed: (Int) -> Unit = {},
    ): ParsedM3uPlaylist {
        val parseStartedNs = System.nanoTime()
        val stageMetrics = if (measureStages) ParseStageMetrics() else null
        val epgUrls = linkedSetOf<String>()
        val items = ArrayList<CatalogItem>(8_192)
        var pendingExtInfLine: String? = null
        var order = 0
        val iterator = lines.iterator()

        while (iterator.hasNext()) {
            val line = iterator.next().trim()
            when {
                line.isBlank() -> Unit
                line.startsWith("#EXTM3U", ignoreCase = true) -> collectEpgUrls(line, epgUrls)
                line.startsWith("#EXTINF", ignoreCase = true) -> pendingExtInfLine = line
                line.startsWith("#") -> Unit
                pendingExtInfLine != null -> {
                    val extInfLine = pendingExtInfLine.orEmpty()
                    if (line.isHttpUrl() || line.startsWith("rtsp://", ignoreCase = true)) {
                        order += 1
                        items += parseEntry(sourceId, extInfLine, line, order, stageMetrics)
                        if (items.size == 1 || items.size % PROGRESS_STEP == 0) {
                            onItemParsed(items.size)
                        }
                    }
                    pendingExtInfLine = null
                }
            }
        }
        if (items.isNotEmpty()) onItemParsed(items.size)

        val parseMs = elapsedMs(parseStartedNs)
        val measuredMs = stageMetrics?.measuredMs() ?: ParseStageMeasuredMs()
        return ParsedM3uPlaylist(
            epgUrls = epgUrls.toList(),
            items = items,
            parseMs = parseMs,
            contentCleaningMs = measuredMs.contentCleaningMs,
            kindSplitMs = measuredMs.kindSplitMs,
            categoryMs = measuredMs.categoryMs,
            seriesMs = measuredMs.seriesMs,
            classificationMs = measuredMs.classificationMs,
            parseOtherMs = (parseMs - measuredMs.totalMs).coerceAtLeast(0L),
        )
    }

    fun parse(sourceId: String, text: String, measureStages: Boolean = false): ParsedM3uPlaylist {
        return parse(sourceId, text.lineSequence(), measureStages)
    }

    fun guessKind(info: ExtInf): KindGuess {
        val title = info.title
        return guessKind(info, "", title, normalizeM3uMarkers(title))
    }

    fun guessKind(info: ExtInf, streamUrl: String): KindGuess {
        val title = info.title
        return guessKind(info, streamUrl, title, normalizeM3uMarkers(title))
    }

    private fun parseEntry(
        sourceId: String,
        extInfLine: String,
        streamUrl: String,
        providerOrder: Int,
        stageMetrics: ParseStageMetrics?,
    ): CatalogItem {
        val contentStartedNs = stageMetrics?.now()
        val info = parseExtInf(extInfLine)
        val title = info.title.takeUnless { it == M3uPlaylistPatterns.UNTITLED } ?: m3uUrlFileName(streamUrl)
        val normalizedTitle = normalizeM3uMarkers(title)
        stageMetrics?.addContentCleaning(contentStartedNs)

        val series = stageMetrics.measureSeries { extractSeriesEpisode(title) }
        val guess = if (series != null) {
            KindGuess(ContentKind.EPISODE, 0.95, "series episode pattern")
        } else {
            stageMetrics.measureClassification {
                guessKind(info, streamUrl, title, normalizedTitle, hasSeriesEpisodePattern = false)
            }
        }
        val category = stageMetrics.measureCategory {
            deriveCategory(
                rawGroupTitle = info.groupTitle,
                title = title,
                normalizedTitle = normalizedTitle,
                kind = guess.kind,
                series = series,
            )
        }

        return CatalogItem(
            id = stableM3uItemId(sourceId, streamUrl, title),
            sourceId = sourceId,
            kind = if (series != null) ContentKind.EPISODE else guess.kind,
            title = title,
            streamUrl = streamUrl,
            category = category,
            logoUrl = info.logoUrl?.takeIf { it.isHttpUrl() },
            tvgId = info.tvgId,
            tvgName = info.tvgName,
            seriesTitle = series?.seriesTitle,
            seasonNumber = series?.seasonNumber,
            episodeNumber = series?.episodeNumber,
            episodeTitle = series?.episodeTitle,
            providerOrder = providerOrder,
        )
    }

    private fun guessKind(
        info: ExtInf,
        streamUrl: String,
        title: String,
        normalizedTitle: String,
        hasSeriesEpisodePattern: Boolean = title.hasSeriesEpisodePattern(),
    ): KindGuess = guessM3uKind(
        info = info,
        streamUrl = streamUrl,
        title = title,
        normalizedTitle = normalizedTitle,
        hasSeriesEpisodePattern = hasSeriesEpisodePattern,
    )

    private fun collectEpgUrls(line: String, epgUrls: MutableSet<String>) {
        val attrs = parseAttributes(line)
        listOfNotNull(attrs["url-tvg"], attrs["x-tvg-url"])
            .flatMap { it.split(',', ';') }
            .map { it.trim() }
            .filter { it.isHttpUrl() }
            .forEach(epgUrls::add)
    }

    private fun elapsedMs(startedNs: Long): Long = nsToMs(System.nanoTime() - startedNs)

    private fun ParseStageMetrics?.measureSeries(block: () -> SeriesEpisodeInfo?): SeriesEpisodeInfo? {
        if (this == null) return block()
        val startedNs = now()
        return block().also { addSeries(startedNs) }
    }

    private fun ParseStageMetrics?.measureClassification(block: () -> KindGuess): KindGuess {
        if (this == null) return block()
        val startedNs = now()
        return block().also { addClassification(startedNs) }
    }

    private fun ParseStageMetrics?.measureCategory(block: () -> String): String {
        if (this == null) return block()
        val startedNs = now()
        return block().also { addCategory(startedNs) }
    }

    private class ParseStageMetrics {
        private var contentCleaningNs: Long = 0L
        private var seriesNs: Long = 0L
        private var classificationNs: Long = 0L
        private var categoryNs: Long = 0L

        fun now(): Long = System.nanoTime()

        fun addContentCleaning(startedNs: Long?) {
            if (startedNs != null) contentCleaningNs += System.nanoTime() - startedNs
        }

        fun addSeries(startedNs: Long) {
            seriesNs += System.nanoTime() - startedNs
        }

        fun addClassification(startedNs: Long) {
            classificationNs += System.nanoTime() - startedNs
        }

        fun addCategory(startedNs: Long) {
            categoryNs += System.nanoTime() - startedNs
        }

        fun measuredMs(): ParseStageMeasuredMs {
            val contentMs = nsToMs(contentCleaningNs)
            val seriesMs = nsToMs(seriesNs)
            val classificationMs = nsToMs(classificationNs)
            val categoryMs = nsToMs(categoryNs)
            return ParseStageMeasuredMs(
                contentCleaningMs = contentMs,
                kindSplitMs = seriesMs + classificationMs,
                categoryMs = categoryMs,
                seriesMs = seriesMs,
                classificationMs = classificationMs,
                totalMs = contentMs + seriesMs + classificationMs + categoryMs,
            )
        }
    }

    private data class ParseStageMeasuredMs(
        val contentCleaningMs: Long = 0L,
        val kindSplitMs: Long = 0L,
        val categoryMs: Long = 0L,
        val seriesMs: Long = 0L,
        val classificationMs: Long = 0L,
        val totalMs: Long = 0L,
    )

    private companion object {
        const val PROGRESS_STEP = 100

        fun nsToMs(ns: Long): Long = ns / 1_000_000L
    }
}
