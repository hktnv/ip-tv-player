package com.hktnv.iptvbox.ui.catalog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
    onToggleFavorite: (CatalogItem) -> Unit,
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
                    onToggleFavorite = onToggleFavorite,
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
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fallbackFocusRequester = remember { FocusRequester() }
        val itemFocusRequester = initialFocusRequester ?: fallbackFocusRequester
        val firstGroupId = groups.firstOrNull()?.id
        var focusedIndex by remember(groups) { mutableStateOf(0) }
        LaunchedEffect(requestInitialFocus, firstGroupId) {
            if (requestInitialFocus && firstGroupId != null) {
                withFrameNanos { }
                runCatching { itemFocusRequester.requestFocus() }
            }
        }
        val minCell = if (maxWidth >= 700.dp) 190.dp else 145.dp
        val horizontalSpacing = 12.dp
        val columnCount = ((maxWidth.value + horizontalSpacing.value) / (minCell.value + horizontalSpacing.value))
            .toInt()
            .coerceAtLeast(1)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minCell),
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onRequestSideMenu != null &&
                        focusedIndex % columnCount == 0
                    ) {
                        onRequestSideMenu()
                        true
                    } else {
                        false
                    }
                },
            contentPadding = PaddingValues(top = 8.dp, bottom = ScreenBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(groups, key = { _, group -> group.id }) { index, group ->
                SeriesGroupCard(
                    group = group,
                    onClick = { onOpen(group) },
                    onFocused = { focusedIndex = index },
                    modifier = if (requestInitialFocus && group.id == firstGroupId) {
                        Modifier.focusRequester(itemFocusRequester)
                    } else {
                        Modifier
                    },
                )
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
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val fallbackFocusRequester = remember { FocusRequester() }
            val itemFocusRequester = initialFocusRequester ?: fallbackFocusRequester
            val firstSeasonId = seasons.firstOrNull()?.id
            var focusedIndex by remember(seasons) { mutableStateOf(0) }
            LaunchedEffect(requestInitialFocus, firstSeasonId) {
                if (requestInitialFocus && firstSeasonId != null) {
                    withFrameNanos { }
                    runCatching { itemFocusRequester.requestFocus() }
                }
            }
            val minCell = if (maxWidth >= 700.dp) 190.dp else 145.dp
            val horizontalSpacing = 12.dp
            val columnCount = ((maxWidth.value + horizontalSpacing.value) / (minCell.value + horizontalSpacing.value))
                .toInt()
                .coerceAtLeast(1)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minCell),
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionLeft &&
                            onRequestSideMenu != null &&
                            focusedIndex % columnCount == 0
                        ) {
                            onRequestSideMenu()
                            true
                        } else {
                            false
                        }
                    },
                contentPadding = PaddingValues(top = 8.dp, bottom = ScreenBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(seasons, key = { _, season -> season.id }) { index, season ->
                    SeasonCard(
                        season = season,
                        onClick = { onOpen(season) },
                        onFocused = { focusedIndex = index },
                        modifier = if (requestInitialFocus && season.id == firstSeasonId) {
                            Modifier.focusRequester(itemFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}
