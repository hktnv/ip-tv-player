package com.hktnv.iptvbox.ui.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusableSurfaceStyle
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.stats

@Composable
internal fun PlaylistRow(
    playlist: LoadedPlaylist,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = remember(playlist.id, playlist.items) { playlist.stats() }
    val totalCount = remember(stats, playlist.items.size) {
        (stats.live + stats.movies + stats.series).takeIf { it > 0 } ?: playlist.items.size
    }
    var focused by remember { mutableStateOf(false) }
    val focusStyle = tvFocusableSurfaceStyle(focused = focused, selected = selected)
    Surface(
        modifier = modifier
            .height(72.dp)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.018f, liftPx = -2f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = focusStyle.container,
        contentColor = focusStyle.content,
        shape = RoundedCornerShape(10.dp),
        border = focusStyle.border,
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (selected) 4.dp else 3.dp)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaylistTypeChip(label = playlist.type.label())
                PlaylistRowText(
                    name = playlist.name,
                    selected = selected,
                    meta = stringResource(
                        R.string.playlist_row_meta,
                        totalCount,
                        stats.live,
                        stats.movies,
                        stats.series,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlaylistTypeChip(label: String) {
    Surface(
        modifier = Modifier
            .height(20.dp)
            .defaultMinSize(minWidth = 48.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlaylistRowText(
    name: String,
    selected: Boolean,
    meta: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f, fill = false),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                ActiveChip()
            }
        }
        Text(
            text = meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActiveChip() {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        shape = RoundedCornerShape(9.dp),
    ) {
        Text(
            text = stringResource(R.string.playlist_row_active),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
