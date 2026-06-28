package com.hktnv.iptvbox.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.ui.catalog.badgeLabel
import com.hktnv.iptvbox.ui.catalog.tint
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.media.ContentArtwork
import com.hktnv.iptvbox.ui.media.FavoriteIndicator
import com.hktnv.iptvbox.ui.media.displayTitle
import com.hktnv.iptvbox.ui.media.metaLine

@Composable
internal fun SearchResultsList(
    items: List<CatalogItem>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    initialFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val favoriteKey = favoriteIds.joinToString("|")
    val favoriteSet = remember(favoriteKey) { favoriteIds.toSet() }
    val firstItemId = items.firstOrNull()?.id
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items, key = { _, item -> item.id }, contentType = { _, item -> "search-${item.kind.name}" }) { index, item ->
            SearchResultRow(
                item = item,
                favorite = item.id in favoriteSet,
                onOpen = { onOpenItem(item) },
                onLongClick = { onShowItemOptions(item) },
                modifier = if (index == 0 && item.id == firstItemId) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    item: CatalogItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val liveLike = item.kind in CatalogTab.LIVE.kinds
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.015f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onLongClick = onLongClick, onClick = onOpen),
        color = if (focused) TvFocusPanel else IptvColors.Panel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                ContentArtwork(
                    title = item.displayTitle(),
                    kind = item.kind,
                    logoUrl = item.logoUrl,
                    showBadge = false,
                    modifier = Modifier
                        .width(if (liveLike) 78.dp else 64.dp)
                        .height(if (liveLike) 58.dp else 86.dp),
                )
                if (favorite) {
                    FavoriteIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp),
                    )
                }
            }
            SearchResultText(item = item, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SearchResultText(item: CatalogItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = item.displayTitle(),
            color = IptvColors.TextPrimary,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchKindPill(item.kind)
            Text(
                text = item.metaLine(),
                color = IptvColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchKindPill(kind: ContentKind) {
    Surface(
        color = kind.tint().copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, kind.tint().copy(alpha = 0.32f)),
    ) {
        Text(
            text = kind.badgeLabel(),
            color = IptvColors.TextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}
