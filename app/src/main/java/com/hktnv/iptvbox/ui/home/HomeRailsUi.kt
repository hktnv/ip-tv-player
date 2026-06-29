package com.hktnv.iptvbox.ui.home
import androidx.compose.material3.MaterialTheme
import com.hktnv.iptvbox.core.designsystem.transparent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.SeriesGroup
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.media.CompactContentCard
import com.hktnv.iptvbox.ui.media.MediaCardCompactWidth
import com.hktnv.iptvbox.ui.media.SeriesGroupCard

@Composable
internal fun HomeContentRow(
    title: String,
    items: List<CatalogItem>,
    emptyText: String,
    onOpenAll: () -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    headerFocusRequester: FocusRequester,
    nextRailFocusRequester: FocusRequester?,
    onRequestSideMenu: () -> Unit,
    favoriteIds: Set<String> = emptySet(),
    cardWidth: Dp? = null,
    cardRatio: Float? = null,
) {
    var focusedIndex by remember(items) { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeRailHeader(
            title = title,
            onOpenAll = onOpenAll,
            focusRequester = headerFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        if (items.isEmpty()) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        } else {
            LazyRow(
                modifier = Modifier.homeRailHorizontalNavigation(
                    focusedIndex = focusedIndex,
                    onRequestSideMenu = onRequestSideMenu,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(items, key = { it.id }, contentType = { it.kind.name }) { item ->
                    val index = items.indexOf(item)
                    Box(
                        modifier = Modifier.homeRailItemVerticalNavigation(
                            headerFocusRequester = headerFocusRequester,
                            nextRailFocusRequester = nextRailFocusRequester,
                        ),
                    ) {
                        CompactContentCard(
                            item = item,
                            onClick = { onOpenItem(item) },
                            favorite = item.id in favoriteIds,
                            onLongClick = { onShowItemOptions(item) },
                            fixedWidth = cardWidth,
                            fixedRatio = cardRatio,
                            onFocused = { focusedIndex = index },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeSeriesRow(
    title: String,
    groups: List<SeriesGroup>,
    emptyText: String,
    onOpenAll: () -> Unit,
    onOpenSeries: (String) -> Unit,
    headerFocusRequester: FocusRequester,
    nextRailFocusRequester: FocusRequester?,
    onRequestSideMenu: () -> Unit,
) {
    var focusedIndex by remember(groups) { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeRailHeader(
            title = title,
            onOpenAll = onOpenAll,
            focusRequester = headerFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        if (groups.isEmpty()) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        } else {
            LazyRow(
                modifier = Modifier.homeRailHorizontalNavigation(
                    focusedIndex = focusedIndex,
                    onRequestSideMenu = onRequestSideMenu,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(groups, key = { it.id }, contentType = { "series-card" }) { group ->
                    val index = groups.indexOf(group)
                    Box(
                        modifier = Modifier.homeRailItemVerticalNavigation(
                            headerFocusRequester = headerFocusRequester,
                            nextRailFocusRequester = nextRailFocusRequester,
                        ),
                    ) {
                        SeriesGroupCard(
                            group = group,
                            onClick = { onOpenSeries(group.title) },
                            modifier = Modifier.width(MediaCardCompactWidth),
                            onFocused = { focusedIndex = index },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeRailHeader(
    title: String,
    onOpenAll: () -> Unit,
    focusRequester: FocusRequester,
    onRequestSideMenu: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft &&
                    onRequestSideMenu().let { true }
            }
            .tvFocusLift(focused = focused, scale = 1.01f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onOpenAll),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(2.dp, TvFocusBorder) else null,
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SeeAllLabel()
        }
    }
}

@Composable
internal fun SeeAllLabel() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, TvRestingBorder),
    ) {
        Text(
            text = "Tümü",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1,
        )
    }
}

private fun Modifier.homeRailItemVerticalNavigation(
    headerFocusRequester: FocusRequester,
    nextRailFocusRequester: FocusRequester?,
): Modifier {
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) {
            return@onPreviewKeyEvent false
        }
        when (event.key) {
            Key.DirectionDown -> nextRailFocusRequester.requestFocusSafely()
            Key.DirectionUp -> headerFocusRequester.requestFocusSafely()
            else -> false
        }
    }
}

private fun Modifier.homeRailHorizontalNavigation(
    focusedIndex: Int,
    onRequestSideMenu: () -> Unit,
): Modifier {
    return onPreviewKeyEvent { event ->
        if (
            event.type == KeyEventType.KeyDown &&
            event.key == Key.DirectionLeft &&
            focusedIndex <= 0
        ) {
            onRequestSideMenu()
            true
        } else {
            false
        }
    }
}

private fun FocusRequester?.requestFocusSafely(): Boolean {
    if (this == null) return false
    return runCatching { requestFocus() }.isSuccess
}
