package com.hktnv.iptvbox.ui.catalog.series

internal enum class SeriesCatalogStage {
    EmptySeries,
    SeriesGroups,
    Seasons,
    Episodes,
    EmptyDetail,
}

internal fun resolveSeriesCatalogStage(
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    hasSeriesGroups: Boolean,
    hasSeasons: Boolean,
    hasEpisodes: Boolean,
): SeriesCatalogStage {
    return when {
        selectedSeriesTitle == null && !hasSeriesGroups -> SeriesCatalogStage.EmptySeries
        selectedSeriesTitle == null -> SeriesCatalogStage.SeriesGroups
        selectedSeasonNumber == null && hasSeasons -> SeriesCatalogStage.Seasons
        hasEpisodes -> SeriesCatalogStage.Episodes
        else -> SeriesCatalogStage.EmptyDetail
    }
}
