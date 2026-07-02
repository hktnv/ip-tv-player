package com.hktnv.iptvbox.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.accentSubtle
import com.hktnv.iptvbox.core.designsystem.accentText
import com.hktnv.iptvbox.core.designsystem.focusBorder
import com.hktnv.iptvbox.core.designsystem.mediaCardRadius
import com.hktnv.iptvbox.core.designsystem.mediaCardSpacing
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusLift

@Composable
internal fun PlayerRelatedHandle(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (!expanded && dragAmount < -6f) onClick()
                    if (expanded && dragAmount > 6f) onClick()
                }
            }
            .tvFocusLift(focused = focused, scale = 1.008f, liftPx = -1f)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = !expanded)
            .tvClickable(enabled = !expanded, onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (focused) 0.34f else 0.18f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.focusBorder else TvRestingBorder.copy(alpha = 0.38f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = if (expanded) 3.dp else 5.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.accentText.copy(alpha = if (focused) 1f else 0.82f),
            )
            Text(
                text = stringResource(R.string.player_related_handle),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (expanded) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PlayerRelatedOptionRow(
    options: List<PlayerRelatedContentOption>,
    optionFocusRequester: FocusRequester,
    cardWidth: Dp,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOptionSelected: (String) -> Unit,
) {
    val optionKeys = options.map { it.id }
    val selectedIndex = options.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
    val focusRequesters = remember(optionKeys, selectedIndex, optionFocusRequester) {
        List(options.size) { index ->
            if (index == selectedIndex) optionFocusRequester else FocusRequester()
        }
    }
    val listState = rememberLazyListState()
    var focusedIndex by remember(optionKeys, selectedIndex) { mutableStateOf(selectedIndex) }
    LaunchedEffect(optionKeys, selectedIndex) {
        focusedIndex = selectedIndex
        listState.animateScrollToItem(selectedIndex)
        withFrameNanos { }
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
                    Key.DirectionDown -> {
                        onMoveDown()
                        true
                    }
                    else -> false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(mediaCardSpacing),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        itemsIndexed(items = options, key = { _, option -> option.id }) { index, option ->
            PlayerRelatedOptionChip(
                option = option,
                width = cardWidth,
                modifier = Modifier
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { if (it.isFocused) focusedIndex = index },
                onClick = { onOptionSelected(option.id) },
            )
        }
    }
}

@Composable
private fun PlayerRelatedOptionChip(
    option: PlayerRelatedContentOption,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val label = option.seasonNumber?.let {
        stringResource(R.string.player_related_season_format, it)
    } ?: option.label.ifBlank {
        stringResource(R.string.player_related_other_category)
    }
    Surface(
        modifier = modifier
            .width(width)
            .height(RelatedOptionCardHeight)
            .tvFocusLift(focused = focused, scale = 1.018f, liftPx = -2f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = when {
            focused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f)
            option.selected -> MaterialTheme.colorScheme.accentSubtle.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.36f)
        },
        shape = RoundedCornerShape(mediaCardRadius),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.focusBorder else TvRestingBorder.copy(alpha = 0.42f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (option.selected) 4.dp else 0.dp)
                    .background(MaterialTheme.colorScheme.accentText),
            )
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (option.selected) 12.dp else 16.dp),
                color = if (focused || option.selected) {
                    MaterialTheme.colorScheme.accentText
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (option.selected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.accentText,
                            shape = RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}

private val RelatedOptionCardHeight = 42.dp
