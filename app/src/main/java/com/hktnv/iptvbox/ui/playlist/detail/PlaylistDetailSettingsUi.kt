package com.hktnv.iptvbox.ui.playlist.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.model.PlaylistAutoUpdateHourOptions
import com.hktnv.iptvbox.model.playlistAutoUpdateLabel
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift

@Composable
internal fun AutoUpdateSelector(
    selectedHours: Int,
    onSelect: (Int) -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }
    DetailPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailSectionTitle(stringResource(R.string.playlist_auto_refresh_interval))
            AutoUpdateInfoButton(
                selected = showInfo,
                onClick = { showInfo = !showInfo },
            )
        }
        AnimatedVisibility(visible = showInfo) {
            Text(
                text = stringResource(R.string.playlist_auto_refresh_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PlaylistAutoUpdateHourOptions.forEach { hours ->
                AutoUpdateOption(
                    label = playlistAutoUpdateLabel(hours),
                    selected = selectedHours == hours,
                    onClick = { onSelect(hours) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AutoUpdateInfoButton(
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .size(36.dp)
            .tvFocusLift(focused = focused, scale = 1.02f, liftPx = 0f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 0.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = stringResource(R.string.playlist_auto_refresh_info_toggle),
            modifier = Modifier.padding(8.dp),
            tint = if (selected || focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AutoUpdateOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .tvFocusLift(focused = focused, scale = 1.012f, liftPx = 0f)
            .onFocusChanged { focused = it.isFocused }
            .defaultMinSize(minHeight = 46.dp)
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> TvFocusBorder
                selected -> MaterialTheme.colorScheme.surfaceBorder
                else -> TvRestingBorder
            },
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 0.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
