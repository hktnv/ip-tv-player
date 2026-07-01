package com.hktnv.iptvbox.state
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.withFrameNanos
import com.hktnv.iptvbox.core.model.ContentHint
import com.hktnv.iptvbox.data.playlist.CreatePlaylistSourceRequest
import com.hktnv.iptvbox.data.playlist.PlaylistLoadResult
import com.hktnv.iptvbox.data.playlist.RemotePlaylistLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hktnv.iptvbox.data.catalog.CatalogStore
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistImportProgress
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.telemetry.finishPlaylistImportTelemetry
import com.hktnv.iptvbox.telemetry.recordCatalogWriteTimings
import com.hktnv.iptvbox.telemetry.recordPlaylistLoadMetrics
import com.hktnv.iptvbox.ui.media.simpleUserMessage
import com.hktnv.iptvbox.ui.playlist.DraftPlaylist
import com.hktnv.iptvbox.ui.playlist.resolvedPlaylistName
import com.hktnv.iptvbox.update.AppUpdateInfo
import com.hktnv.iptvbox.update.AppUpdateInstaller
import com.hktnv.iptvbox.update.AppUpdateService
import com.hktnv.iptvbox.update.AppUpdateUiState

internal fun CoroutineScope.reloadPlaylistAction(
    playlist: LoadedPlaylist,
    loader: RemotePlaylistLoader,
    telemetry: AppPerformanceTelemetry,
    catalogStore: CatalogStore,
    onBanner: (String) -> Unit,
    onProgress: (PlaylistImportProgress?) -> Unit,
    onStored: (LoadedPlaylist) -> Unit,
) {
    onBanner("${playlist.name} yenileniyor")
    launch {
        val importStartedAtMs = SystemClock.elapsedRealtime()
        val expectedItemCount = playlist.cachedItemCount ?: playlist.items.size.takeIf { it > 0 }
        onProgress(
            PlaylistImportProgress(
                playlistId = playlist.id,
                message = "İndiriliyor",
                totalItems = expectedItemCount,
            ),
        )
        val request = CreatePlaylistSourceRequest(
            type = playlist.type,
            name = playlist.name,
            endpoint = playlist.endpoint,
            headers = playlist.headers,
            contentHint = ContentHint.AUTO,
        )
        runCatching {
            loader.load(
                playlistId = playlist.id,
                request = request,
                expectedItemCount = expectedItemCount,
            ) { progress ->
                launch { onProgress(progress.toPlaylistImportProgress(playlist.id)) }
            }
        }
            .onSuccess { result ->
                recordPlaylistLoadMetrics(telemetry, result.metrics)
                telemetry.record("playlist_import_image_ms", 0L)
                val normalizeStartedAt = SystemClock.elapsedRealtime()
                onProgress(
                    PlaylistImportProgress(
                        playlistId = playlist.id,
                        message = "Katalog hazırlanıyor",
                        processedItems = result.items.size,
                        totalItems = result.items.size,
                    ),
                )
                val loaded = withContext(Dispatchers.Default) {
                    playlist.copy(
                        items = result.items,
                        epgUrls = result.epgUrls,
                        warnings = result.warnings.map(::simpleUserMessage).filter { it.isNotBlank() }.distinct().take(1),
                        xtreamApiSupported = result.xtreamApiSupported,
                    )
                }
                val normalizeMs = SystemClock.elapsedRealtime() - normalizeStartedAt
                val writeResult = withContext(Dispatchers.IO) {
                    catalogStore.replacePlaylistMeasured(loaded, result.xtreamCategoryMappings)
                }
                recordCatalogWriteTimings(telemetry, writeResult.timings)
                val uiStartedAt = SystemClock.elapsedRealtime()
                onStored(writeResult.playlist)
                withFrameNanos { }
                val uiUpdateMs = SystemClock.elapsedRealtime() - uiStartedAt
                finishPlaylistImportTelemetry(
                    telemetry = telemetry,
                    importStartedAtMs = importStartedAtMs,
                    firstResponseMs = 0L,
                    metrics = result.metrics,
                    dbTimings = writeResult.timings,
                    normalizeMs = normalizeMs,
                    uiUpdateMs = uiUpdateMs,
                    itemCount = result.items.size,
                )
                telemetry.record("playlist_import_item_count", result.items.size.toLong())
                onProgress(
                    PlaylistImportProgress(
                        playlistId = playlist.id,
                        message = "Tamamlandı",
                        processedItems = result.items.size,
                        totalItems = result.items.size,
                        complete = true,
                    ),
                )
                onBanner("${playlist.name} yenilendi: ${result.items.size} içerik")
            }
            .onFailure { throwable ->
                val message = simpleUserMessage(throwable.message.orEmpty()).ifBlank { "Liste yüklenemedi" }
                onProgress(
                    PlaylistImportProgress(
                        playlistId = playlist.id,
                        message = "Liste yüklenemedi",
                        totalItems = expectedItemCount,
                        error = message,
                    ),
                )
                telemetry.recordError("Liste yenileme hatası", throwable)
                onBanner(simpleUserMessage(throwable.message.orEmpty()).ifBlank { "Liste yüklenemedi" })
            }
    }
}

