package com.evomrdm.iptvbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.evomrdm.iptvbox.core.designsystem.IptvColors

@Composable
internal fun CategoryStrip(
    categories: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
    contentFocusRequester: FocusRequester,
    onRequestSideMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowState = rememberLazyListState()
    val selectedIndex = if (selected == null) 0 else categories.indexOf(selected).takeIf { it >= 0 }?.plus(1) ?: 0
    var focusedIndex by remember(categories, selected) { mutableStateOf(selectedIndex) }
    LaunchedEffect(categories, selected) {
        rowState.scrollToItem(selectedIndex)
    }
    LazyRow(
        modifier = modifier
            .padding(bottom = 6.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (focusedIndex <= 0) {
                            onRequestSideMenu()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> runCatching { contentFocusRequester.requestFocus() }.getOrDefault(false)
                    else -> false
                }
            },
        state = rowState,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        item {
            CategoryButton(
                label = "Tüm kategoriler",
                selected = selected == null,
                onClick = { onSelected(null) },
                onFocused = { focusedIndex = 0 },
            )
        }
        items(categories, key = { it }) { category ->
            val categoryIndex = categories.indexOf(category) + 1
            CategoryButton(
                label = category,
                selected = selected == category,
                onClick = { onSelected(category) },
                onFocused = { focusedIndex = categoryIndex },
            )
        }
    }
}

@Composable
internal fun CategoryButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .height(34.dp)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> IptvColors.Accent.copy(alpha = 0.12f)
            else -> Color(0xFF101720)
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            when {
                focused -> TvFocusBorder
                selected -> TvRestingBorder.copy(alpha = 0.35f)
                else -> TvRestingBorder
            },
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 10.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) IptvColors.TextPrimary else IptvColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
