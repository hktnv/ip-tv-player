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
    initialFocusRequester: FocusRequester,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSelectItem: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!model.hasContent) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (
                    expanded &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionUp
                ) {
                    onCollapse()
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (!expanded) {
            PlayerRelatedHandle(onExpand = onExpand)
        } else {
            if (model.options.isNotEmpty()) {
                PlayerRelatedOptionRow(
                    options = model.options,
                    initialFocusRequester = initialFocusRequester,
                    onOptionSelected = onOptionSelected,
                )
            }
            PlayerRelatedCardRow(
                items = model.items,
                initialFocusRequester = if (model.options.isEmpty()) initialFocusRequester else null,
                onSelectItem = onSelectItem,
            )
        }
    }
}

@Composable
private fun PlayerRelatedHandle(
    onExpand: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onExpand() })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -6f) onExpand()
                }
            }
            .tvFocusLift(focused = focused, scale = 1.012f, liftPx = -2f)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .tvClickable(onClick = onExpand),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (focused) 0.44f else 0.28f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.focusBorder else TvRestingBorder.copy(alpha = 0.62f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.accentText,
            )
            Text(
                text = stringResource(R.string.player_related_handle),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
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
    initialFocusRequester: FocusRequester,
    onOptionSelected: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(items = options, key = { it.id }) { option ->
            PlayerRelatedOptionChip(
                option = option,
                modifier = if (option == options.first()) {
                    Modifier.focusRequester(initialFocusRequester)
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
            option.selected -> MaterialTheme.colorScheme.accentSubtle.copy(alpha = 0.64f)
            focused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
        },
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.focusBorder else TvRestingBorder.copy(alpha = 0.58f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            color = if (option.selected) {
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
    initialFocusRequester: FocusRequester?,
    onSelectItem: (CatalogItem) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = when {
            maxWidth < 600.dp -> 132.dp
            maxWidth < 900.dp -> 148.dp
            else -> 166.dp
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(items = items, key = { it.id }) { item ->
                PlayerRelatedContentCard(
                    item = item,
                    width = cardWidth,
                    modifier = if (item == items.first() && initialFocusRequester != null) {
                        Modifier.focusRequester(initialFocusRequester)
                    } else {
                        Modifier
                    },
                    onClick = { onSelectItem(item) },
                )
            }
        }
    }
}
