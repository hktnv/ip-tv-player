package com.hktnv.iptvbox.ui.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.ui.catalog.category.CategoryLandingGrid
import com.hktnv.iptvbox.ui.catalog.category.categoryCards
import com.hktnv.iptvbox.ui.catalog.series.SeriesCatalogContent
import com.hktnv.iptvbox.ui.common.EmptyCatalog
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.common.LoadingPanel
import com.hktnv.iptvbox.ui.common.WarningText
import com.hktnv.iptvbox.ui.media.ContentGrid
import com.hktnv.iptvbox.ui.media.FocusedContentInfo
import com.hktnv.iptvbox.ui.media.FocusedContentInfoPanel

@Composable
internal fun CatalogScreen(
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    showCategoryLanding: Boolean,
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

    val categoryCards = remember(snapshot, selectedTab) { snapshot?.categoryCards(selectedTab).orEmpty() }
    val visibleItems = remember(snapshot, selectedTab, selectedCategory) {
        snapshot?.visibleItems(selectedTab, selectedCategory).orEmpty()
    }
    val fallbackContentFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = initialFocusRequester ?: fallbackContentFocusRequester
    var focusedContentInfo by remember { mutableStateOf<FocusedContentInfo?>(null) }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        if (playlist.warnings.isNotEmpty()) {
            WarningText(playlist.warnings.first())
        }
        when {
            snapshot == null -> LoadingPanel(
                text = stringResource(R.string.catalog_preparing),
                modifier = Modifier.padding(top = 18.dp),
            )
            catalogIndexLoading -> LoadingPanel(
                text = stringResource(R.string.catalog_preparing),
                modifier = Modifier.padding(top = 18.dp),
            )
            showCategoryLanding -> CategoryLandingGrid(
                cards = categoryCards,
                selectedCategory = selectedCategory,
                tab = selectedTab,
                onSelected = onCategorySelected,
                onRequestSideMenu = onRequestSideMenu,
                modifier = Modifier.weight(1f),
                requestInitialFocus = initialFocusRequester != null,
                initialFocusRequester = contentFocusRequester,
            )
            selectedTab == CatalogTab.SERIES -> SeriesCatalogContent(
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
                onFocusedInfoChanged = { focusedContentInfo = it },
            )
            visibleItems.isEmpty() -> EmptyState(
                title = selectedTab.emptyLabel,
                body = stringResource(R.string.catalog_empty_try_other_category),
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
            else -> ContentGrid(
                items = visibleItems,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                onRequestSideMenu = onRequestSideMenu,
                modifier = Modifier.weight(1f),
                requestInitialFocus = initialFocusRequester != null,
                initialFocusRequester = contentFocusRequester,
                onFocusedInfoChanged = { focusedContentInfo = it },
            )
        }
    }
    FocusedContentInfoPanel(
        info = focusedContentInfo,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
    }
}
