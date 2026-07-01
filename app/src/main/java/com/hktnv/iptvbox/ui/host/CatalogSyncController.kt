package com.hktnv.iptvbox.ui.host

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.hktnv.iptvbox.data.catalog.CatalogStore
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.repository.catalog.CatalogSyncStatusStore
import com.hktnv.iptvbox.repository.catalog.XtreamCategoryEnrichmentQueue
import com.hktnv.iptvbox.repository.catalog.XtreamLazySyncRepository

internal data class CatalogSyncController(
    val lazySyncRepository: XtreamLazySyncRepository,
    val enrichmentQueue: XtreamCategoryEnrichmentQueue,
    val statuses: Map<String, CatalogSyncStatus>,
)

@Composable
internal fun rememberCatalogSyncController(
    context: Context,
    catalogStore: CatalogStore,
): CatalogSyncController {
    val lazySyncRepository = remember(catalogStore) { XtreamLazySyncRepository(catalogStore) }
    val statusStore = remember(context) { CatalogSyncStatusStore(context) }
    val enrichmentQueue = remember(lazySyncRepository, statusStore) {
        XtreamCategoryEnrichmentQueue(
            syncQueuedCategories = { playlist, onCategory ->
                lazySyncRepository.syncQueuedCategories(
                    playlist = playlist,
                    onCategory = { onCategory(it.name, it.kind) },
                )
            },
            statusReporter = statusStore,
        )
    }
    val statuses by statusStore.statuses.collectAsState()
    return CatalogSyncController(lazySyncRepository, enrichmentQueue, statuses)
}
