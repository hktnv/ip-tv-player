package com.hktnv.iptvbox.repository.catalog

import android.content.Context
import com.hktnv.iptvbox.data.catalog.CategoryKindMovie
import com.hktnv.iptvbox.data.catalog.CategoryKindSeries
import com.hktnv.iptvbox.model.CatalogSyncCategoryKind
import com.hktnv.iptvbox.model.CatalogSyncPhase
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.model.LoadedPlaylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CatalogSyncStatusStore(context: Context) : CatalogSyncStatusReporter {
    private val preferences = context.applicationContext.getSharedPreferences(
        "catalog_sync_status",
        Context.MODE_PRIVATE,
    )
    private val _statuses = MutableStateFlow(loadStatuses())
    val statuses: StateFlow<Map<String, CatalogSyncStatus>> = _statuses.asStateFlow()

    override fun markWaiting(playlist: LoadedPlaylist) {
        publish(
            CatalogSyncStatus(
                playlistId = playlist.id,
                phase = CatalogSyncPhase.WAITING,
            ),
            persist = false,
        )
    }

    override fun markActive(playlist: LoadedPlaylist, categoryName: String, categoryKind: String) {
        publish(
            CatalogSyncStatus(
                playlistId = playlist.id,
                phase = CatalogSyncPhase.ACTIVE,
                categoryName = categoryName,
                categoryKind = categoryKind.toCatalogSyncKind(),
            ),
            persist = false,
        )
    }

    override fun markCompleted(playlist: LoadedPlaylist, changedItemCount: Int) {
        publish(
            CatalogSyncStatus(
                playlistId = playlist.id,
                phase = CatalogSyncPhase.COMPLETED,
                changedItemCount = changedItemCount,
            ),
            persist = true,
        )
    }

    override fun markFailed(playlist: LoadedPlaylist, message: String?) {
        publish(
            CatalogSyncStatus(
                playlistId = playlist.id,
                phase = CatalogSyncPhase.FAILED,
                errorMessage = message,
            ),
            persist = true,
        )
    }

    private fun publish(status: CatalogSyncStatus, persist: Boolean) {
        _statuses.value = _statuses.value + (status.playlistId to status)
        if (persist) saveStatus(status)
    }

    private fun loadStatuses(): Map<String, CatalogSyncStatus> {
        return preferences.getStringSet(KEY_PLAYLIST_IDS, emptySet()).orEmpty()
            .mapNotNull { playlistId -> readStatus(playlistId)?.let { playlistId to it } }
            .toMap()
    }

    private fun readStatus(playlistId: String): CatalogSyncStatus? {
        val phase = preferences.getString(key(playlistId, "phase"), null)
            ?.let { runCatching { CatalogSyncPhase.valueOf(it) }.getOrNull() }
            ?: return null
        return CatalogSyncStatus(
            playlistId = playlistId,
            phase = phase,
            categoryKind = preferences.getString(key(playlistId, "kind"), null)
                ?.let { runCatching { CatalogSyncCategoryKind.valueOf(it) }.getOrNull() }
                ?: CatalogSyncCategoryKind.CATALOG,
            changedItemCount = preferences.getInt(key(playlistId, "changed"), 0),
            errorMessage = preferences.getString(key(playlistId, "error"), null),
            updatedAtEpochMillis = preferences.getLong(key(playlistId, "updated"), 0L),
        )
    }

    private fun saveStatus(status: CatalogSyncStatus) {
        val ids = preferences.getStringSet(KEY_PLAYLIST_IDS, emptySet()).orEmpty() + status.playlistId
        preferences.edit()
            .putStringSet(KEY_PLAYLIST_IDS, ids)
            .putString(key(status.playlistId, "phase"), status.phase.name)
            .putString(key(status.playlistId, "kind"), status.categoryKind.name)
            .putInt(key(status.playlistId, "changed"), status.changedItemCount)
            .putString(key(status.playlistId, "error"), status.errorMessage)
            .putLong(key(status.playlistId, "updated"), status.updatedAtEpochMillis)
            .apply()
    }

    private fun String.toCatalogSyncKind(): CatalogSyncCategoryKind {
        return when (this) {
            CategoryKindMovie -> CatalogSyncCategoryKind.MOVIE
            CategoryKindSeries -> CatalogSyncCategoryKind.SERIES
            else -> CatalogSyncCategoryKind.CATALOG
        }
    }

    private fun key(playlistId: String, field: String): String = "playlist_${playlistId}_$field"

    private companion object {
        const val KEY_PLAYLIST_IDS = "playlist_ids"
    }
}
