package com.hktnv.iptvbox.ui.playlist.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistImportProgress
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.ui.common.ScreenHeader

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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        val splitLayout = maxWidth >= 760.dp
        LazyColumn(
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
            if (splitLayout) {
                item {
                    PlaylistDetailSplitLayout(
                        playlist = playlist,
                        active = active,
                        progress = progress,
                        syncStatus = syncStatus,
                        onUse = onUse,
                        onReload = onReload,
                        onRename = onRename,
                        onDelete = onDelete,
                        onAutoUpdateHoursChange = onAutoUpdateHoursChange,
                    )
                }
            } else {
                item {
                    PlaylistDetailSingleColumn(
                        playlist = playlist,
                        active = active,
                        progress = progress,
                        syncStatus = syncStatus,
                        onUse = onUse,
                        onReload = onReload,
                        onRename = onRename,
                        onDelete = onDelete,
                        onAutoUpdateHoursChange = onAutoUpdateHoursChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailSplitLayout(
    playlist: LoadedPlaylist,
    active: Boolean,
    progress: PlaylistImportProgress?,
    syncStatus: CatalogSyncStatus?,
    onUse: () -> Unit,
    onReload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAutoUpdateHoursChange: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlaylistOverviewPanel(playlist = playlist, syncStatus = syncStatus)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AutoUpdateSelector(
                selectedHours = playlist.autoUpdateHours,
                onSelect = onAutoUpdateHoursChange,
            )
            PlaylistDetailActions(
                active = active,
                progress = progress,
                onUse = onUse,
                onReload = onReload,
                onRename = onRename,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun PlaylistDetailSingleColumn(
    playlist: LoadedPlaylist,
    active: Boolean,
    progress: PlaylistImportProgress?,
    syncStatus: CatalogSyncStatus?,
    onUse: () -> Unit,
    onReload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAutoUpdateHoursChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PlaylistOverviewPanel(playlist = playlist, syncStatus = syncStatus)
        AutoUpdateSelector(
            selectedHours = playlist.autoUpdateHours,
            onSelect = onAutoUpdateHoursChange,
        )
        PlaylistDetailActions(
            active = active,
            progress = progress,
            onUse = onUse,
            onReload = onReload,
            onRename = onRename,
            onDelete = onDelete,
        )
    }
}
