package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.KindGuess

internal fun guessM3uKind(
    info: ExtInf,
    streamUrl: String,
    title: String,
    normalizedTitle: String,
    hasSeriesEpisodePattern: Boolean = title.hasSeriesEpisodePattern(),
): KindGuess {
    val normalizedGroup = info.normalizedGroupTitle
    val normalizedTvg = info.normalizedTvgName
    return when {
        streamUrl.contains("/live/", ignoreCase = true) || streamUrl.hasLiveExtension() ->
            KindGuess(ContentKind.LIVE_CHANNEL, 0.94, "live url")

        streamUrl.contains("/movie/", ignoreCase = true) ||
            streamUrl.hasMovieExtension() ||
            containsMovieMarker(normalizedTitle, normalizedGroup, normalizedTvg) ->
            KindGuess(ContentKind.MOVIE, 0.86, "movie markers")

        streamUrl.contains("/series/", ignoreCase = true) ||
            hasSeriesEpisodePattern ||
            containsSeriesMarker(normalizedTitle, normalizedGroup, normalizedTvg) ->
            KindGuess(ContentKind.EPISODE, 0.88, "series markers")

        containsRadioMarker(normalizedTitle, normalizedGroup, normalizedTvg) ->
            KindGuess(ContentKind.RADIO, 0.70, "radio markers")

        else -> KindGuess(ContentKind.LIVE_CHANNEL, 0.60, "default m3u live assumption")
    }
}

internal fun String.hasSeriesEpisodePattern(): Boolean {
    if (!mayContainEpisodeInfo()) return false
    return M3uPlaylistPatterns.seriesRegexes.any { it.containsMatchIn(this) }
}

internal fun deriveCategory(
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

private fun String.hasRadioMarker(): Boolean = contains("radio") || contains("radyo") || contains(" fm")

private fun String.hasMovieExtension(): Boolean = hasPathExtension(MOVIE_EXTENSIONS)

private fun String.hasLiveExtension(): Boolean = hasPathExtension(LIVE_EXTENSIONS)

private fun String.hasPathExtension(extensions: Set<String>): Boolean {
    val path = substringBefore('?').substringBefore('#')
    val dotIndex = path.lastIndexOf('.')
    if (dotIndex < 0 || dotIndex == path.lastIndex) return false
    return path.substring(dotIndex + 1).lowercase() in extensions
}

private val MOVIE_EXTENSIONS = setOf("mp4", "avi", "mov", "m4v")
private val LIVE_EXTENSIONS = setOf("ts", "m3u8")
