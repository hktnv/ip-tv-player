package com.hktnv.iptvbox.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
internal fun PlayerRelatedContentCard(
    item: CatalogItem,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDirectionalKey: (Key) -> Boolean = { false },
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .width(width)
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && onDirectionalKey(event.key)
            }
            .tvClickable(onClick = onClick)
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && onDirectionalKey(event.key)
            }
            .onKeyEvent { event ->
                event.type == KeyEventType.KeyDown && onDirectionalKey(event.key)
            },
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (focused) 0.78f else 0.52f),
        shape = RoundedCornerShape(mediaCardRadius),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.focusBorder else TvRestingBorder.copy(alpha = 0.52f),
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
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.displayTitle(),
                    modifier = Modifier.height(34.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (focused) 12.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (focused) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.metaLine(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
