package com.hktnv.iptvbox.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistAutoUpdateHourOptions
import com.hktnv.iptvbox.model.PlaylistImportProgress
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.model.playlistAutoUpdateLabel
import com.hktnv.iptvbox.state.contentProgressLabel
import com.hktnv.iptvbox.ui.common.ScreenHeader
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.stats

@Composable
internal fun PlaylistDetailScreen(
    playlist: LoadedPlaylist,
    active: Boolean,
    contentPadding: Dp,
    onBack: () -> Unit,
    onUse: () -> Unit,
    onReload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAutoUpdateHoursChange: (Int) -> Unit,
    progress: PlaylistImportProgress? = null,
    syncStatus: CatalogSyncStatus? = null,
) {
    BackHandler(onBack = onBack)
    val stats = remember(playlist.id, playlist.items) { playlist.stats() }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
        contentPadding = PaddingValues(top = 16.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = playlist.name,
                subtitle = if (active) {
                    stringResource(R.string.playlist_active_subtitle)
                } else {
                    stringResource(R.string.playlist_management_subtitle)
                },
                actionLabel = null,
                onAction = null,
            )
        }
        item {
            DetailPanel {
                DetailLine(
                    stringResource(R.string.playlist_source_type),
                    if (playlist.xtreamApiSupported) {
                        "${playlist.type.label()} · ${stringResource(R.string.playlist_xtream_supported)}"
                    } else {
                        playlist.type.label()
                    },
                )
                DetailLine(
                    stringResource(R.string.playlist_content_summary),
                    "${stats.total} içerik · ${stats.live} canlı · ${stats.movies} film · ${stats.series} dizi",
                )
                DetailLine(stringResource(R.string.playlist_auto_refresh), playlistAutoUpdateLabel(playlist.autoUpdateHours))
            }
        }
        if (playlist.xtreamApiSupported || syncStatus != null) {
            item {
                PlaylistSyncStatusPanel(syncStatus)
            }
        }
        item {
            AutoUpdateSelector(
                selectedHours = playlist.autoUpdateHours,
                onSelect = onAutoUpdateHoursChange,
            )
        }
        progress?.let {
            item { PlaylistProgressPanel(it) }
        }
        item {
            PlaylistDetailActions(
                active = active,
                refreshing = progress?.active == true,
                onUse = onUse,
                onReload = onReload,
                onRename = onRename,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun DetailPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AutoUpdateSelector(
    selectedHours: Int,
    onSelect: (Int) -> Unit,
) {
    DetailPanel {
        Text(
            stringResource(R.string.playlist_auto_refresh_interval),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        BoxWithConstraints {
            val rows = if (maxWidth < 560.dp) {
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
                            FilterChip(
                                selected = selectedHours == hours,
                                onClick = { onSelect(hours) },
                                modifier = Modifier.weight(1f),
                                label = { Text(playlistAutoUpdateLabel(hours), maxLines = 1) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistProgressPanel(progress: PlaylistImportProgress) {
    DetailPanel {
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

@Composable
private fun PlaylistDetailActions(
    active: Boolean,
    refreshing: Boolean,
    onUse: () -> Unit,
    onReload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onUse,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                if (active) {
                    stringResource(R.string.action_open_playlist)
                } else {
                    stringResource(R.string.action_open_or_use_playlist)
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onReload,
                enabled = !refreshing,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (refreshing) {
                        stringResource(R.string.action_refreshing)
                    } else {
                        stringResource(R.string.action_refresh)
                    },
                )
            }
            OutlinedButton(onClick = onRename, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_edit))
            }
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
        }
    }
}
