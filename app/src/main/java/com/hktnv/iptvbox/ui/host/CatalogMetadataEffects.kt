package com.hktnv.iptvbox.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentMetadata
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.repository.catalog.XtreamLazySyncRepository

@Composable
internal fun CatalogMetadataEffects(
    selectedPlaylist: LoadedPlaylist?,
    selectedSeriesTitle: String?,
    catalogSnapshot: CatalogSnapshot?,
    catalogRefreshToken: Int,
    contentOptionsItem: CatalogItem?,
    lazySyncRepository: XtreamLazySyncRepository,
    onSeriesChanged: () -> Unit,
    onContentOptionsMetadata: (ContentMetadata?) -> Unit,
) {
    val syncedSeries = remember { mutableSetOf<String>() }
    LaunchedEffect(selectedPlaylist?.id, selectedSeriesTitle, catalogSnapshot, catalogRefreshToken) {
        val playlist = selectedPlaylist ?: return@LaunchedEffect
        val seriesTitle = selectedSeriesTitle ?: return@LaunchedEffect
        val representative = catalogSnapshot
            ?.episodes(seriesTitle, null)
            ?.firstOrNull { it.xtreamId != null }
            ?: return@LaunchedEffect
        val syncKey = "${playlist.id}|$seriesTitle"
        if (!syncedSeries.add(syncKey)) return@LaunchedEffect
        val changed = lazySyncRepository.syncSeries(playlist, seriesTitle, representative)
        if (changed) onSeriesChanged()
    }
    LaunchedEffect(contentOptionsItem?.id, selectedPlaylist?.id) {
        val item = contentOptionsItem ?: run {
            onContentOptionsMetadata(null)
            return@LaunchedEffect
        }
        onContentOptionsMetadata(lazySyncRepository.syncVodMetadata(selectedPlaylist, item))
    }
}