internal fun CoroutineScope.saveLoadedPlaylistAction(
    draft: DraftPlaylist,
    result: PlaylistLoadResult,
    telemetry: AppPerformanceTelemetry,
    catalogStore: CatalogStore,
    onSaving: () -> Unit,
    onStored: (LoadedPlaylist, Int, String) -> Unit,
    onFailure: (String) -> Unit,
) {
    onSaving()
    launch {
        runCatching {
            val normalizeStartedAt = SystemClock.elapsedRealtime()
            val playlist = withContext(Dispatchers.Default) {
                LoadedPlaylist(
                    id = draft.id,
                    name = draft.name,
                    type = draft.type,
                    endpoint = draft.endpoint,
                    headers = draft.headers,
                    items = result.items,
                    epgUrls = result.epgUrls,
                    warnings = result.warnings.map(::simpleUserMessage).filter { it.isNotBlank() }.distinct().take(1),
                    xtreamApiSupported = result.xtreamApiSupported,
                )
            }
            val normalizeMs = SystemClock.elapsedRealtime() - normalizeStartedAt
            val writeResult = withContext(Dispatchers.IO) {
                catalogStore.replacePlaylistMeasured(playlist, result.xtreamCategoryMappings)
            }
            val uiStartedAt = SystemClock.elapsedRealtime()
            onStored(writeResult.playlist, result.items.size, playlist.name)
            withFrameNanos { }
            val uiUpdateMs = SystemClock.elapsedRealtime() - uiStartedAt
            finishPlaylistImportTelemetry(
                telemetry = telemetry,
                importStartedAtMs = draft.loadStartedAtMs,
                firstResponseMs = draft.firstResponseMs,
                metrics = result.metrics,
                dbTimings = writeResult.timings,
                normalizeMs = normalizeMs,
                uiUpdateMs = uiUpdateMs,
                itemCount = result.items.size,
            )
        }.onFailure { throwable ->
            telemetry.recordError("Oynatma listesi kaydetme hatası", throwable)
            onFailure(simpleUserMessage(throwable.message.orEmpty()).ifBlank { "Liste kaydedilemedi" })
        }
        telemetry.endUiWatch(draft.uiWatchId)
    }
}

internal fun CoroutineScope.startUpdateDownloadAction(
    context: Context,
    update: AppUpdateInfo,
    updateService: AppUpdateService,
    updateInstaller: AppUpdateInstaller,
    telemetry: AppPerformanceTelemetry,
    onUpdateState: (AppUpdateUiState) -> Unit,
) {
    launch {
        onUpdateState(AppUpdateUiState.Downloading(update, progress = null))
        runCatching {
            updateService.downloadVerifiedApk(
                cacheDir = context.cacheDir,
                release = update.release,
                onProgress = { progress -> onUpdateState(AppUpdateUiState.Downloading(update, progress)) },
            )
        }.onSuccess { apkFile ->
            if (updateInstaller.canInstallPackages()) {
                updateInstaller.openInstaller(apkFile)
                onUpdateState(AppUpdateUiState.Hidden)
            } else {
                onUpdateState(AppUpdateUiState.PermissionRequired(update, apkFile))
            }
        }.onFailure { throwable ->
            telemetry.recordError("Güncelleme indirme hatası", throwable)
            onUpdateState(
                AppUpdateUiState.Error(
                    message = simpleUserMessage(throwable.message.orEmpty()).ifBlank {
                        "Güncelleme indirilemedi."
                    },
                    required = false,
                ),
            )
        }
    }
}

internal fun CoroutineScope.renamePlaylistAction(
    playlist: LoadedPlaylist,
    requestedName: String,
    playlists: List<LoadedPlaylist>,
    catalogStore: CatalogStore,
    onUpdated: (LoadedPlaylist) -> Unit,
) {
    val finalName = resolvedPlaylistName(
        requestedName = requestedName,
        type = playlist.type,
        endpoint = playlist.endpoint,
        existingNames = playlists.filterNot { it.id == playlist.id }.map { it.name },
    )
    launch {
        val updated = withContext(Dispatchers.IO) {
            catalogStore.updatePlaylistName(playlist.id, finalName)
        } ?: playlist.copy(name = finalName)
        onUpdated(updated)
    }
}

internal fun CoroutineScope.updatePlaylistAutoRefreshAction(
    playlist: LoadedPlaylist,
    hours: Int,
    catalogStore: CatalogStore,
    onUpdated: (LoadedPlaylist) -> Unit,
) {
    launch {
        val updated = withContext(Dispatchers.IO) {
            catalogStore.updatePlaylistAutoUpdateHours(playlist.id, hours)
        } ?: playlist.copy(autoUpdateHours = hours.coerceAtLeast(0))
        onUpdated(updated)
    }
}

internal data class DeletedPlaylist(
    val id: String,
    val name: String,
    val itemIds: Set<String>,
)

internal fun CoroutineScope.deletePlaylistAction(
    playlist: LoadedPlaylist,
    catalogStore: CatalogStore,
    onDeleted: (DeletedPlaylist) -> Unit,
    onFailure: (String) -> Unit,
) {
    launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val itemIds = catalogStore.itemIds(playlist.id)
                catalogStore.deletePlaylist(playlist.id)
                DeletedPlaylist(id = playlist.id, name = playlist.name, itemIds = itemIds)
            }
        }.onSuccess(onDeleted).onFailure { throwable ->
            onFailure(simpleUserMessage(throwable.message.orEmpty()).ifBlank { "Oynatma listesi silinemedi" })
        }
    }
}

internal fun CoroutineScope.clearBrokenStateAction(
    stateStore: AppStateStore,
    onCleared: () -> Unit,
) {
    launch {
        withContext(Dispatchers.IO) { stateStore.clear() }
        onCleared()
    }
}
