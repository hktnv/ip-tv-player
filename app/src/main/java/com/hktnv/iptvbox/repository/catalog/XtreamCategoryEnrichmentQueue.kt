package com.hktnv.iptvbox.repository.catalog

import com.hktnv.iptvbox.model.LoadedPlaylist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class XtreamCategoryEnrichmentQueue(
    private val syncQueuedCategories: suspend (LoadedPlaylist, (String, String) -> Unit) -> Int,
    private val statusReporter: CatalogSyncStatusReporter = NoOpCatalogSyncStatusReporter,
) {
    private val mutex = Mutex()
    private val runningPlaylistIds = mutableSetOf<String>()

    fun start(
        scope: CoroutineScope,
        playlist: LoadedPlaylist,
        startupDelayMs: Long = 0L,
        onChanged: (Int) -> Unit = {},
    ): Job = scope.launch {
        if (startupDelayMs > 0L) delay(startupDelayMs)
        run(playlist, onChanged)
    }

    suspend fun run(
        playlist: LoadedPlaylist,
        onChanged: (Int) -> Unit = {},
    ) {
        if (!markRunning(playlist.id)) return
        statusReporter.markWaiting(playlist)
        try {
            val changed = syncQueuedCategories(playlist) { categoryName, categoryKind ->
                statusReporter.markActive(playlist, categoryName, categoryKind)
            }
            statusReporter.markCompleted(playlist, changed)
            if (changed > 0) onChanged(changed)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            statusReporter.markFailed(playlist, error.message)
        } finally {
            clearRunning(playlist.id)
        }
    }

    private suspend fun markRunning(playlistId: String): Boolean = mutex.withLock {
        runningPlaylistIds.add(playlistId)
    }

    private suspend fun clearRunning(playlistId: String) {
        mutex.withLock {
            runningPlaylistIds.remove(playlistId)
        }
    }
}
