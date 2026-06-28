package com.hktnv.iptvbox.ui.media
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.model.CatalogItem
import kotlinx.coroutines.delay
import com.hktnv.iptvbox.model.ScreenBottomPadding

@Composable
internal fun ContentGrid(
    items: List<CatalogItem>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fallbackFocusRequester = remember { FocusRequester() }
        val itemFocusRequester = initialFocusRequester ?: fallbackFocusRequester
        var focusedIndex by remember(items) { mutableStateOf(0) }
        val firstItemId = items.firstOrNull()?.id
        LaunchedEffect(requestInitialFocus, firstItemId) {
            if (requestInitialFocus && firstItemId != null) {
                repeat(4) { attempt ->
                    withFrameNanos { }
                    if (attempt > 0) delay(80L)
                    val focused = runCatching { itemFocusRequester.requestFocus() }.getOrDefault(false)
                    if (focused) return@LaunchedEffect
                }
            }
        }
        val favoriteKey = favoriteIds.joinToString("|")
        val favoriteSet = remember(favoriteKey) { favoriteIds.toSet() }
        val minCell = when {
            maxWidth < 600.dp -> 160.dp
            maxWidth >= 900.dp -> 178.dp
            maxWidth >= 600.dp -> 158.dp
            else -> 145.dp
        }
        val horizontalSpacing = 10.dp
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }, contentType = { _, item -> item.kind.name }) { index, item ->
                ContentCard(
                    item = item,
                    favorite = item.id in favoriteSet,
                    onOpen = { onOpenItem(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onFocused = { focusedIndex = index },
                    modifier = if (requestInitialFocus && item.id == firstItemId) {
                        Modifier.focusRequester(itemFocusRequester)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}
