package com.hktnv.iptvbox.ui.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.ui.common.EmptyCatalog
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.common.LoadingPanel
import com.hktnv.iptvbox.ui.common.WarningText
import com.hktnv.iptvbox.ui.media.ContentGrid

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        if (playlist.warnings.isNotEmpty()) {
            WarningText(playlist.warnings.first())
        }
        when {
            snapshot == null || catalogIndexLoading -> LoadingPanel(
                text = "Katalog hazırlanıyor",
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
            )
            visibleItems.isEmpty() -> EmptyState(
                title = selectedTab.emptyLabel,
                body = "Başka kategori seç veya farklı bir oynatma listesi yükle.",
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
            )
        }
    }
}
