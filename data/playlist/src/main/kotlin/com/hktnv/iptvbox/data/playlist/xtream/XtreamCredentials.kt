package com.hktnv.iptvbox.data.playlist.xtream

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
)

data class XtreamBulkEntry(
    val xtreamId: Int,
    val title: String,
    val posterUrl: String?,
    val rating: String?,
    val tmdbId: Int?,
    val addedAtEpochSeconds: Long?,
)

data class XtreamCategoryEntry(
    val categoryId: String,
    val title: String,
)

data class XtreamCategoryMapping(
    val kind: String,
    val localName: String,
    val xtreamCategoryId: String,
)

data class XtreamMetadataPayload(
    val plot: String?,
    val cast: String?,
    val director: String?,
    val youtubeTrailer: String?,
    val duration: String?,
    val backdropUrl: String?,
)

data class XtreamSeriesInfoPayload(
    val metadata: XtreamMetadataPayload,
    val episodes: List<XtreamSeriesEpisodeEntry>,
)

data class XtreamSeriesEpisodeEntry(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String?,
    val plot: String?,
    val imageUrl: String?,
)
