package com.hktnv.iptvbox.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.focusBorder
import com.hktnv.iptvbox.core.designsystem.mediaCardRadius
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.media.ContentArtwork
import com.hktnv.iptvbox.ui.media.displayTitle
import com.hktnv.iptvbox.ui.media.metaLine

@Composable
internal fun PlayerRelatedContentRail(
    items: List<CatalogItem>,
    onSelectItem: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = when {
            maxWidth < 600.dp -> 136.dp
            maxWidth < 900.dp -> 154.dp
            else -> 178.dp
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = items, key = { it.id }) { item ->
                PlayerRelatedContentCard(
                    item = item,
                    width = cardWidth,
                    onClick = { onSelectItem(item) },
                )
            }
        }
    }
}

@Composable
private fun PlayerRelatedContentCard(
    item: CatalogItem,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(width)
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .tvClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (focused) 0.86f else 0.66f),
        shape = RoundedCornerShape(mediaCardRadius),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.focusBorder else TvRestingBorder,
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 8.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center,
            ) {
                ContentArtwork(
                    title = item.displayTitle(),
                    kind = item.kind,
                    logoUrl = item.logoUrl,
                    showBadge = false,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.displayTitle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.metaLine(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
