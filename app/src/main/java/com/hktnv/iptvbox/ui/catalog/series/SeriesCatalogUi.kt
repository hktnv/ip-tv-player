package com.hktnv.iptvbox.ui.catalog.series

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.media.ContentGrid

@Composable
internal fun SeriesCatalogContent(
    snapshot: CatalogSnapshot,
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    favoriteIds: List<String>,
    onSeriesSelected: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onRequestSideMenu: () -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
) {
    val seriesGroups = remember(snapshot, selectedCategory) { snapshot.seriesGroups(selectedCategory) }
    val seasons = remember(snapshot, selectedSeriesTitle) {
        selectedSeriesTitle?.let(snapshot::seasons).orEmpty()
    }
    val visibleEpisodes = remember(snapshot, selectedSeriesTitle, selectedSeasonNumber) {
        selectedSeriesTitle?.let { snapshot.episodes(it, selectedSeasonNumber) }.orEmpty()
    }
    val stage = remember(selectedSeriesTitle, selectedSeasonNumber, seriesGroups, seasons, visibleEpisodes) {
        resolveSeriesCatalogStage(
            selectedSeriesTitle = selectedSeriesTitle,
            selectedSeasonNumber = selectedSeasonNumber,
            hasSeriesGroups = seriesGroups.isNotEmpty(),
            hasSeasons = seasons.isNotEmpty(),
            hasEpisodes = visibleEpisodes.isNotEmpty(),
        )
    }

    when (stage) {
        SeriesCatalogStage.EmptySeries -> EmptyState(
            title = "Dizi bulunamadı",
            body = "Bu kategoride dizi bölümü yok. Başka kategori seç veya farklı bir liste yükle.",
            actionLabel = null,
            onAction = null,
            modifier = modifier.padding(top = 18.dp),
        )
        SeriesCatalogStage.SeriesGroups -> SeriesGroupGrid(
            groups = seriesGroups,
            onOpen = { onSeriesSelected(it.title) },
            onLongClick = { group ->
                snapshot.episodes(group.title, null).firstOrNull()?.let(onShowItemOptions)
            },
            modifier = modifier,
            requestInitialFocus = requestInitialFocus,
            initialFocusRequester = initialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        SeriesCatalogStage.Seasons -> SeasonGroupGrid(
            seasons = seasons,
            seriesTitle = selectedSeriesTitle.orEmpty(),
            onOpen = { onSeasonSelected(it.seasonNumber) },
            onLongClick = { season ->
                selectedSeriesTitle
                    ?.let { snapshot.episodes(it, season.seasonNumber).firstOrNull() }
                    ?.let(onShowItemOptions)
            },
            modifier = modifier,
            requestInitialFocus = requestInitialFocus,
            initialFocusRequester = initialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        SeriesCatalogStage.Episodes -> SeriesEpisodesGrid(
            title = selectedSeriesTitle.orEmpty(),
            seasonNumber = selectedSeasonNumber,
            episodes = visibleEpisodes,
            favoriteIds = favoriteIds,
            onOpenItem = onOpenItem,
            onShowItemOptions = onShowItemOptions,
            modifier = modifier,
            requestInitialFocus = requestInitialFocus,
            initialFocusRequester = initialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        SeriesCatalogStage.EmptyDetail -> EmptyState(
            title = "Dizi içeriği bulunamadı",
            body = "Bu dizi için bölüm bilgisi bulunamadı. Başka kategori seç veya listeyi yenile.",
            actionLabel = null,
            onAction = null,
            modifier = modifier.padding(top = 18.dp),
        )
    }
}

@Composable
private fun SeriesEpisodesGrid(
    title: String,
    seasonNumber: Int?,
    episodes: List<CatalogItem>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onRequestSideMenu: () -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (seasonNumber == null) title else "$title · Sezon $seasonNumber",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ContentGrid(
            items = episodes,
            favoriteIds = favoriteIds,
            onOpenItem = onOpenItem,
            onShowItemOptions = onShowItemOptions,
            modifier = Modifier.weight(1f),
            requestInitialFocus = requestInitialFocus,
            initialFocusRequester = initialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
    }
}
