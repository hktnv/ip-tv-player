package com.hktnv.iptvbox.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.repository.catalog.XtreamCategoryEnrichmentQueue
import kotlinx.coroutines.delay

@Composable
internal fun StartupCategoryEnrichmentEffect(
    restoredApplied: Boolean,
    selectedPlaylist: LoadedPlaylist?,
    enrichmentQueue: XtreamCategoryEnrichmentQueue,
    onCatalogChanged: () -> Unit,
) {
    LaunchedEffect(restoredApplied, selectedPlaylist?.id) {
        val playlist = selectedPlaylist ?: return@LaunchedEffect
        if (!restoredApplied) return@LaunchedEffect
        delay(5_000L)
        enrichmentQueue.run(playlist) { onCatalogChanged() }
    }
}
