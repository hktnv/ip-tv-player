package com.hktnv.iptvbox.ui.search
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.ui.catalog.badgeContainerColor
import com.hktnv.iptvbox.ui.catalog.badgeContentColor
import com.hktnv.iptvbox.ui.catalog.badgeLabel
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.media.ContentArtwork
import com.hktnv.iptvbox.ui.media.FavoriteIndicator
import com.hktnv.iptvbox.ui.media.HorizontalMediaCardGrid

private val SearchResultRowHeight = 118.dp
private val SearchResultArtworkWidth = 104.dp
private val SearchResultArtworkHeight = 94.dp

@Composable
internal fun SearchResultsList(
    items: List<CatalogItem>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onOpenSeries: (String) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onRequestSideMenu: () -> Unit,
    initialFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val favoriteKey = favoriteIds.joinToString("|")
    val favoriteSet = remember(favoriteKey) { favoriteIds.toSet() }
    HorizontalMediaCardGrid(
        items = items,
        itemKey = { it.id },
        modifier = modifier,
        initialFocusRequester = initialFocusRequester,
        contentType = { "search-${it.kind.name}" },
    ) { item, requestSideMenuOnLeft, itemModifier ->
        SearchResultRow(
            item = item,
            favorite = item.id in favoriteSet,
            onOpen = {
                val seriesTitle = item.searchSeriesTitleOrNull()
                if (seriesTitle == null) onOpenItem(item) else onOpenSeries(seriesTitle)
            },
            onLongClick = { onShowItemOptions(item) },
            onRequestSideMenu = onRequestSideMenu,
            requestSideMenuOnLeft = requestSideMenuOnLeft,
            modifier = itemModifier,
        )
    }
}

@Composable
private fun SearchResultRow(
    item: CatalogItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    onRequestSideMenu: () -> Unit,
    requestSideMenuOnLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val title = item.searchResultTitle()
    val displayKind = item.searchResultKind()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchResultRowHeight)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.015f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft &&
                    requestSideMenuOnLeft
                ) {
                    onRequestSideMenu()
                    true
                } else {
                    false
                }
            }
            .tvClickable(onLongClick = onLongClick, onClick = onOpen),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
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
                    title = title,
                    kind = displayKind,
                    logoUrl = item.logoUrl,
                    showBadge = false,
                    modifier = Modifier
                        .width(SearchResultArtworkWidth)
                        .height(SearchResultArtworkHeight),
                )
                if (favorite) {
                    FavoriteIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp),
                    )
                }
            }
            SearchResultText(item = item, title = title, kind = displayKind, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SearchResultText(
    item: CatalogItem,
    title: String,
    kind: ContentKind,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
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
            SearchKindPill(kind)
            Text(
                text = item.searchResultMetaLine(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = kind.badgeContainerColor(),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, kind.badgeContentColor().copy(alpha = 0.32f)),
    ) {
        Text(
            text = kind.badgeLabel(),
            color = kind.badgeContentColor(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}
