package com.hktnv.iptvbox.model

internal data class SeriesGroup(
    val id: String,
    val title: String,
    val category: String?,
    val logoUrl: String?,
    val seasonCount: Int,
    val episodeCount: Int,
    val firstOrder: Int,
)

internal data class SeasonGroup(
    val id: String,
    val title: String,
    val seasonNumber: Int,
    val episodeCount: Int,
    val logoUrl: String?,
    val firstOrder: Int,
)
