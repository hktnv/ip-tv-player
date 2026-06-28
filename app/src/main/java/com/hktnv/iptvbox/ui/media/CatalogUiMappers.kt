package com.hktnv.iptvbox.ui.media
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.data.catalog.episodes
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistStats
import com.hktnv.iptvbox.model.SeasonGroup
import com.hktnv.iptvbox.model.SeriesGroup
import com.hktnv.iptvbox.ui.catalog.label

private val episodeTitleRegex = Regex(
    """(?i)\bS(?:eason)?\s*(\d{1,2})\s*[ ._-]*E(?:pisode)?\s*(\d{1,3})\b|\b(\d{1,2})x(\d{1,3})\b""",
)

internal fun LoadedPlaylist.stats(): PlaylistStats {
    if (cachedLiveCount != null && cachedMovieCount != null && cachedSeriesCount != null) {
        return PlaylistStats(
            live = cachedLiveCount,
            movies = cachedMovieCount,
            series = cachedSeriesCount,
        )
    }
    val seriesTitles = HashSet<String>()
    items.forEach { item ->
        if (item.kind == ContentKind.EPISODE || item.seriesTitle != null) {
            seriesTitles += item.seriesDisplayTitle()
        }
    }
    return PlaylistStats(
        live = items.count { it.kind in CatalogTab.LIVE.kinds },
        movies = items.count { it.kind == ContentKind.MOVIE },
        series = seriesTitles.size,
    )
}

internal fun LoadedPlaylist.count(tab: CatalogTab): Int = stats().count(tab)

internal fun firstAvailableTab(playlist: LoadedPlaylist): CatalogTab {
    val stats = playlist.stats()
    return when {
        stats.live > 0 -> CatalogTab.LIVE
        stats.movies > 0 -> CatalogTab.MOVIES
        stats.series > 0 -> CatalogTab.SERIES
        else -> CatalogTab.LIVE
    }
}

internal fun LoadedPlaylist.itemsByIds(ids: List<String>): List<CatalogItem> {
    val byId = items.associateBy { it.id }
    return ids.mapNotNull { byId[it] }
}

internal fun LoadedPlaylist.catalogSummary(): String {
    return stats().catalogSummary()
}

internal fun PlaylistStats.catalogSummary(): String = "$live canlı · $movies film · $series dizi"

internal fun buildCatalogSubtitle(
    playlist: LoadedPlaylist,
    tab: CatalogTab,
    category: String?,
    seriesTitle: String?,
    seasonNumber: Int?,
): String {
    val parts = mutableListOf(playlist.name, "${playlist.count(tab)} içerik")
    category?.takeIf { it.isNotBlank() }?.let(parts::add)
    seriesTitle?.takeIf { it.isNotBlank() }?.let(parts::add)
    seasonNumber?.let { parts += "Sezon $it" }
    return parts.joinToString(" · ")
}

internal fun LoadedPlaylist.normalizedForUi(): LoadedPlaylist {
    val normalizedItems = items.map { it.normalizedForUi() }
    val normalized = copy(
        items = normalizedItems,
        warnings = warnings.map(::simpleUserMessage).filter { it.isNotBlank() }.distinct().take(1),
    )
    val stats = normalized.statsWithoutCache()
    return normalized.copy(
        cachedItemCount = normalizedItems.size,
        cachedLiveCount = stats.live,
        cachedMovieCount = stats.movies,
        cachedSeriesCount = stats.series,
    )
}

internal fun catalogSignature(playlists: List<LoadedPlaylist>): String {
    return playlists.joinToString("|") { playlist ->
        listOf(
            playlist.id,
            playlist.type.name,
            playlist.items.size,
            playlist.cachedItemCount ?: 0,
            playlist.warnings.hashCode(),
        ).joinToString(":")
    }
}

internal fun simpleUserMessage(value: String): String {
    val normalized = SearchNormalizer.normalize(value)
    return when {
        normalized.isBlank() -> ""
        "baglan" in normalized || "connect" in normalized || "timeout" in normalized ||
            "tls" in normalized || "ssl" in normalized || "packet" in normalized -> "Bağlantı kurulamadı"
        "bulunamadi" in normalized || "404" in normalized -> "İçerik bulunamadı"
        "yuklen" in normalized || "http" in normalized || "https" in normalized -> ""
        else -> ""
    }
}

