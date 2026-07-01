package com.hktnv.iptvbox.ui.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.accentSubtle
import com.hktnv.iptvbox.model.PlaylistAutoUpdateHourOptions
import com.hktnv.iptvbox.model.PlaylistImportProgress
import com.hktnv.iptvbox.model.playlistAutoUpdateLabel
import com.hktnv.iptvbox.state.contentProgressLabel
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
    DetailPanel {
        DetailSectionTitle(stringResource(R.string.playlist_auto_refresh_interval))
        Text(
            text = stringResource(R.string.playlist_auto_refresh_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
        BoxWithConstraints {
            val rows = if (maxWidth < 460.dp) {
                PlaylistAutoUpdateHourOptions.chunked(2)
            } else {
                listOf(PlaylistAutoUpdateHourOptions)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowOptions.forEach { hours ->
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
        }
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
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .defaultMinSize(minHeight = 48.dp)
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> MaterialTheme.colorScheme.accentSubtle
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> TvFocusBorder
                selected -> MaterialTheme.colorScheme.primary
                else -> TvRestingBorder
            },
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 10.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun PlaylistProgressPanel(progress: PlaylistImportProgress) {
    DetailPanel {
        DetailSectionTitle(stringResource(R.string.playlist_refresh_status))
        Text(
            progress.message,
            color = if (progress.error == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        if (progress.error == null) {
            LinearProgressIndicator(
                progress = {
                    val total = progress.totalItems ?: 0
                    if (total > 0) {
                        (progress.processedItems.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = progress.error ?: contentProgressLabel(progress.processedItems, progress.totalItems),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
    }
}
