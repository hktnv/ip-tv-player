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
)

data class XtreamMetadataPayload(
    val plot: String?,
    val cast: String?,
    val director: String?,
    val youtubeTrailer: String?,
    val duration: String?,
    val backdropUrl: String?,
)