internal fun LoadedPlaylist.seriesGroups(): List<SeriesGroup> = items
    .filter { it.kind == ContentKind.EPISODE || it.seriesTitle != null }
    .seriesGroups()

internal fun LoadedPlaylist.seriesPreview(limit: Int): List<SeriesGroup> {
    if (limit <= 0) return emptyList()
    val titles = linkedSetOf<String>()
    for (item in items) {
        if (item.kind == ContentKind.EPISODE || item.seriesTitle != null) {
            titles += item.seriesDisplayTitle()
            if (titles.size >= limit) break
        }
    }
    if (titles.isEmpty()) return emptyList()

    return titles.map { title ->
        val episodes = items.asSequence()
            .filter { (it.kind == ContentKind.EPISODE || it.seriesTitle != null) && it.seriesDisplayTitle() == title }
            .toList()
        SeriesGroup(
            id = SearchNormalizer.normalize(title).ifBlank { title },
            title = title,
            category = episodes.firstNotNullOfOrNull { it.category },
            logoUrl = episodes.firstNotNullOfOrNull { it.logoUrl },
            seasonCount = episodes.map { it.seasonNumber ?: 1 }.distinct().size,
            episodeCount = episodes.size,
            firstOrder = episodes.minOfOrNull { it.providerOrder } ?: 0,
        )
    }
}

internal fun CatalogItem.seriesDisplayTitle(): String {
    val explicit = seriesTitle?.cleanUiTitle().orEmpty()
    if (explicit.isNotBlank()) return explicit.readableContentTitle()
    val marker = Regex("""(?i)\bS\d{1,2}\s*E\d{1,3}\b""").find(title)
    val inferred = if (marker == null) title else title.substring(0, marker.range.first)
    return inferred.cleanUiTitle().ifBlank { displayTitle() }.readableContentTitle()
}

internal fun CatalogItem.displayTitle(): String = title.cleanUiTitle()
    .ifBlank { tvgName?.cleanUiTitle().orEmpty() }
    .ifBlank { kind.label() }
    .readableContentTitle()

internal fun CatalogItem.metaLine(): String {
    val cleanCategory = category?.cleanUiTitle().orEmpty()
    val cleanSeries = seriesTitle?.cleanUiTitle().orEmpty()
    return when {
        kind == ContentKind.EPISODE && seasonNumber != null && episodeNumber != null ->
            listOfNotNull(
                cleanSeries.takeIf { it.isNotBlank() },
                "Sezon $seasonNumber",
                "Bölüm $episodeNumber",
            ).joinToString(" · ")
        cleanCategory.isNotBlank() -> cleanCategory
        else -> kind.label()
    }
}

internal fun CatalogItem.posterRatio(): Float {
    return when (kind) {
        ContentKind.MOVIE,
        ContentKind.SERIES,
        ContentKind.SEASON,
        ContentKind.EPISODE -> 2f / 3f
        ContentKind.LIVE_CHANNEL,
        ContentKind.RADIO -> 16f / 9f
    }
}

