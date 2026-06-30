package com.hktnv.iptvbox.state

import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.LoadedPlaylist

internal data class PlaylistDeletionUiState(
    val selectedPlaylistId: String?,
    val screen: AppScreen,
    val showPlaylistEntry: Boolean,
    val deletedActivePlaylist: Boolean,
)

internal fun playlistStateAfterDeletion(
    remainingPlaylists: List<LoadedPlaylist>,
    deletedPlaylistId: String,
    selectedPlaylistId: String?,
    currentScreen: AppScreen,
): PlaylistDeletionUiState {
    val remainingSelected = remainingPlaylists.any { it.id == selectedPlaylistId }
    val deletedActive = selectedPlaylistId == deletedPlaylistId || !remainingSelected
    val nextSelectedId = when {
        remainingSelected -> selectedPlaylistId
        else -> remainingPlaylists.firstOrNull()?.id
    }
    return PlaylistDeletionUiState(
        selectedPlaylistId = nextSelectedId,
        screen = if (remainingPlaylists.isEmpty()) AppScreen.PLAYLISTS else if (deletedActive) AppScreen.HOME else currentScreen,
        showPlaylistEntry = remainingPlaylists.isEmpty(),
        deletedActivePlaylist = deletedActive,
    )
}
