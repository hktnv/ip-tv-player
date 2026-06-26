package com.evomrdm.iptvbox

import androidx.compose.runtime.Composable
import com.evomrdm.iptvbox.data.playlist.PlaylistLoadResult
import com.evomrdm.iptvbox.data.playlist.RemotePlaylistLoader

@Composable
internal fun IptvDialogHost(
    showAddDialog: Boolean,
    loader: RemotePlaylistLoader,
    telemetry: AppPerformanceTelemetry,
    existingPlaylistNames: List<String>,
    renamingPlaylist: LoadedPlaylist?,
    updateState: AppUpdateUiState,
    screen: AppScreen,
    showRecovery: Boolean,
    onDismissAdd: () -> Unit,
    onPlaylistLoaded: (DraftPlaylist, PlaylistLoadResult) -> Unit,
    onDismissRename: () -> Unit,
    onRenamePlaylist: (LoadedPlaylist, String) -> Unit,
    onDownloadUpdate: () -> Unit,
    onOpenPermission: () -> Unit,
    onOpenInstaller: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    if (showAddDialog) {
        AddPlaylistDialog(
            loader = loader,
            telemetry = telemetry,
            existingPlaylistNames = existingPlaylistNames,
            onDismiss = onDismissAdd,
            onLoaded = onPlaylistLoaded,
        )
    }

    renamingPlaylist?.let { playlist ->
        RenamePlaylistDialog(
            playlist = playlist,
            onDismiss = onDismissRename,
            onSave = { onRenamePlaylist(playlist, it) },
        )
    }

    val visibleUpdateState = updateState.takeUnless {
        it is AppUpdateUiState.Hidden ||
            screen == AppScreen.PLAYER ||
            showAddDialog ||
            renamingPlaylist != null ||
            showRecovery
    }
    if (visibleUpdateState != null) {
        AppUpdateDialog(
            state = visibleUpdateState,
            onDownload = onDownloadUpdate,
            onOpenPermission = onOpenPermission,
            onOpenInstaller = onOpenInstaller,
            onDismiss = onDismissUpdate,
        )
    }
}
