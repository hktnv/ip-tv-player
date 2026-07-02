package com.hktnv.iptvbox.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.model.CatalogItem
import kotlinx.coroutines.launch

@Composable
internal fun PlayerRelatedContentRail(
    model: PlayerRelatedContentModel,
    expanded: Boolean,
    optionFocusRequester: FocusRequester,
    cardFocusRequester: FocusRequester,
    onExpand: () -> Unit,
    onReturnToControls: () -> Unit,
    onRequestOptionsFocus: () -> Unit,
    onRequestCardsFocus: () -> Unit,
    onLoadMoreItems: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSelectItem: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!model.hasContent) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (expanded) 6.dp else 0.dp),
    ) {
        if (!expanded) {
            PlayerRelatedHandle(expanded = false, onClick = onExpand)
        } else {
            PlayerRelatedHandle(expanded = true, onClick = onReturnToControls)
            if (model.options.isNotEmpty()) {
                PlayerRelatedOptionRow(
                    options = model.options,
                    optionFocusRequester = optionFocusRequester,
                    onMoveUp = onReturnToControls,
                    onMoveDown = onRequestCardsFocus,
                    onOptionSelected = onOptionSelected,
                )
            }
            PlayerRelatedCardRow(
                items = model.items,
                cardFocusRequester = cardFocusRequester,
                onMoveUp = if (model.options.isEmpty()) onReturnToControls else onRequestOptionsFocus,
                hasMoreItems = model.hasMoreItems,
                onLoadMoreItems = onLoadMoreItems,
                onSelectItem = onSelectItem,
            )
        }
    }
}

@Composable
private fun PlayerRelatedCardRow(
    items: List<CatalogItem>,
    cardFocusRequester: FocusRequester,
    onMoveUp: () -> Unit,
    hasMoreItems: Boolean,
    onLoadMoreItems: () -> Unit,
    onSelectItem: (CatalogItem) -> Unit,
) {
    val itemKeys = items.map { it.id }
    val focusRequesters = remember(itemKeys) { List(items.size) { FocusRequester() } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var focusedIndex by remember(itemKeys) { mutableStateOf(0) }
    var pendingFocusIndex by remember { mutableStateOf<Int?>(null) }
    fun moveFocusTo(index: Int) {
        val targetIndex = index.coerceIn(items.indices)
        focusedIndex = targetIndex
        scope.launch {
            listState.scrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { focusRequesters[targetIndex].requestFocus() }
        }
    }
    LaunchedEffect(items.size, pendingFocusIndex) {
        val targetIndex = pendingFocusIndex ?: return@LaunchedEffect
        if (targetIndex in focusRequesters.indices) {
            focusedIndex = targetIndex
            listState.scrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { focusRequesters[targetIndex].requestFocus() }
            pendingFocusIndex = null
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = when {
            maxWidth < 600.dp -> 122.dp
            maxWidth < 900.dp -> 138.dp
            else -> 154.dp
        }
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (focusedIndex > 0) {
                                moveFocusTo(focusedIndex - 1)
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (focusedIndex < items.lastIndex) {
                                moveFocusTo(focusedIndex + 1)
                            } else if (hasMoreItems) {
                                pendingFocusIndex = items.size
                                onLoadMoreItems()
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            onMoveUp()
                            true
                        }
                        else -> false
                    }
                },
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            itemsIndexed(items = items, key = { _, item -> item.id }) { index, item ->
                PlayerRelatedContentCard(
                    item = item,
                    width = cardWidth,
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .then(
                            if (index == 0) {
                                Modifier.focusRequester(cardFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged { if (it.isFocused) focusedIndex = index },
                    onDirectionalKey = { key ->
                        when (key) {
                            Key.DirectionLeft -> {
                                if (index > 0) {
                                    moveFocusTo(index - 1)
                                }
                                true
                            }
                            Key.DirectionRight -> {
                                if (index < items.lastIndex) {
                                    moveFocusTo(index + 1)
                                } else if (hasMoreItems) {
                                    pendingFocusIndex = items.size
                                    onLoadMoreItems()
                                }
                                true
                            }
                            Key.DirectionUp -> {
                                onMoveUp()
                                true
                            }
                            else -> false
                        }
                    },
                    onClick = { onSelectItem(item) },
                )
            }
        }
    }
}
