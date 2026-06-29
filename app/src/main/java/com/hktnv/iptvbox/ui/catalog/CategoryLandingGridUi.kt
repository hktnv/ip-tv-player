package com.hktnv.iptvbox.ui.catalog

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.designsystem.mediaCardSpacing
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.TvSelectedPanel
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import kotlinx.coroutines.delay

internal data class CategoryCardState(
    val category: String?,
    val title: String,
    val count: Int,
)

internal fun CatalogSnapshot.categoryCards(tab: CatalogTab): List<CategoryCardState> {
    return buildList {
        add(CategoryCardState(category = null, title = "Tüm kategoriler", count = categoryLandingTotal(tab)))
        categories(tab).forEach { category ->
            val count = categoryCount(tab, category)
            if (count > 0) {
                add(CategoryCardState(category = category, title = category, count = count))
            }
        }
    }
}

private fun CatalogSnapshot.categoryLandingTotal(tab: CatalogTab): Int {
    if (tab != CatalogTab.SERIES) return categoryCount(tab, null)
    return categoryCountsByTab[tab]?.values?.sum()?.takeIf { it > 0 } ?: categoryCount(tab, null)
}

@Composable
internal fun CategoryLandingGrid(
    cards: List<CategoryCardState>,
    selectedCategory: String?,
    tab: CatalogTab,
    onSelected: (String?) -> Unit,
    onRequestSideMenu: () -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val configuration = LocalConfiguration.current
        val television = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
        val metrics = categoryGridMetrics(maxWidth = maxWidth, television = television)
        val fallbackFocusRequester = remember { FocusRequester() }
        val focusRequester = initialFocusRequester ?: fallbackFocusRequester
        val selectedIndex = cards.indexOfFirst { it.category == selectedCategory }.takeIf { it >= 0 } ?: 0
        var focusedIndex by remember(cards, selectedCategory) { mutableStateOf(selectedIndex) }

        LaunchedEffect(requestInitialFocus, cards, selectedCategory) {
            if (requestInitialFocus && cards.isNotEmpty()) {
                repeat(4) { attempt ->
                    withFrameNanos { }
                    if (attempt > 0) delay(80L)
                    if (runCatching { focusRequester.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(metrics.columnCount),
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionLeft &&
                        focusedIndex % metrics.columnCount == 0
                    ) {
                        onRequestSideMenu()
                        true
                    } else {
                        false
                    }
                },
            contentPadding = PaddingValues(top = 14.dp, bottom = ScreenBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(mediaCardSpacing),
            verticalArrangement = Arrangement.spacedBy(mediaCardSpacing),
        ) {
            itemsIndexed(cards, key = { _, card -> card.category ?: "__all_categories__" }) { index, card ->
                CategoryLandingCard(
                    card = card,
                    countLabel = card.count.categoryCountLabel(tab),
                    selected = card.category == selectedCategory,
                    onClick = { onSelected(card.category) },
                    onFocused = { focusedIndex = index },
                    modifier = if (requestInitialFocus && index == selectedIndex) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }.height(metrics.cardHeight),
                )
            }
        }
    }
}

@Composable
private fun CategoryLandingCard(
    card: CategoryCardState,
    countLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -5f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> TvSelectedPanel
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 14.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = card.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = countLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private data class CategoryGridMetrics(
    val columnCount: Int,
    val cardHeight: Dp,
)

private fun categoryGridMetrics(maxWidth: Dp, television: Boolean): CategoryGridMetrics {
    return when {
        television && maxWidth >= 700.dp -> CategoryGridMetrics(columnCount = 4, cardHeight = 104.dp)
        maxWidth >= 900.dp -> CategoryGridMetrics(columnCount = 4, cardHeight = 104.dp)
        maxWidth >= 600.dp -> CategoryGridMetrics(columnCount = 3, cardHeight = 100.dp)
        else -> CategoryGridMetrics(columnCount = 2, cardHeight = 92.dp)
    }
}

private fun Int.categoryCountLabel(tab: CatalogTab): String {
    return if (tab == CatalogTab.SERIES) "$this dizi" else "$this içerik"
}
