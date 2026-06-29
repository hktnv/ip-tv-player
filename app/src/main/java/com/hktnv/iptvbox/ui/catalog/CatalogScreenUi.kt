package com.hktnv.iptvbox.ui.catalog
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistStats
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.ui.common.EmptyCatalog
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.common.LoadingPanel
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.TvSelectedPanel
import com.hktnv.iptvbox.ui.common.WarningText
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.media.ContentGrid

@Composable
internal fun CatalogScreen(
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    favoriteIds: List<String>,
    onTabSelected: (CatalogTab) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    onRequestSideMenu: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
) {
    if (playlist == null) {
        EmptyCatalog(onAddPlaylist, contentPadding)
        return
    }

    val categories = remember(snapshot, selectedTab) { snapshot?.categories(selectedTab).orEmpty() }
    val visibleItems = remember(snapshot, selectedTab, selectedCategory) {
        snapshot?.visibleItems(selectedTab, selectedCategory).orEmpty()
    }
    val fallbackContentFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = initialFocusRequester ?: fallbackContentFocusRequester

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        CategoryStrip(
            categories = categories,
            selected = selectedCategory,
            onSelected = onCategorySelected,
            contentFocusRequester = contentFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (playlist.warnings.isNotEmpty()) {
            WarningText(playlist.warnings.first())
        }
        if (snapshot == null || catalogIndexLoading) {
            LoadingPanel(
                text = "Katalog hazÄ±rlanÄ±yor",
                modifier = Modifier.padding(top = 18.dp),
            )
        } else if (selectedTab == CatalogTab.SERIES) {
            SeriesCatalogContent(
                snapshot = snapshot,
                selectedCategory = selectedCategory,
                selectedSeriesTitle = selectedSeriesTitle,
                selectedSeasonNumber = selectedSeasonNumber,
                favoriteIds = favoriteIds,
                onSeriesSelected = onSeriesSelected,
                onSeasonSelected = onSeasonSelected,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                modifier = Modifier.weight(1f),
                requestInitialFocus = initialFocusRequester != null,
                initialFocusRequester = contentFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
            )
        } else if (visibleItems.isEmpty()) {
            EmptyState(
                title = selectedTab.emptyLabel,
                body = "BaÅŸka kategori seÃ§ veya farklÄ± bir oynatma listesi yÃ¼kle.",
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = visibleItems,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                onRequestSideMenu = onRequestSideMenu,
                modifier = Modifier.weight(1f),
                requestInitialFocus = initialFocusRequester != null,
                initialFocusRequester = contentFocusRequester,
            )
        }
    }
}
@Composable
internal fun ContentTabs(
    stats: PlaylistStats,
    selectedTab: CatalogTab,
    onSelected: (CatalogTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CatalogTab.entries.forEach { tab ->
            MainSectionButton(
                label = tab.label,
                count = stats.count(tab),
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun MainSectionButton(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .height(50.dp)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.02f, liftPx = -4f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> TvSelectedPanel
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            when {
                focused -> TvFocusBorder
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> TvRestingBorder
            },
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = count.toString(),
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LegacyCategoryStrip(
    categories: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
    contentFocusRequester: FocusRequester,
    onRequestSideMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowState = rememberLazyListState()
    val selectedIndex = if (selected == null) 0 else categories.indexOf(selected).takeIf { it >= 0 }?.plus(1) ?: 0
    var focusedIndex by remember(categories, selected) { mutableStateOf(selectedIndex) }
    LaunchedEffect(categories, selected) {
        rowState.scrollToItem(selectedIndex)
    }
    LazyRow(
        modifier = modifier
            .padding(bottom = 6.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (focusedIndex <= 0) {
                            onRequestSideMenu()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> runCatching { contentFocusRequester.requestFocus() }.getOrDefault(false)
                    else -> false
                }
            },
        state = rowState,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        item {
            CategoryButton(
                label = "TÃ¼m kategoriler",
                selected = selected == null,
                onClick = { onSelected(null) },
                onFocused = { focusedIndex = 0 },
            )
        }
        items(categories, key = { it }) { category ->
            val categoryIndex = categories.indexOf(category) + 1
            CategoryButton(
                label = category,
                selected = selected == category,
                onClick = { onSelected(category) },
                onFocused = { focusedIndex = categoryIndex },
            )
        }
    }
}
