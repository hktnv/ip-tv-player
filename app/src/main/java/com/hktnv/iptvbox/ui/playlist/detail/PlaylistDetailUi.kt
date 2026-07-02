package com.hktnv.iptvbox.ui.playlist.detail

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistImportProgress
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.ui.common.ScreenHeader
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift

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
    val configuration = LocalConfiguration.current
    val television = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        val splitLayout = television || maxWidth >= 900.dp
        val sectionGap = if (maxWidth < 600.dp) 16.dp else 10.dp
        LazyColumn(
            contentPadding = PaddingValues(top = 10.dp, bottom = ScreenBottomPadding),
            verticalArrangement = Arrangement.spacedBy(sectionGap),
        ) {
            item {
                PlaylistDetailHeader(
                    title = playlist.name,
                    subtitle = if (active) {
                        stringResource(R.string.playlist_active_subtitle)
                    } else {
                        stringResource(R.string.playlist_management_subtitle)
                    },
                    onBack = onBack,
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
                        sectionGap = sectionGap,
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
                        sectionGap = sectionGap,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailBackButton(onBack = onBack)
        ScreenHeader(
            title = title,
            subtitle = subtitle,
            actionLabel = null,
            onAction = null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DetailBackButton(onBack: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(52.dp)
            .height(44.dp)
            .tvFocusLift(focused = focused, scale = 1.012f, liftPx = 0f)
            .tvClickable(onClick = onBack),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 0.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.playlist_detail_back_action),
                modifier = Modifier.size(20.dp),
            )
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
    sectionGap: Dp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier.weight(0.4f),
            verticalArrangement = Arrangement.spacedBy(sectionGap),
        ) {
            PlaylistOverviewPanel(playlist = playlist, syncStatus = syncStatus)
        }
        Column(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.spacedBy(sectionGap),
        ) {
            PlaylistDetailActions(
                active = active,
                progress = progress,
                onUse = onUse,
                onReload = onReload,
                onRename = onRename,
                onDelete = onDelete,
            )
            AutoUpdateSelector(
                selectedHours = playlist.autoUpdateHours,
                updatedAtEpochMillis = playlist.updatedAtEpochMillis,
                onSelect = onAutoUpdateHoursChange,
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
    sectionGap: Dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(sectionGap)) {
        PlaylistOverviewPanel(playlist = playlist, syncStatus = syncStatus)
        PlaylistDetailActions(
            active = active,
            progress = progress,
            onUse = onUse,
            onReload = onReload,
            onRename = onRename,
            onDelete = onDelete,
        )
        AutoUpdateSelector(
            selectedHours = playlist.autoUpdateHours,
            updatedAtEpochMillis = playlist.updatedAtEpochMillis,
            onSelect = onAutoUpdateHoursChange,
        )
    }
}
