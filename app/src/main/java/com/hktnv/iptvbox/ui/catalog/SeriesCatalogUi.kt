package com.hktnv.iptvbox.ui.catalog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.model.SeasonGroup
import com.hktnv.iptvbox.model.SeriesGroup
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.media.ContentGrid
import com.hktnv.iptvbox.ui.media.MediaCardGrid
import com.hktnv.iptvbox.ui.media.SeasonCard
import com.hktnv.iptvbox.ui.media.SeriesGroupCard

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
                onLongClick = { group ->
                    snapshot.episodes(group.title, null).firstOrNull()?.let(onShowItemOptions)
                },
                modifier = modifier,
                requestInitialFocus = requestInitialFocus,
                initialFocusRequester = initialFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
            )
        }
        selectedSeasonNumber == null -> {
            SeasonGroupGrid(
                seasons = seasons,
                seriesTitle = selectedSeriesTitle,
                onOpen = { onSeasonSelected(it.seasonNumber) },
                onLongClick = { season ->
                    snapshot.episodes(selectedSeriesTitle, season.seasonNumber).firstOrNull()?.let(onShowItemOptions)
                },
                modifier = modifier,
                requestInitialFocus = requestInitialFocus,
                initialFocusRequester = initialFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
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
                    onShowItemOptions = onShowItemOptions,
                    modifier = Modifier.weight(1f),
                    requestInitialFocus = requestInitialFocus,
                    initialFocusRequester = initialFocusRequester,
                    onRequestSideMenu = onRequestSideMenu,
                )
            }
        }
    }
}

@Composable
internal fun SeriesGroupGrid(
    groups: List<SeriesGroup>,
    onOpen: (SeriesGroup) -> Unit,
    onLongClick: ((SeriesGroup) -> Unit)? = null,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    MediaCardGrid(
        items = groups,
        itemKey = { it.id },
        modifier = modifier,
        requestInitialFocus = requestInitialFocus,
        initialFocusRequester = initialFocusRequester,
        onRequestSideMenu = onRequestSideMenu,
    ) { group, itemModifier, onFocused ->
        SeriesGroupCard(
            group = group,
            onClick = { onOpen(group) },
            onLongClick = onLongClick?.let { { it(group) } },
            onFocused = onFocused,
            modifier = itemModifier,
        )
    }
}

@Composable
internal fun SeasonGroupGrid(
    seasons: List<SeasonGroup>,
    seriesTitle: String,
    onOpen: (SeasonGroup) -> Unit,
    onLongClick: ((SeasonGroup) -> Unit)? = null,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
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
        MediaCardGrid(
            items = seasons,
            itemKey = { it.id },
            modifier = Modifier.weight(1f),
            requestInitialFocus = requestInitialFocus,
            initialFocusRequester = initialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        ) { season, itemModifier, onFocused ->
            SeasonCard(
                season = season,
                onClick = { onOpen(season) },
                onLongClick = onLongClick?.let { { it(season) } },
                onFocused = onFocused,
                modifier = itemModifier,
            )
        }
    }
}
