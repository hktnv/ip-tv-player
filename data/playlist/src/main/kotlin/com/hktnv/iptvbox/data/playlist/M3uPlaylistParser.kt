package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.KindGuess

class M3uPlaylistParser {
    fun parse(
        sourceId: String,
        lines: Sequence<String>,
        onItemParsed: (Int) -> Unit = {},
    ): ParsedM3uPlaylist {
        val parseStartedNs = System.nanoTime()
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
                        items += parseEntry(sourceId, extInfLine, line, order)
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
        return ParsedM3uPlaylist(
            epgUrls = epgUrls.toList(),
            items = items,
            parseMs = parseMs,
            parseOtherMs = parseMs,
        )
    }

    fun parse(sourceId: String, text: String): ParsedM3uPlaylist {
        return parse(sourceId, text.lineSequence())
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
    ): CatalogItem {
        val info = parseExtInf(extInfLine)
        val title = info.title.takeUnless { it == M3uPlaylistPatterns.UNTITLED } ?: m3uUrlFileName(streamUrl)
        val normalizedTitle = normalizeM3uMarkers(title)
        val series = extractSeriesEpisode(title)
        val guess = if (series != null) {
            KindGuess(ContentKind.EPISODE, 0.95, "series episode pattern")
        } else {
            guessKind(info, streamUrl, title, normalizedTitle)
        }
        val category = deriveCategory(
            rawGroupTitle = info.groupTitle,
            title = title,
            normalizedTitle = normalizedTitle,
            kind = guess.kind,
            series = series,
        )

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
    ): KindGuess = guessM3uKind(info, streamUrl, title, normalizedTitle)

    private fun collectEpgUrls(line: String, epgUrls: MutableSet<String>) {
        val attrs = parseAttributes(line)
        listOfNotNull(attrs["url-tvg"], attrs["x-tvg-url"])
            .flatMap { it.split(',', ';') }
            .map { it.trim() }
            .filter { it.isHttpUrl() }
            .forEach(epgUrls::add)
    }

    private fun elapsedMs(startedNs: Long): Long = nsToMs(System.nanoTime() - startedNs)

    private fun nsToMs(ns: Long): Long = ns / 1_000_000L

    private companion object {
        const val PROGRESS_STEP = 100
    }
}
