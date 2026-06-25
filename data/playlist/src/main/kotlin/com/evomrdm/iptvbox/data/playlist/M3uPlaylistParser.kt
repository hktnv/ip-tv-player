package com.evomrdm.iptvbox.data.playlist

import com.evomrdm.iptvbox.core.common.SearchNormalizer
import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.core.model.ContentKind
import com.evomrdm.iptvbox.core.model.KindGuess

class M3uPlaylistParser {
    fun parse(sourceId: String, lines: Sequence<String>): ParsedM3uPlaylist {
        val parseStartedNs = System.nanoTime()
        val epgUrls = linkedSetOf<String>()
        val rawEntries = ArrayList<RawM3uEntry>(8_192)
        val lineReadStartedNs = System.nanoTime()
        var pendingExtInfLine: String? = null
        var order = 0

        lines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> Unit
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    val attrs = parseAttributes(line)
                    listOfNotNull(attrs["url-tvg"], attrs["x-tvg-url"])
                        .flatMap { it.split(',', ';') }
                        .map { it.trim() }
                        .filter { it.isHttpUrl() }
                        .forEach(epgUrls::add)
                }
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingExtInfLine = line
                }
                line.startsWith("#") -> Unit
                pendingExtInfLine != null -> {
                    val extInfLine = pendingExtInfLine
                    if (line.isHttpUrl() || line.startsWith("rtsp://", ignoreCase = true)) {
                        order += 1
                        rawEntries += RawM3uEntry(extInfLine = extInfLine, streamUrl = line, providerOrder = order)
                    }
                    pendingExtInfLine = null
                }
            }
        }
        val lineReadMs = elapsedMs(lineReadStartedNs)

        val cleaningStartedNs = System.nanoTime()
        val preparedEntries = ArrayList<PreparedM3uEntry>(rawEntries.size)
        rawEntries.forEach { entry ->
            val info = parseExtInf(entry.extInfLine)
            val title = info.title.takeUnless { it == UNTITLED } ?: urlFileName(entry.streamUrl)
            preparedEntries += PreparedM3uEntry(
                raw = entry,
                info = info,
                title = title,
                normalizedTitle = SearchNormalizer.normalize(title),
            )
        }
        val cleaningMs = elapsedMs(cleaningStartedNs)

        val seriesStartedNs = System.nanoTime()
        val seriesInfo = preparedEntries.map { extractSeriesEpisode(it.title) }
        val seriesMs = elapsedMs(seriesStartedNs)

        val kindStartedNs = System.nanoTime()
        val guesses = preparedEntries.mapIndexed { index, entry ->
            if (seriesInfo[index] != null) {
                KindGuess(ContentKind.EPISODE, 0.95, "series episode pattern")
            } else {
                guessKind(entry.info, entry.raw.streamUrl, entry.title, entry.normalizedTitle)
            }
        }
        val kindSplitMs = elapsedMs(kindStartedNs)

        val categoryStartedNs = System.nanoTime()
        val categories = preparedEntries.mapIndexed { index, entry ->
            deriveCategory(
                rawGroupTitle = entry.info.groupTitle,
                title = entry.title,
                normalizedTitle = entry.normalizedTitle,
                kind = guesses[index].kind,
                series = seriesInfo[index],
            )
        }
        val categoryMs = elapsedMs(categoryStartedNs)

        val items = ArrayList<CatalogItem>(preparedEntries.size)
        preparedEntries.forEachIndexed { index, entry ->
            val series = seriesInfo[index]
            val guess = guesses[index]
            items += CatalogItem(
                id = stableItemId(sourceId, entry.raw.streamUrl, entry.title),
                sourceId = sourceId,
                kind = if (series != null) ContentKind.EPISODE else guess.kind,
                title = entry.title,
                streamUrl = entry.raw.streamUrl,
                category = categories[index],
                logoUrl = entry.info.logoUrl?.takeIf { it.isHttpUrl() },
                tvgId = entry.info.tvgId,
                tvgName = entry.info.tvgName,
                seriesTitle = series?.seriesTitle,
                seasonNumber = series?.seasonNumber,
                episodeNumber = series?.episodeNumber,
                episodeTitle = series?.episodeTitle,
                providerOrder = entry.raw.providerOrder,
            )
        }

        val parseMs = elapsedMs(parseStartedNs)
        val subMs = lineReadMs + cleaningMs + kindSplitMs + categoryMs + seriesMs
        return ParsedM3uPlaylist(
            epgUrls = epgUrls.toList(),
            items = items,
            parseMs = parseMs,
            lineReadMs = lineReadMs,
            contentCleaningMs = cleaningMs,
            kindSplitMs = kindSplitMs,
            categoryMs = categoryMs,
            seriesMs = seriesMs,
            parseOtherMs = (parseMs - subMs).coerceAtLeast(0L),
            classificationMs = kindSplitMs + categoryMs + seriesMs,
        )
    }

    fun parse(sourceId: String, text: String): ParsedM3uPlaylist {
        return parse(sourceId, text.lineSequence())
    }

    fun guessKind(info: ExtInf): KindGuess {
        val title = info.title
        return guessKind(info, "", title, SearchNormalizer.normalize(title))
    }

    fun guessKind(info: ExtInf, streamUrl: String): KindGuess {
        val title = info.title
        return guessKind(info, streamUrl, title, SearchNormalizer.normalize(title))
    }

    private fun guessKind(
        info: ExtInf,
        streamUrl: String,
        title: String,
        normalizedTitle: String,
    ): KindGuess {
        val normalizedGroup = info.normalizedGroupTitle
        val normalizedTvg = info.normalizedTvgName
        return when {
            streamUrl.contains("/series/", ignoreCase = true) ||
                (title.mayContainEpisodeInfo() && seriesRegexes.any { it.containsMatchIn(title) }) ||
                containsSeriesMarker(normalizedTitle, normalizedGroup, normalizedTvg) ->
                KindGuess(ContentKind.EPISODE, 0.88, "series markers")

            streamUrl.contains("/movie/", ignoreCase = true) ||
                mediaExtensionRegex.containsMatchIn(streamUrl) ||
                containsMovieMarker(normalizedTitle, normalizedGroup, normalizedTvg) ->
                KindGuess(ContentKind.MOVIE, 0.86, "movie markers")

            containsRadioMarker(normalizedTitle, normalizedGroup, normalizedTvg) ->
                KindGuess(ContentKind.RADIO, 0.70, "radio markers")

            else ->
                KindGuess(ContentKind.LIVE_CHANNEL, 0.60, "default m3u live assumption")
        }
    }

    private fun parseExtInf(line: String): ExtInf {
        val (metadata, rawTitle) = splitExtInf(line)
        val attrs = parseAttributes(metadata)
        val title = cleanTitle(rawTitle)
            .ifBlank { cleanTitle(attrs["tvg-name"].orEmpty()) }
            .ifBlank { UNTITLED }
        val tvgName = cleanOptional(attrs["tvg-name"])
        val groupTitle = cleanCategory(attrs["group-title"])
        return ExtInf(
            title = title,
            tvgId = cleanOptional(attrs["tvg-id"]),
            tvgName = tvgName,
            groupTitle = groupTitle,
            logoUrl = attrs["tvg-logo"]?.let(::repairEncoding)?.trim()?.takeIf { it.isNotBlank() },
            normalizedGroupTitle = groupTitle?.let(SearchNormalizer::normalize).orEmpty(),
            normalizedTvgName = tvgName?.let(SearchNormalizer::normalize).orEmpty(),
        )
    }

    private fun splitExtInf(line: String): Pair<String, String> {
        var quoted = false
        line.forEachIndexed { index, char ->
            when (char) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) return line.substring(0, index) to line.substring(index + 1)
            }
        }
        return line to ""
    }

    private fun parseAttributes(value: String): Map<String, String> {
        val attrs = LinkedHashMap<String, String>(6)
        attributeRegex.findAll(value).forEach { match ->
            attrs[match.groupValues[1].lowercase()] = repairEncoding(match.groupValues[2]).trim()
        }
        return attrs
    }

    private fun deriveCategory(
        rawGroupTitle: String?,
        title: String,
        normalizedTitle: String,
        kind: ContentKind,
        series: SeriesEpisodeInfo?,
    ): String {
        rawGroupTitle?.takeIf { it.isNotBlank() }?.let { return it }

        return when {
            kind == ContentKind.EPISODE || series != null -> "Diziler"
            kind == ContentKind.MOVIE -> inferMovieCategory(normalizedTitle)
            normalizedTitle.contains("spor") || normalizedTitle.contains("sport") || normalizedTitle.contains("bein") -> "Spor"
            normalizedTitle.contains("haber") || normalizedTitle.contains("news") -> "Haber"
            normalizedTitle.contains("cocuk") || normalizedTitle.contains("kids") || normalizedTitle.contains("cartoon") -> "Çocuk"
            normalizedTitle.contains("belgesel") || normalizedTitle.contains("documentary") -> "Belgesel"
            normalizedTitle.contains("muzik") || normalizedTitle.contains("music") -> "Müzik"
            normalizedTitle.startsWith("tr ") || title.startsWith("TR:", ignoreCase = true) -> "Türk Kanalları"
            normalizedTitle.startsWith("de ") || title.startsWith("DE:", ignoreCase = true) -> "Alman Kanalları"
            normalizedTitle.startsWith("uk ") ||
                normalizedTitle.startsWith("us ") ||
                title.startsWith("UK:", true) ||
                title.startsWith("US:", true) -> "Yabancı Kanallar"
            else -> "Genel"
        }
    }

    private fun inferMovieCategory(normalized: String): String {
        return when {
            normalized.contains("komedi") || normalized.contains("comedy") -> "Komedi"
            normalized.contains("korku") || normalized.contains("horror") -> "Korku"
            normalized.contains("aksiyon") || normalized.contains("action") -> "Aksiyon"
            normalized.contains("dram") || normalized.contains("drama") -> "Dram"
            normalized.contains("belgesel") || normalized.contains("documentary") -> "Belgesel Filmler"
            normalized.contains("cocuk") || normalized.contains("animasyon") || normalized.contains("animation") -> "Çocuk Filmleri"
            else -> "Filmler"
        }
    }

    private fun extractSeriesEpisode(title: String): SeriesEpisodeInfo? {
        if (!title.mayContainEpisodeInfo()) return null
        seriesRegexes.forEach { regex ->
            val match = regex.find(title) ?: return@forEach
            val season = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@forEach
            val episode = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return@forEach
            val seriesTitle = cleanSeriesTitle(title.substring(0, match.range.first))
                .ifBlank { cleanSeriesTitle(title.substringBefore(match.value)) }
            val episodeTitle = cleanTitle(title.substring(match.range.last + 1))
                .ifBlank { "Bölüm $episode" }
            return SeriesEpisodeInfo(
                seriesTitle = seriesTitle.ifBlank { title },
                seasonNumber = season,
                episodeNumber = episode,
                episodeTitle = episodeTitle,
            )
        }
        return null
    }

    private fun cleanSeriesTitle(value: String): String {
        return cleanTitle(value)
            .replace(trailingSeparatorRegex, "")
            .ifBlank { value.trim() }
    }

    private fun cleanTitle(value: String): String {
        return repairEncoding(value)
            .replace(extInfRegex, " ")
            .replace(attributeRegex, " ")
            .replace(urlRegex, " ")
            .replace(secretParamRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim(' ', ',', '-', '|', '»', '•')
            .stripLanguagePrefix()
            .replace(whitespaceRegex, " ")
            .trim()
    }

    private fun cleanCategory(value: String?): String? {
        return cleanOptional(value)
            ?.replace(pipeEdgeRegex, "")
            ?.replace(whitespaceRegex, " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun cleanOptional(value: String?): String? {
        return value
            ?.let(::cleanTitle)
            ?.takeIf { it.isNotBlank() }
    }

    private fun urlFileName(url: String): String {
        return url.substringBefore('?')
            .substringAfterLast('/')
            .substringBeforeLast('.', missingDelimiterValue = "")
            .replace('-', ' ')
            .replace('_', ' ')
            .let(::cleanTitle)
            .ifBlank { UNTITLED }
    }

    private fun String.stripLanguagePrefix(): String {
        return languageCodePrefixRegex.replace(this, "")
            .let { languageWordPrefixRegex.replace(it, "") }
            .trim()
    }

    private fun repairEncoding(value: String): String {
        if (!value.any { it == 'Ã' || it == 'Ä' || it == 'Å' }) return value
        return runCatching {
            String(value.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        }.getOrDefault(value)
    }

    private fun stableItemId(sourceId: String, url: String, title: String): String {
        var hash = 1125899906842597L
        hash = hash.mix(sourceId)
        hash = hash.mix(title)
        hash = hash.mix(url)
        return java.lang.Long.toUnsignedString(hash, 16)
    }

    private fun containsSeriesMarker(title: String, group: String, tvg: String): Boolean {
        return title.hasSeriesMarker() || group.hasSeriesMarker() || tvg.hasSeriesMarker()
    }

    private fun containsMovieMarker(title: String, group: String, tvg: String): Boolean {
        return title.hasMovieMarker() || group.hasMovieMarker() || tvg.hasMovieMarker()
    }

    private fun containsRadioMarker(title: String, group: String, tvg: String): Boolean {
        return title.hasRadioMarker() || group.hasRadioMarker() || tvg.hasRadioMarker()
    }

    private fun String.hasSeriesMarker(): Boolean {
        return contains("series") ||
            contains("serie") ||
            contains("dizi") ||
            contains("sezon") ||
            contains("bolum") ||
            contains("season") ||
            contains("episode")
    }

    private fun String.hasMovieMarker(): Boolean {
        return contains("movie") ||
            contains("movies") ||
            contains("film") ||
            contains("filmler") ||
            contains("vod") ||
            contains("sinema") ||
            contains("cinema")
    }

    private fun String.hasRadioMarker(): Boolean {
        return contains("radio") || contains("radyo") || contains(" fm")
    }

    private fun String.mayContainEpisodeInfo(): Boolean {
        if (contains("sezon", ignoreCase = true) ||
            contains("season", ignoreCase = true) ||
            contains("episode", ignoreCase = true) ||
            contains("bölüm", ignoreCase = true) ||
            contains("bolum", ignoreCase = true)
        ) {
            return true
        }
        for (index in 0 until lastIndex) {
            val current = this[index]
            val next = this[index + 1]
            if ((current == 'S' || current == 's') && next.isDigit()) return true
            if (current.isDigit() && (next == 'x' || next == 'X')) return true
        }
        return false
    }

    private fun Long.mix(value: String): Long {
        var current = this
        value.forEach { char ->
            current = current * 31 + char.code
        }
        return current
    }

    private fun elapsedMs(startedNs: Long): Long = (System.nanoTime() - startedNs) / 1_000_000L

    private companion object {
        const val UNTITLED = "İsimsiz içerik"
        val attributeRegex = Regex("""([\w-]+)="([^"]*)"""")
        val seriesRegexes = listOf(
            Regex("""\bS(?:eason)?\s*(\d{1,2})\s*[ ._-]*E(?:pisode)?\s*(\d{1,3})\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{1,2})x(\d{1,3})\b""", RegexOption.IGNORE_CASE),
            Regex("""\bsezon\s*(\d{1,2})\s*(?:bölüm|bolum|episode|ep\.?)\s*(\d{1,3})\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{1,2})\.\s*sezon\s*(\d{1,3})\.\s*(?:bölüm|bolum)\b""", RegexOption.IGNORE_CASE),
        )
        val mediaExtensionRegex = Regex("""\.(mp4|mkv|avi|mov|m4v)(\?|$)""", RegexOption.IGNORE_CASE)
        val extInfRegex = Regex("""#EXTINF[^,]*,?""", RegexOption.IGNORE_CASE)
        val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
        val secretParamRegex = Regex("""\b(output|type|username|password|token)=\S+""", RegexOption.IGNORE_CASE)
        val whitespaceRegex = Regex("""\s+""")
        val trailingSeparatorRegex = Regex("""[\s._-]+$""")
        val pipeEdgeRegex = Regex("""^\|+|\|+$""")
        val languageCodePrefixRegex = Regex("""^[A-Z]{2,3}\s*[:|\-]\s*""")
        val languageWordPrefixRegex = Regex("""^(TR|DE|UK|US|EN)\s+""", RegexOption.IGNORE_CASE)
    }
}

private data class RawM3uEntry(
    val extInfLine: String,
    val streamUrl: String,
    val providerOrder: Int,
)

private data class PreparedM3uEntry(
    val raw: RawM3uEntry,
    val info: ExtInf,
    val title: String,
    val normalizedTitle: String,
)

data class ParsedM3uPlaylist(
    val epgUrls: List<String>,
    val items: List<CatalogItem>,
    val parseMs: Long = 0L,
    val lineReadMs: Long = 0L,
    val contentCleaningMs: Long = 0L,
    val kindSplitMs: Long = 0L,
    val categoryMs: Long = 0L,
    val seriesMs: Long = 0L,
    val parseOtherMs: Long = 0L,
    val classificationMs: Long = 0L,
)

data class ExtInf(
    val title: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val groupTitle: String? = null,
    val logoUrl: String? = null,
    val normalizedGroupTitle: String = "",
    val normalizedTvgName: String = "",
)

private data class SeriesEpisodeInfo(
    val seriesTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitle: String,
)
