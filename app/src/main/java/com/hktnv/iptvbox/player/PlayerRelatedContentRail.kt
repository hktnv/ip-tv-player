package com.hktnv.iptvbox.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.designsystem.mediaCardSpacing
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.ui.media.CompactContentCard
import com.hktnv.iptvbox.ui.media.CompactContentCardChrome

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
        PlayerRelatedHandle(
            expanded = expanded,
            onClick = if (expanded) onReturnToControls else onExpand,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 120)) +
                expandVertically(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 90)) +
                shrinkVertically(animationSpec = tween(durationMillis = 140)),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val optionCardWidth = relatedRailOptionCardWidth(maxWidth)
                val contentCardWidth = relatedRailCardWidth(maxWidth)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (model.options.isNotEmpty()) {
                        PlayerRelatedOptionRow(
                            options = model.options,
                            optionFocusRequester = optionFocusRequester,
                            cardWidth = optionCardWidth,
                            onMoveUp = onReturnToControls,
                            onMoveDown = {
                                if (model.items.isNotEmpty()) onRequestCardsFocus()
                            },
                            onOptionSelected = onOptionSelected,
                        )
                    }
                    if (model.items.isNotEmpty()) {
                        PlayerRelatedCardRow(
                            items = model.items,
                            cardFocusRequester = cardFocusRequester,
                            cardWidth = contentCardWidth,
                            onMoveUp = if (model.options.isEmpty()) {
                                onReturnToControls
                            } else {
                                onRequestOptionsFocus
                            },
                            hasMoreItems = model.hasMoreItems,
                            onLoadMoreItems = onLoadMoreItems,
                            onSelectItem = onSelectItem,
                        )
                    }
                }
            }
        }
    }
}

internal fun relatedRailCardWidth(maxWidth: Dp): Dp {
    val availableWidth = (maxWidth - mediaCardSpacing * RelatedRailVisibleGapCount)
        .coerceAtLeast(RelatedRailMinimumCardWidth)
    return (availableWidth / RelatedRailVisibleCardCount).coerceAtLeast(RelatedRailMinimumCardWidth)
}

internal fun relatedRailOptionCardWidth(maxWidth: Dp): Dp {
    val availableWidth = (maxWidth - mediaCardSpacing * RelatedOptionVisibleGapCount)
        .coerceAtLeast(RelatedRailMinimumCardWidth)
    return (availableWidth / RelatedOptionVisibleCardCount).coerceAtLeast(RelatedRailMinimumCardWidth)
}

@Composable
private fun PlayerRelatedCardRow(
    items: List<CatalogItem>,
    cardFocusRequester: FocusRequester,
    cardWidth: Dp,
    onMoveUp: () -> Unit,
    hasMoreItems: Boolean,
    onLoadMoreItems: () -> Unit,
    onSelectItem: (CatalogItem) -> Unit,
) {
    val itemKeys = items.map { it.id }
    val listState = rememberLazyListState()
    val focusRequesters = remember(itemKeys, cardFocusRequester) {
        List(items.size) { index ->
            if (index == 0) cardFocusRequester else FocusRequester()
        }
    }
    var focusedIndex by remember(itemKeys) { mutableStateOf(0) }
    var pendingFocusIndex by remember(itemKeys) { mutableStateOf<Int?>(null) }
    LaunchedEffect(itemKeys, pendingFocusIndex) {
        val targetIndex = pendingFocusIndex ?: return@LaunchedEffect
        if (targetIndex in focusRequesters.indices) {
            listState.animateScrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { focusRequesters[targetIndex].requestFocus() }
            pendingFocusIndex = null
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        onMoveUp()
                        true
                    }
                    Key.DirectionRight -> {
                        if (focusedIndex >= items.lastIndex && hasMoreItems) {
                            pendingFocusIndex = items.size
                            onLoadMoreItems()
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            },
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(mediaCardSpacing),
    ) {
        itemsIndexed(items = items, key = { _, item -> item.id }) { index, item ->
            CompactContentCard(
                item = item,
                fixedWidth = cardWidth,
                chrome = CompactContentCardChrome.TranslucentOsd,
                onClick = { onSelectItem(item) },
                modifier = Modifier
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged {
                        if (it.isFocused) focusedIndex = index
                    },
            )
        }
    }
}

private const val RelatedRailVisibleCardCount = 6.1f
private const val RelatedRailVisibleGapCount = 6
private const val RelatedOptionVisibleCardCount = 7.2f
private const val RelatedOptionVisibleGapCount = 7
private val RelatedRailMinimumCardWidth = 110.dp
