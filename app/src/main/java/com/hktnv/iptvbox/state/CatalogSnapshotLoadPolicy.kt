package com.hktnv.iptvbox.state

import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot

internal data class CatalogSnapshotLoadPlan(
    val clearSnapshot: Boolean,
    val showBlockingLoading: Boolean,
)

internal fun planCatalogSnapshotLoad(
    currentSnapshotPlaylistId: String?,
    targetPlaylistId: String?,
    showPlaylistEntry: Boolean,
    currentSnapshotViewReady: Boolean = true,
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
        showBlockingLoading = !reusableSnapshot || !currentSnapshotViewReady,
    )
}

internal fun isCatalogSnapshotReadyForView(
    snapshot: CatalogSnapshot?,
    playlistId: String?,
    showCategoryLanding: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
): Boolean {
    if (playlistId == null) return true
    if (snapshot?.playlistId != playlistId) return false
    if (showCategoryLanding) return true
    return when (selectedTab) {
        CatalogTab.LIVE,
        CatalogTab.MOVIES -> snapshot.hasItemsForCategory(selectedTab, selectedCategory)
        CatalogTab.SERIES -> snapshot.hasSeriesView(
            selectedCategory = selectedCategory,
            selectedSeriesTitle = selectedSeriesTitle,
            selectedSeasonNumber = selectedSeasonNumber,
        )
    }
}

private fun CatalogSnapshot.hasItemsForCategory(tab: CatalogTab, category: String?): Boolean {
    if (category == null) return items(tab).isNotEmpty() || categoryCount(tab, null) == 0
    return visibleItems(tab, category).isNotEmpty() || categoryCount(tab, category) == 0
}

private fun CatalogSnapshot.hasSeriesView(
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
): Boolean {
    if (selectedSeriesTitle == null) {
        return seriesGroups(selectedCategory).isNotEmpty() ||
            categoryCount(CatalogTab.SERIES, selectedCategory) == 0
    }
    if (selectedSeasonNumber != null) {
        return episodes(selectedSeriesTitle, selectedSeasonNumber).isNotEmpty()
    }
    return seasons(selectedSeriesTitle).isNotEmpty() ||
        episodes(selectedSeriesTitle, seasonNumber = null).isNotEmpty()
}
