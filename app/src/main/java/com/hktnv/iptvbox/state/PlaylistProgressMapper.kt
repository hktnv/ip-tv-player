package com.hktnv.iptvbox.state

import com.hktnv.iptvbox.data.playlist.PlaylistLoadProgress
import com.hktnv.iptvbox.data.playlist.PlaylistLoadStage
import com.hktnv.iptvbox.model.DraftLoadState
import com.hktnv.iptvbox.model.PlaylistImportProgress

internal fun PlaylistLoadProgress.toDraftLoadState(): DraftLoadState {
    return DraftLoadState(
        loading = stage != PlaylistLoadStage.COMPLETED,
        message = stage.userLabel(),
        processedItems = processedItems,
        totalItems = totalItems,
    )
}

internal fun PlaylistLoadProgress.toPlaylistImportProgress(playlistId: String): PlaylistImportProgress {
    return PlaylistImportProgress(
        playlistId = playlistId,
        message = stage.userLabel(),
        processedItems = processedItems,
        totalItems = totalItems,
        complete = stage == PlaylistLoadStage.COMPLETED,
    )
}

internal fun contentProgressLabel(processedItems: Int, totalItems: Int?): String {
    return if (totalItems != null && totalItems > 0) {
        "$processedItems / $totalItems içerik"
    } else {
        "$processedItems içerik"
    }
}

private fun PlaylistLoadStage.userLabel(): String {
    return when (this) {
        PlaylistLoadStage.CONNECTING,
        PlaylistLoadStage.DOWNLOADING -> "İndiriliyor"
        PlaylistLoadStage.READING -> "İçerikler okunuyor"
        PlaylistLoadStage.PREPARING,
        PlaylistLoadStage.WRITING -> "Katalog hazırlanıyor"
        PlaylistLoadStage.COMPLETED -> "Tamamlandı"
    }
}
