package com.hktnv.iptvbox.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.designsystem.mediaCardSpacing
import com.hktnv.iptvbox.model.ScreenBottomPadding

internal const val HorizontalMediaGridColumnCount = 2

@Composable
internal fun <T> HorizontalMediaCardGrid(
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester? = null,
    contentType: (T) -> Any? = { null },
    itemContent: @Composable (item: T, requestSideMenuOnLeft: Boolean, modifier: Modifier) -> Unit,
) {
    val firstItemKey = items.firstOrNull()?.let(itemKey)
    LazyVerticalGrid(
        columns = GridCells.Fixed(HorizontalMediaGridColumnCount),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(mediaCardSpacing),
        horizontalArrangement = Arrangement.spacedBy(mediaCardSpacing),
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> itemKey(item) },
            contentType = { _, item -> contentType(item) },
        ) { index, item ->
            itemContent(
                item,
                shouldOpenDrawerFromHorizontalMediaGrid(index),
                if (initialFocusRequester != null && itemKey(item) == firstItemKey) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

internal fun shouldOpenDrawerFromHorizontalMediaGrid(
    index: Int,
    columnCount: Int = HorizontalMediaGridColumnCount,
): Boolean {
    return columnCount > 0 && index >= 0 && index % columnCount == 0
}
