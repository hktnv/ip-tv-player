package com.hktnv.iptvbox

import com.hktnv.iptvbox.ui.catalog.series.SeriesCatalogStage
import com.hktnv.iptvbox.ui.catalog.series.resolveSeriesCatalogStage
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesCatalogStateTest {
    @Test
    fun selectedSeriesNeverFallsBackToEmptyCategoryGroups() {
        assertEquals(
            SeriesCatalogStage.Episodes,
            resolveSeriesCatalogStage(
                selectedSeriesTitle = "Prens",
                selectedSeasonNumber = null,
                hasSeriesGroups = false,
                hasSeasons = false,
                hasEpisodes = true,
            ),
        )
    }

    @Test
    fun selectedSeriesShowsSeasonsBeforeEpisodeGridWhenSeasonsExist() {
        assertEquals(
            SeriesCatalogStage.Seasons,
            resolveSeriesCatalogStage(
                selectedSeriesTitle = "Prens",
                selectedSeasonNumber = null,
                hasSeriesGroups = true,
                hasSeasons = true,
                hasEpisodes = true,
            ),
        )
    }

    @Test
    fun selectedSeasonShowsEpisodesOnlyWhenEpisodesExist() {
        assertEquals(
            SeriesCatalogStage.EmptyDetail,
            resolveSeriesCatalogStage(
                selectedSeriesTitle = "Prens",
                selectedSeasonNumber = 1,
                hasSeriesGroups = true,
                hasSeasons = true,
                hasEpisodes = false,
            ),
        )
    }
}
