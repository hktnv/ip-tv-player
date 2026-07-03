package com.hktnv.iptvbox.ui.media

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.designsystem.mediaCardSpacing
import com.hktnv.iptvbox.model.ScreenBottomPadding
import kotlinx.coroutines.delay

@Composable
internal fun <T> MediaCardGrid(
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
    contentType: (T) -> Any? = { null },
    placeholderCount: Int = 0,
    placeholderContent: (@Composable (Modifier) -> Unit)? = null,
    itemContent: @Composable (
        item: T,
        modifier: Modifier,
        onFocused: () -> Unit,
        onFocusChanged: (Boolean) -> Unit,
    ) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val configuration = LocalConfiguration.current
        val television = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
        val fallbackFocusRequester = remember { FocusRequester() }
        val itemFocusRequester = initialFocusRequester ?: fallbackFocusRequester
        val firstItemKey = items.firstOrNull()?.let(itemKey)
        var focusedIndex by remember(items) { mutableStateOf(0) }

        LaunchedEffect(requestInitialFocus, firstItemKey) {
            if (requestInitialFocus && firstItemKey != null) {
                repeat(4) { attempt ->
                    withFrameNanos { }
                    if (attempt > 0) delay(80L)
                    val focused = runCatching { itemFocusRequester.requestFocus() }.getOrDefault(false)
                    if (focused) return@LaunchedEffect
                }
            }
        }

        val grid = mediaGridMetrics(maxWidth = maxWidth, television = television)
        LazyVerticalGrid(
            columns = grid.cells,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        onRequestSideMenu != null &&
                        focusedIndex % grid.columnCount == 0
                    ) {
                        onRequestSideMenu()
                        true
                    } else {
                        false
                    }
                },
            contentPadding = PaddingValues(top = 8.dp, bottom = ScreenBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(grid.spacing),
            verticalArrangement = Arrangement.spacedBy(grid.spacing),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> itemKey(item) },
                contentType = { _, item -> contentType(item) },
            ) { index, item ->
                val itemModifier = if (requestInitialFocus && itemKey(item) == firstItemKey) {
                        Modifier.focusRequester(itemFocusRequester)
                    } else {
                        Modifier
                    }
                itemContent(
                    item,
                    itemModifier,
                    { focusedIndex = index },
                    { focused ->
                        if (focused) focusedIndex = index
                    },
                )
            }
            if (placeholderCount > 0 && placeholderContent != null) {
                items(
                    count = placeholderCount,
                    key = { "media-card-placeholder-$it" },
                    contentType = { "media-card-placeholder" },
                ) {
                    placeholderContent(Modifier)
                }
            }
        }
    }
}

private data class MediaGridMetrics(
    val cells: GridCells,
    val columnCount: Int,
    val spacing: Dp,
)

private fun mediaGridMetrics(maxWidth: Dp, television: Boolean): MediaGridMetrics {
    val spacing = mediaCardSpacing
    val minCell = when {
        maxWidth >= 900.dp -> 112.dp
        maxWidth >= 600.dp -> 124.dp
        else -> 146.dp
    }
    val wideTvGrid = television && maxWidth >= 700.dp
    val columnCount = if (wideTvGrid) {
        6
    } else {
        ((maxWidth.value + spacing.value) / (minCell.value + spacing.value))
            .toInt()
            .coerceAtLeast(1)
    }
    return MediaGridMetrics(
        cells = if (wideTvGrid) GridCells.Fixed(columnCount) else GridCells.Adaptive(minCell),
        columnCount = columnCount,
        spacing = spacing,
    )
}
