package com.hktnv.iptvbox.navigation

import com.hktnv.iptvbox.model.AppScreen

internal enum class RootBackAction {
    None,
    ReturnToPlaylistEntryOrigin,
    OpenNavigationDrawer,
    ShowExitConfirmation,
}

internal fun resolveRootBackAction(
    wide: Boolean,
    contentBackBlocked: Boolean,
    showPlaylistEntry: Boolean,
    playlistDetailId: String?,
    playlistEntryReturnScreen: AppScreen?,
    screen: AppScreen,
    sideMenuExpanded: Boolean,
): RootBackAction {
    if (contentBackBlocked || screen == AppScreen.PLAYER || playlistDetailId != null) {
        return RootBackAction.None
    }
    if (showPlaylistEntry) {
        return if (playlistEntryReturnScreen != null) {
            RootBackAction.ReturnToPlaylistEntryOrigin
        } else {
            RootBackAction.ShowExitConfirmation
        }
    }
    if (!wide) return RootBackAction.ShowExitConfirmation
    return if (sideMenuExpanded) {
        RootBackAction.ShowExitConfirmation
    } else {
        RootBackAction.OpenNavigationDrawer
    }
}
