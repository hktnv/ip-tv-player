package com.hktnv.iptvbox.ui.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.accentSubtle
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift

@Composable
internal fun PlaylistDetailActions(
    active: Boolean,
    refreshing: Boolean,
    onUse: () -> Unit,
    onReload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DetailPanel {
        DetailSectionTitle(stringResource(R.string.playlist_detail_actions_section))
        PlaylistActionButton(
            label = if (active) {
                stringResource(R.string.action_open_playlist)
            } else {
                stringResource(R.string.action_open_or_use_playlist)
            },
            icon = Icons.Filled.PlayArrow,
            style = PlaylistActionStyle.Primary,
            onClick = onUse,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlaylistActionButton(
                label = if (refreshing) {
                    stringResource(R.string.action_refreshing)
                } else {
                    stringResource(R.string.action_refresh)
                },
                icon = Icons.Filled.Refresh,
                enabled = !refreshing,
                style = PlaylistActionStyle.Secondary,
                onClick = onReload,
                modifier = Modifier.weight(1f),
            )
            PlaylistActionButton(
                label = stringResource(R.string.action_edit),
                icon = Icons.Filled.Edit,
                style = PlaylistActionStyle.Secondary,
                onClick = onRename,
                modifier = Modifier.weight(1f),
            )
        }
        PlaylistActionButton(
            label = stringResource(R.string.action_delete),
            icon = Icons.Filled.Delete,
            style = PlaylistActionStyle.Danger,
            onClick = onDelete,
        )
    }
}

@Composable
private fun PlaylistActionButton(
    label: String,
    icon: ImageVector,
    style: PlaylistActionStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = actionColors(style, enabled, focused)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.018f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onClick),
        color = colors.container,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, colors.border),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colors.content,
            )
            Text(
                text = label,
                color = colors.content,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun actionColors(
    style: PlaylistActionStyle,
    enabled: Boolean,
    focused: Boolean,
): PlaylistActionColors {
    val disabledContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val disabledContent = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    if (!enabled) {
        return PlaylistActionColors(
            container = disabledContainer,
            content = disabledContent,
            border = MaterialTheme.colorScheme.surfaceBorder,
        )
    }
    return when (style) {
        PlaylistActionStyle.Primary -> PlaylistActionColors(
            container = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.accentSubtle,
            content = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            border = if (focused) TvFocusBorder else MaterialTheme.colorScheme.primary,
        )
        PlaylistActionStyle.Secondary -> PlaylistActionColors(
            container = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
            content = MaterialTheme.colorScheme.onSurface,
            border = if (focused) TvFocusBorder else TvRestingBorder,
        )
        PlaylistActionStyle.Danger -> PlaylistActionColors(
            container = if (focused) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
            content = if (focused) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.error,
            border = if (focused) TvFocusBorder else MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
        )
    }
}

private enum class PlaylistActionStyle {
    Primary,
    Secondary,
    Danger,
}

private data class PlaylistActionColors(
    val container: Color,
    val content: Color,
    val border: Color,
)
