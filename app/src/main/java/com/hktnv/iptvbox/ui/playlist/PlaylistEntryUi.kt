package com.hktnv.iptvbox.ui.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistImportProgress
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.media.catalogSummary
import com.hktnv.iptvbox.ui.playlist.detail.PlaylistDetailScreen

@Composable
internal fun PlaylistEntryScreen(
    playlists: List<LoadedPlaylist>,
    selectedPlaylistId: String?,
    detailPlaylistId: String?,
    contentPadding: Dp,
    onOpenLastPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onOpenPlaylistDetails: (String) -> Unit,
    onClosePlaylistDetails: () -> Unit,
    onUsePlaylist: (String) -> Unit,
    onReloadPlaylist: (LoadedPlaylist) -> Unit,
    onRenamePlaylist: (LoadedPlaylist) -> Unit,
    onDeletePlaylist: (LoadedPlaylist) -> Unit,
    onAutoUpdateHoursChange: (LoadedPlaylist, Int) -> Unit,
    onOpenSettings: () -> Unit,
    progress: PlaylistImportProgress? = null,
    syncStatuses: Map<String, CatalogSyncStatus> = emptyMap(),
) {
    if (playlists.isEmpty()) {
        EmptyPlaylistEntryScene(contentPadding = contentPadding, onAddPlaylist = onAddPlaylist)
        return
    }

    val detailPlaylist = playlists.firstOrNull { it.id == detailPlaylistId }
    if (detailPlaylist != null) {
        PlaylistDetailScreen(
            playlist = detailPlaylist,
            active = detailPlaylist.id == selectedPlaylistId,
            contentPadding = contentPadding,
            onBack = onClosePlaylistDetails,
            onUse = { onUsePlaylist(detailPlaylist.id) },
            onReload = { onReloadPlaylist(detailPlaylist) },
            onRename = { onRenamePlaylist(detailPlaylist) },
            onDelete = { onDeletePlaylist(detailPlaylist) },
            onAutoUpdateHoursChange = { onAutoUpdateHoursChange(detailPlaylist, it) },
            progress = progress?.takeIf { it.playlistId == detailPlaylist.id },
            syncStatus = syncStatuses[detailPlaylist.id],
        )
        return
    }

    PlaylistHubContent(
        playlists = playlists,
        selectedPlaylistId = selectedPlaylistId,
        contentPadding = contentPadding,
        onOpenLastPlaylist = onOpenLastPlaylist,
        onAddPlaylist = onAddPlaylist,
        onOpenSettings = onOpenSettings,
        onOpenPlaylistDetails = onOpenPlaylistDetails,
    )
}

@Composable
private fun PlaylistHubContent(
    playlists: List<LoadedPlaylist>,
    selectedPlaylistId: String?,
    contentPadding: Dp,
    onOpenLastPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlaylistDetails: (String) -> Unit,
) {
    val lastPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId } ?: playlists.first()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = contentPadding),
    ) {
        val columns = if (maxWidth >= 900.dp) 2 else 1
        val compact = maxWidth < 600.dp
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = ScreenBottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "playlist-hub-header", contentType = "header") {
                PlaylistHubHeader(
                    playlistCount = playlists.size,
                    onAddPlaylist = onAddPlaylist,
                    onOpenSettings = onOpenSettings,
                )
            }
            item(key = "playlist-hub-continue", contentType = "continue") {
                ContinuePlaylistBanner(
                    playlist = lastPlaylist,
                    compact = compact,
                    onClick = onOpenLastPlaylist,
                )
            }
            item(key = "playlist-hub-list-title", contentType = "section-title") {
                Text(
                    text = stringResource(R.string.playlist_hub_section_title),
                    modifier = Modifier.padding(top = 8.dp, bottom = 0.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            items(
                items = playlists.chunked(columns),
                key = { row -> row.joinToString(separator = ":") { it.id } },
                contentType = { "playlist-row" },
            ) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            selected = playlist.id == selectedPlaylistId,
                            onClick = { onOpenPlaylistDetails(playlist.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size < columns) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
