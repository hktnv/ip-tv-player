package com.hktnv.iptvbox.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.accentSubtle
import com.hktnv.iptvbox.core.designsystem.accentText
import com.hktnv.iptvbox.core.designsystem.focusBorder
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusLift

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
                onSelectItem = onSelectItem,
            )
        }
    }
}

@Composable
private fun PlayerRelatedHandle(
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
private fun PlayerRelatedOptionRow(
    options: List<PlayerRelatedContentOption>,
    optionFocusRequester: FocusRequester,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOptionSelected: (String) -> Unit,
) {
    LazyRow(
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
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(items = options, key = { it.id }) { option ->
            PlayerRelatedOptionChip(
                option = option,
                modifier = if (option.selected) {
                    Modifier.focusRequester(optionFocusRequester)
                } else {
                    Modifier
                },
                onClick = { onOptionSelected(option.id) },
            )
        }
    }
}

@Composable
private fun PlayerRelatedOptionChip(
    option: PlayerRelatedContentOption,
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
            .tvFocusLift(focused = focused, scale = 1.018f, liftPx = -2f)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .tvClickable(onClick = onClick),
        color = when {
            focused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f)
            option.selected -> MaterialTheme.colorScheme.accentSubtle.copy(alpha = 0.48f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        },
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.focusBorder else TvRestingBorder.copy(alpha = 0.58f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (focused || option.selected) {
                MaterialTheme.colorScheme.accentText
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlayerRelatedCardRow(
    items: List<CatalogItem>,
    cardFocusRequester: FocusRequester,
    onMoveUp: () -> Unit,
    onSelectItem: (CatalogItem) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = when {
            maxWidth < 600.dp -> 122.dp
            maxWidth < 900.dp -> 138.dp
            else -> 154.dp
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                        onMoveUp()
                        true
                    } else {
                        false
                    }
                },
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(items = items, key = { it.id }) { item ->
                PlayerRelatedContentCard(
                    item = item,
                    width = cardWidth,
                    modifier = if (item == items.first()) {
                        Modifier.focusRequester(cardFocusRequester)
                    } else {
                        Modifier
                    },
                    onClick = { onSelectItem(item) },
                )
            }
        }
    }
}
