package com.hktnv.iptvbox.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContentKind {
    LIVE_CHANNEL,
    MOVIE,
    SERIES,
    SEASON,
    EPISODE,
    RADIO,
}

@Serializable
enum class ContentHint {
    AUTO,
    LIVE,
    MOVIES,
    SERIES,
    MIXED,
}

@Serializable
data class CatalogItem(
    val id: String,
    val sourceId: String,
    val kind: ContentKind,
    val title: String,
    val streamUrl: String,
    val category: String? = null,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val seriesTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val xtreamId: Int? = null,
    val rating: String? = null,
    val tmdbId: Int? = null,
    val providerOrder: Int = 0,
    val addedAtEpochMillis: Long = 0L,
)

@Serializable
data class ContentMetadata(
    val itemId: String,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val youtubeTrailer: String? = null,
    val duration: String? = null,
    val backdropUrl: String? = null,
)

@Serializable
data class KindGuess(
    val kind: ContentKind,
    val confidence: Double,
    val reason: String,
)
