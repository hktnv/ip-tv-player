package com.hktnv.iptvbox.ui.media
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.hktnv.iptvbox.core.model.CatalogItem

@Composable
internal fun ContentGrid(
    items: List<CatalogItem>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
    onFocusedInfoChanged: (FocusedContentInfo?) -> Unit = {},
    placeholderCount: Int = 0,
) {
    val favoriteKey = favoriteIds.joinToString("|")
    val favoriteSet = remember(favoriteKey) { favoriteIds.toSet() }
    MediaCardGrid(
        items = items,
        itemKey = { it.id },
        modifier = modifier,
        requestInitialFocus = requestInitialFocus,
        initialFocusRequester = initialFocusRequester,
        onRequestSideMenu = onRequestSideMenu,
        contentType = { it.kind.name },
        placeholderCount = placeholderCount,
        placeholderContent = { itemModifier ->
            ContentCardPlaceholder(modifier = itemModifier)
        },
    ) { item, itemModifier, onFocused, onFocusChanged ->
        ContentCard(
            item = item,
            favorite = item.id in favoriteSet,
            onOpen = { onOpenItem(item) },
            onLongClick = { onShowItemOptions(item) },
            onFocused = {
                onFocused()
                onFocusedInfoChanged(item.focusedContentInfo())
            },
            onFocusChanged = { focused ->
                onFocusChanged(focused)
                onFocusedInfoChanged(if (focused) item.focusedContentInfo() else null)
            },
            modifier = itemModifier,
        )
    }
}
