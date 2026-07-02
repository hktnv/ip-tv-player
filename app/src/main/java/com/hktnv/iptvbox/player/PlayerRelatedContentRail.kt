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
import androidx.compose.ui.focus.focusProperties
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
    val optionKeys = model.options.map { it.id }
    val itemKeys = model.items.map { it.id }
    val selectedOptionIndex = model.options.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
    val optionFocusRequesters = remember(optionKeys, selectedOptionIndex, optionFocusRequester) {
        List(model.options.size) { index ->
            if (index == selectedOptionIndex) optionFocusRequester else FocusRequester()
        }
    }
    val cardFocusRequesters = remember(itemKeys, cardFocusRequester) {
        List(model.items.size) { index ->
            if (index == 0) cardFocusRequester else FocusRequester()
        }
    }
    var focusRequestNonce by remember(optionKeys, itemKeys) { mutableStateOf(0) }
    var optionFocusRequest by remember(optionKeys) { mutableStateOf<IndexedFocusRequest?>(null) }
    var cardFocusRequest by remember(itemKeys) { mutableStateOf<IndexedFocusRequest?>(null) }

    fun nextFocusRequest(index: Int): IndexedFocusRequest {
        focusRequestNonce += 1
        return IndexedFocusRequest(index = index, nonce = focusRequestNonce)
    }

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
                            focusRequesters = optionFocusRequesters,
                            cardFocusRequesters = cardFocusRequesters,
                            focusRequest = optionFocusRequest,
                            cardWidth = optionCardWidth,
                            onMoveUp = onReturnToControls,
                            onMoveDown = { sourceIndex ->
                                val targetIndex = relatedVerticalTargetIndex(sourceIndex, model.items.size)
                                if (targetIndex != null) {
                                    onRequestCardsFocus()
                                    cardFocusRequest = nextFocusRequest(targetIndex)
                                }
                            },
                            onOptionSelected = onOptionSelected,
                        )
                    }
                    if (model.items.isNotEmpty()) {
                        PlayerRelatedCardRow(
                            items = model.items,
                            focusRequesters = cardFocusRequesters,
                            optionFocusRequesters = optionFocusRequesters,
                            focusRequest = cardFocusRequest,
                            cardWidth = contentCardWidth,
                            onMoveUp = if (model.options.isEmpty()) {
                                { _: Int -> onReturnToControls() }
                            } else {
                                { sourceIndex ->
                                    val targetIndex = relatedVerticalTargetIndex(sourceIndex, model.options.size)
                                    if (targetIndex != null) {
                                        onRequestOptionsFocus()
                                        optionFocusRequest = nextFocusRequest(targetIndex)
                                    }
                                }
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

internal fun relatedVerticalTargetIndex(sourceIndex: Int, targetCount: Int): Int? {
    if (targetCount <= 0) return null
    return sourceIndex.coerceIn(0, targetCount - 1)
}

@Composable
private fun PlayerRelatedCardRow(
    items: List<CatalogItem>,
    focusRequesters: List<FocusRequester>,
    optionFocusRequesters: List<FocusRequester>,
    focusRequest: IndexedFocusRequest?,
    cardWidth: Dp,
    onMoveUp: (Int) -> Unit,
    hasMoreItems: Boolean,
    onLoadMoreItems: () -> Unit,
    onSelectItem: (CatalogItem) -> Unit,
) {
    val itemKeys = items.map { it.id }
    val listState = rememberLazyListState()
    var focusedIndex by remember(itemKeys) { mutableStateOf(0) }
    var pendingFocusIndex by remember(itemKeys) { mutableStateOf<Int?>(null) }
    LaunchedEffect(itemKeys, focusRequest) {
        val targetIndex = focusRequest?.index ?: return@LaunchedEffect
        if (targetIndex in focusRequesters.indices) {
            listState.animateScrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { focusRequesters[targetIndex].requestFocus() }
        }
    }
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
                    .focusProperties {
                        relatedVerticalTargetIndex(index, optionFocusRequesters.size)?.let { targetIndex ->
                            up = optionFocusRequesters[targetIndex]
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionUp -> {
                                relatedVerticalTargetIndex(index, optionFocusRequesters.size)?.let { targetIndex ->
                                    runCatching { optionFocusRequesters[targetIndex].requestFocus() }
                                }
                                onMoveUp(index)
                                true
                            }
                            Key.DirectionRight -> {
                                if (index >= items.lastIndex && hasMoreItems) {
                                    pendingFocusIndex = items.size
                                    onLoadMoreItems()
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }
                    .onFocusChanged {
                        if (it.isFocused) {
                            focusedIndex = index
                        }
                    },
            )
        }
    }
}

internal data class IndexedFocusRequest(
    val index: Int,
    val nonce: Int,
)

private const val RelatedRailVisibleCardCount = 6.1f
private const val RelatedRailVisibleGapCount = 6
private const val RelatedOptionVisibleCardCount = 7.2f
private const val RelatedOptionVisibleGapCount = 7
private val RelatedRailMinimumCardWidth = 110.dp
