package com.hktnv.iptvbox.data.playlist

internal fun extractSeriesEpisode(title: String): SeriesEpisodeInfo? {
    if (!title.mayContainEpisodeInfo()) return null
    M3uPlaylistPatterns.seriesRegexes.forEach { regex ->
        val match = regex.find(title) ?: return@forEach
        val season = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@forEach
        val episode = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return@forEach
        val seriesTitle = cleanSeriesTitle(title.substring(0, match.range.first))
            .ifBlank { cleanSeriesTitle(title.substringBefore(match.value)) }
        val episodeTitle = cleanM3uTitle(title.substring(match.range.last + 1))
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

internal fun String.mayContainEpisodeInfo(): Boolean {
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

private fun cleanSeriesTitle(value: String): String {
    return cleanM3uTitle(value)
        .replace(M3uPlaylistPatterns.trailingSeparatorRegex, "")
        .ifBlank { value.trim() }
}
