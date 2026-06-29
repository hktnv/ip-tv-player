package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.CatalogItem

internal data class ParsedM3uEntry(
    val item: CatalogItem,
    val cleaningNs: Long,
    val seriesNs: Long,
    val kindNs: Long,
    val categoryNs: Long,
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

internal data class SeriesEpisodeInfo(
    val seriesTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitle: String,
)

internal object M3uPlaylistPatterns {
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
