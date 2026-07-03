package com.hktnv.iptvbox.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.ui.common.EmptyCatalog
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.media.ContentGrid
import com.hktnv.iptvbox.ui.media.itemsByIds

@Composable
internal fun SavedItemsScreen(
    title: String,
    emptyText: String,
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    itemIds: List<String>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    if (playlist == null) {
        EmptyCatalog(onAddPlaylist, contentPadding)
        return
    }
    val idSignature = itemIds.joinToString("|")
    val items = remember(snapshot, idSignature) { snapshot?.itemsByIds(itemIds).orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        if (snapshot == null || catalogIndexLoading) {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                modifier = Modifier.weight(1f),
                requestInitialFocus = initialFocusRequester != null && items.isNotEmpty(),
                initialFocusRequester = initialFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
                placeholderCount = (12 - items.size).coerceAtLeast(0),
            )
        } else if (items.isEmpty()) {
            EmptyState(
                title = emptyText,
                body = stringResource(R.string.empty_saved_body),
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                modifier = Modifier.weight(1f),
                requestInitialFocus = initialFocusRequester != null,
                initialFocusRequester = initialFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
            )
        }
    }
}

@Composable
internal fun LatestItemsScreen(
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    if (playlist == null) {
        EmptyCatalog(onAddPlaylist, contentPadding)
        return
    }
    val items = remember(snapshot) {
        snapshot?.allItems
            .orEmpty()
            .toList()
            .asReversed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        if (snapshot == null || catalogIndexLoading) {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                modifier = Modifier.weight(1f),
                requestInitialFocus = initialFocusRequester != null && items.isNotEmpty(),
                initialFocusRequester = initialFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
                placeholderCount = (12 - items.size).coerceAtLeast(0),
            )
        } else if (items.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_latest_title),
                body = stringResource(R.string.empty_latest_body),
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                modifier = Modifier.weight(1f),
                requestInitialFocus = true,
                initialFocusRequester = initialFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
            )
        }
    }
}

