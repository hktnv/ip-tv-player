package com.hktnv.iptvbox.ui.dialog
import androidx.compose.runtime.Composable
import com.hktnv.iptvbox.core.model.CatalogItem
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
    contentOptionsItem: CatalogItem?,
    contentOptionsFavorite: Boolean,
    updateState: AppUpdateUiState,
    screen: AppScreen,
    showRecovery: Boolean,
    onDismissAdd: () -> Unit,
    onDismissExit: () -> Unit,
    onConfirmExit: () -> Unit,
    onPlaylistLoaded: (DraftPlaylist, PlaylistLoadResult) -> Unit,
    onDismissRename: () -> Unit,
    onRenamePlaylist: (LoadedPlaylist, String) -> Unit,
    onDismissContentOptions: () -> Unit,
    onOpenContentOptionsItem: () -> Unit,
    onToggleContentOptionsFavorite: () -> Unit,
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

    contentOptionsItem?.let { item ->
        ContentOptionsDialog(
            item = item,
            favorite = contentOptionsFavorite,
            onDismiss = onDismissContentOptions,
            onOpen = onOpenContentOptionsItem,
            onToggleFavorite = onToggleContentOptionsFavorite,
        )
    }

    val visibleUpdateState = updateState.takeUnless {
        it is AppUpdateUiState.Hidden ||
            screen == AppScreen.PLAYER ||
            showAddDialog ||
            showExitDialog ||
            renamingPlaylist != null ||
            contentOptionsItem != null ||
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
