package com.hktnv.iptvbox.ui.playlist

import androidx.annotation.StringRes
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.model.CatalogSyncCategoryKind
import com.hktnv.iptvbox.model.CatalogSyncPhase
import com.hktnv.iptvbox.model.CatalogSyncStatus

internal data class PlaylistSyncStatusUiModel(
    val active: Boolean,
    val error: Boolean,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val bodyArg: String? = null,
)

internal fun CatalogSyncStatus?.toPlaylistSyncStatusUiModel(): PlaylistSyncStatusUiModel {
    return when (this?.phase) {
        CatalogSyncPhase.WAITING -> PlaylistSyncStatusUiModel(
            active = true,
            error = false,
            titleRes = R.string.playlist_sync_running_title,
            bodyRes = R.string.playlist_sync_waiting_body,
        )

        CatalogSyncPhase.ACTIVE -> PlaylistSyncStatusUiModel(
            active = true,
            error = false,
            titleRes = R.string.playlist_sync_running_title,
            bodyRes = activeBodyRes(),
            bodyArg = categoryName.orEmpty(),
        )

        CatalogSyncPhase.FAILED -> PlaylistSyncStatusUiModel(
            active = false,
            error = true,
            titleRes = R.string.playlist_sync_failed_title,
            bodyRes = R.string.playlist_sync_failed_body,
        )

        CatalogSyncPhase.COMPLETED,
        null,
        -> PlaylistSyncStatusUiModel(
            active = false,
            error = false,
            titleRes = R.string.playlist_sync_current_title,
            bodyRes = R.string.playlist_sync_current_body,
        )
    }
}

@StringRes
private fun CatalogSyncStatus.activeBodyRes(): Int {
    return when (categoryKind) {
        CatalogSyncCategoryKind.MOVIE -> R.string.playlist_sync_active_movie
        CatalogSyncCategoryKind.SERIES -> R.string.playlist_sync_active_series
        CatalogSyncCategoryKind.CATALOG -> R.string.playlist_sync_active_catalog
    }
}
