package com.hktnv.iptvbox.repository.catalog

import com.hktnv.iptvbox.model.LoadedPlaylist

internal interface CatalogSyncStatusReporter {
    fun markWaiting(playlist: LoadedPlaylist)
    fun markActive(playlist: LoadedPlaylist, categoryName: String, categoryKind: String)
    fun markCompleted(playlist: LoadedPlaylist, changedItemCount: Int)
    fun markFailed(playlist: LoadedPlaylist, message: String?)
}

internal object NoOpCatalogSyncStatusReporter : CatalogSyncStatusReporter {
    override fun markWaiting(playlist: LoadedPlaylist) = Unit
    override fun markActive(playlist: LoadedPlaylist, categoryName: String, categoryKind: String) = Unit
    override fun markCompleted(playlist: LoadedPlaylist, changedItemCount: Int) = Unit
    override fun markFailed(playlist: LoadedPlaylist, message: String?) = Unit
}
