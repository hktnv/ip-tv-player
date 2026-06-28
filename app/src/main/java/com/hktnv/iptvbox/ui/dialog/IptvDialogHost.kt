package com.hktnv.iptvbox.ui.dialog
import androidx.compose.runtime.Composable
import com.hktnv.iptvbox.data.playlist.PlaylistLoadResult
import com.hktnv.iptvbox.data.playlist.RemotePlaylistLoader
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.ui.playlist.AddPlaylistDialog
import com.hktnv.iptvbox.ui.playlist.DraftPlaylist
import com.hktnv.iptvbox.ui.playlist.RenamePlaylistDialog
import com.hktnv.iptvbox.update.AppUpdateDialog
import com.hktnv.iptvbox.update.AppUpdateUiState

@Composable
internal fun IptvDialogHost(
    showAddDialog: Boolean,
    showExitDialog: Boolean,
    loader: RemotePlaylistLoader,
    telemetry: AppPerformanceTelemetry,
    existingPlaylistNames: List<String>,
    renamingPlaylist: LoadedPlaylist?,
    updateState: AppUpdateUiState,
    screen: AppScreen,
    showRecovery: Boolean,
    onDismissAdd: () -> Unit,
    onDismissExit: () -> Unit,
    onConfirmExit: () -> Unit,
    onPlaylistLoaded: (DraftPlaylist, PlaylistLoadResult) -> Unit,
    onDismissRename: () -> Unit,
    onRenamePlaylist: (LoadedPlaylist, String) -> Unit,
    onDownloadUpdate: () -> Unit,
    onOpenPermission: () -> Unit,
    onOpenInstaller: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    if (showExitDialog) {
        ExitConfirmationDialog(
            onDismiss = onDismissExit,
            onConfirm = onConfirmExit,
        )
    }

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
            showExitDialog ||
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
