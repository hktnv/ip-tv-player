package com.evomrdm.iptvbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import com.evomrdm.iptvbox.core.model.CatalogItem

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
    onToggleFavorite: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seriesGroups = remember(snapshot, selectedCategory) { snapshot.seriesGroups(selectedCategory) }
    val seasons = remember(snapshot, selectedSeriesTitle) {
        selectedSeriesTitle?.let(snapshot::seasons).orEmpty()
    }
    val visibleEpisodes = remember(snapshot, selectedSeriesTitle, selectedSeasonNumber) {
        selectedSeriesTitle?.let { snapshot.episodes(it, selectedSeasonNumber) }.orEmpty()
    }

    when {
        seriesGroups.isEmpty() -> {
            EmptyState(
                title = "Dizi bulunamadı",
                body = "Bu kategoride dizi bölümü yok. Başka kategori seç veya farklı bir liste yükle.",
                actionLabel = null,
                onAction = null,
                modifier = modifier.padding(top = 18.dp),
            )
        }
        selectedSeriesTitle == null -> {
            SeriesGroupGrid(
                groups = seriesGroups,
                onOpen = { onSeriesSelected(it.title) },
                modifier = modifier,
            )
        }
        selectedSeasonNumber == null -> {
            SeasonGroupGrid(
                seasons = seasons,
                seriesTitle = selectedSeriesTitle,
                onOpen = { onSeasonSelected(it.seasonNumber) },
                modifier = modifier,
            )
        }
        else -> {
            Column(modifier = modifier.fillMaxWidth()) {
                Text(
                    text = "$selectedSeriesTitle · Sezon $selectedSeasonNumber",
                    color = IptvColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ContentGrid(
                    items = visibleEpisodes,
                    favoriteIds = favoriteIds,
                    onOpenItem = onOpenItem,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun SeriesGroupGrid(
    groups: List<SeriesGroup>,
    onOpen: (SeriesGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minCell = if (maxWidth >= 700.dp) 190.dp else 145.dp
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minCell),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = ScreenBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(groups, key = { it.id }) { group ->
                SeriesGroupCard(group = group, onClick = { onOpen(group) })
            }
        }
    }
}

@Composable
internal fun SeasonGroupGrid(
    seasons: List<SeasonGroup>,
    seriesTitle: String,
    onOpen: (SeasonGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = seriesTitle,
            color = IptvColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val minCell = if (maxWidth >= 700.dp) 190.dp else 145.dp
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minCell),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = ScreenBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(seasons, key = { it.id }) { season ->
                    SeasonCard(season = season, onClick = { onOpen(season) })
                }
            }
        }
    }
}
