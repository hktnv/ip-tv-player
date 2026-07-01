package com.hktnv.iptvbox.model

internal data class CatalogSyncStatus(
    val playlistId: String,
    val phase: CatalogSyncPhase,
    val categoryName: String? = null,
    val categoryKind: CatalogSyncCategoryKind = CatalogSyncCategoryKind.CATALOG,
    val changedItemCount: Int = 0,
    val errorMessage: String? = null,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
) {
    val active: Boolean
        get() = phase == CatalogSyncPhase.WAITING || phase == CatalogSyncPhase.ACTIVE
}

internal enum class CatalogSyncPhase {
    WAITING,
    ACTIVE,
    COMPLETED,
    FAILED,
}

internal enum class CatalogSyncCategoryKind {
    CATALOG,
    MOVIE,
    SERIES,
}
