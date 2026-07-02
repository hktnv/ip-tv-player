package com.hktnv.iptvbox.state

internal data class CatalogSnapshotLoadPlan(
    val clearSnapshot: Boolean,
    val showBlockingLoading: Boolean,
)

internal fun planCatalogSnapshotLoad(
    currentSnapshotPlaylistId: String?,
    targetPlaylistId: String?,
    showPlaylistEntry: Boolean,
): CatalogSnapshotLoadPlan {
    if (targetPlaylistId == null || showPlaylistEntry) {
        return CatalogSnapshotLoadPlan(
            clearSnapshot = currentSnapshotPlaylistId != null,
            showBlockingLoading = false,
        )
    }

    val reusableSnapshot = currentSnapshotPlaylistId == targetPlaylistId
    return CatalogSnapshotLoadPlan(
        clearSnapshot = !reusableSnapshot,
        showBlockingLoading = !reusableSnapshot,
    )
}