internal fun String.cleanUiTitle(): String {
    return replace(Regex("""#EXTINF[^,]*,?""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("[\\w-]+=\"[^\"]*\"", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""https?://\S+""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\b(output|type|username|password|token)=\S+""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""^[A-Z]{2,3}\s*[:|\-]\s*"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim { it == ' ' || it == ',' || it == '-' || it == '|' || it.code == 0x00BB || it.code == 0x2022 }
}

internal fun String.initials(): String {
    val words = cleanUiTitle().split(' ').filter { it.isNotBlank() }
    return words.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifBlank { "IP" }
}

internal fun restoredTab(name: String?): CatalogTab {
    return CatalogTab.entries.firstOrNull { it.name == name } ?: CatalogTab.LIVE
}

internal fun restoredScreen(name: String?, hasPlaylist: Boolean): AppScreen {
    return AppScreen.HOME
}

internal fun AppScreen.requiresPlaylist(): Boolean {
    return this == AppScreen.CATALOG ||
        this == AppScreen.SEARCH ||
        this == AppScreen.LATEST ||
        this == AppScreen.FAVORITES ||
        this == AppScreen.RECENT ||
        this == AppScreen.PLAYER
}

internal fun toggleFavorite(favoriteIds: MutableList<String>, itemId: String) {
    if (itemId in favoriteIds) {
        favoriteIds.remove(itemId)
    } else {
        favoriteIds.add(0, itemId)
    }
}

private fun LoadedPlaylist.statsWithoutCache(): PlaylistStats {
    val seriesTitles = HashSet<String>()
    var live = 0
    var movies = 0
    items.forEach { item ->
        when {
            item.kind in CatalogTab.LIVE.kinds -> live += 1
            item.kind == ContentKind.MOVIE -> movies += 1
            item.kind == ContentKind.EPISODE || item.seriesTitle != null -> seriesTitles += item.seriesDisplayTitle()
        }
    }
    return PlaylistStats(live = live, movies = movies, series = seriesTitles.size)
}

private fun CatalogItem.normalizedForUi(): CatalogItem {
    val refinedKind = refinedKind()
    val episodeHint = if (refinedKind == ContentKind.EPISODE) episodeHint() else null
    return copy(
        kind = refinedKind,
        title = displayTitle(),
        category = category?.cleanUiTitle()?.ifBlank { null },
        seriesTitle = seriesTitle?.cleanUiTitle()?.ifBlank { null } ?: episodeHint?.seriesTitle,
        seasonNumber = seasonNumber ?: episodeHint?.seasonNumber,
        episodeNumber = episodeNumber ?: episodeHint?.episodeNumber,
        episodeTitle = episodeTitle?.cleanUiTitle()?.ifBlank { null } ?: episodeHint?.episodeTitle,
    )
}

private fun CatalogItem.refinedKind(): ContentKind {
    val url = streamUrl.lowercase()
    val text = SearchNormalizer.normalize(listOf(title, category.orEmpty(), tvgName.orEmpty()).joinToString(" "))
    return when {
        "/live/" in url || Regex("\\.(ts|m3u8)(\\?|$)").containsMatchIn(url) -> ContentKind.LIVE_CHANNEL
        "/series/" in url || episodeHint() != null -> ContentKind.EPISODE
        "/movie/" in url || Regex("\\.(mp4|mkv|avi|mov|m4v)(\\?|$)").containsMatchIn(url) -> ContentKind.MOVIE
        Regex("\\b(film|filmler|movie|movies|vod|sinema)\\b").containsMatchIn(text) -> ContentKind.MOVIE
        else -> kind
    }
}

private data class EpisodeHint(
    val seriesTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitle: String?,
)

private fun CatalogItem.episodeHint(): EpisodeHint? {
    val match = episodeTitleRegex.find(title) ?: return null
    val season = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        ?: match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        ?: return null
    val episode = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        ?: match.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        ?: return null
    val series = title.substring(0, match.range.first).cleanUiTitle().ifBlank { title.cleanUiTitle() }
    val tail = title.substring(match.range.last + 1).cleanUiTitle().ifBlank { null }
    return EpisodeHint(series, season, episode, tail)
}

private fun List<CatalogItem>.seriesGroups(): List<SeriesGroup> {
    return groupBy { it.seriesDisplayTitle() }
        .map { (title, episodes) ->
            SeriesGroup(
                id = SearchNormalizer.normalize(title).ifBlank { title },
                title = title,
                category = episodes.firstNotNullOfOrNull { it.category },
                logoUrl = episodes.firstNotNullOfOrNull { it.logoUrl },
                seasonCount = episodes.map { it.seasonNumber ?: 1 }.distinct().size,
                episodeCount = episodes.size,
                firstOrder = episodes.minOfOrNull { it.providerOrder } ?: 0,
            )
        }
        .sortedWith(compareBy<SeriesGroup> { it.firstOrder }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
}

