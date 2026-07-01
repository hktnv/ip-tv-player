package com.hktnv.iptvbox.repository.catalog

import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistStats

internal fun LoadedPlaylist.cachedStats(): PlaylistStats {
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

internal fun CatalogItem.searchTextForIndex(): String {
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

internal fun CatalogItem.seriesDisplayTitleForIndex(): String {
    val explicit = seriesTitle?.cleanIndexTitle().orEmpty()
    if (explicit.isNotBlank()) return explicit
    val marker = episodeMarkerRegex.find(title)
    val inferred = if (marker == null) title else title.substring(0, marker.range.first)
    return inferred.cleanIndexTitle().ifBlank { title.cleanIndexTitle() }
}

private fun String.cleanIndexTitle(): String {
    return replace(extInfRegex, " ")
        .replace(attributeRegex, " ")
        .replace(urlRegex, " ")
        .replace(secretParamRegex, " ")
        .replace(languageCodePrefixRegex, "")
        .replace(whitespaceRegex, " ")
        .trim(' ', ',', '-', '|', '\u00BB', '\u2022')
}

private val episodeMarkerRegex = Regex("""(?i)\bS\d{1,2}\s*E\d{1,3}\b""")
private val extInfRegex = Regex("""#EXTINF[^,]*,?""", RegexOption.IGNORE_CASE)
private val attributeRegex = Regex("[\\w-]+=\"[^\"]*\"", RegexOption.IGNORE_CASE)
private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
private val secretParamRegex = Regex("""\b(output|type|username|password|token)=\S+""", RegexOption.IGNORE_CASE)
private val languageCodePrefixRegex = Regex("""^[A-Z]{2,3}\s*[:|\-]\s*""")
private val whitespaceRegex = Regex("""\s+""")
