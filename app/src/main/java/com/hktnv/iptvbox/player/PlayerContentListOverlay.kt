package com.hktnv.iptvbox.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift

@Composable
internal fun PlayerContentListOverlay(
    queue: PlayerPlaybackQueue,
    onSelectItem: (CatalogItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    val currentIndex = queue.currentIndex.coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - 3).coerceAtLeast(0),
    )
    val currentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(queue.current?.id) {
        runCatching { currentFocusRequester.requestFocus() }
    }
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier
                .width(430.dp)
                .fillMaxHeight()
                .padding(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "İçerik Listesi",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(queue.items, key = { _, item -> item.id }) { index, item ->
                        PlayerContentListItem(
                            item = item,
                            selected = index == queue.currentIndex,
                            modifier = if (index == queue.currentIndex) {
                                Modifier.focusRequester(currentFocusRequester)
                            } else {
                                Modifier
                            },
                            onClick = { onSelectItem(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerContentListItem(
    item: CatalogItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val info = remember(item.id) { item.toPlayerContentInfo() }
    val surfaceColor = when {
        focused -> TvFocusPanel
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f)
    }
    val borderColor = when {
        focused -> TvFocusBorder
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
        else -> TvRestingBorder
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusLift(focused = focused, scale = 1.015f, liftPx = -2f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = surfaceColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = info.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${info.typeLabel} · ${info.category}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
