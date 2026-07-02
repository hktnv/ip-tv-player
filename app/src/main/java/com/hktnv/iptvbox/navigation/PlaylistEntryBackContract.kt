package com.hktnv.iptvbox.navigation

import com.hktnv.iptvbox.model.AppScreen

internal fun playlistEntryReturnScreen(origin: AppScreen?): AppScreen? {
    return origin?.takeUnless { it == AppScreen.PLAYLISTS || it == AppScreen.PLAYER }
}

internal fun shouldHandlePlaylistEntryBack(
    showPlaylistEntry: Boolean,
    playlistDetailId: String?,
    returnScreen: AppScreen?,
): Boolean {
    return showPlaylistEntry && playlistDetailId == null && returnScreen != null
}
