package com.hktnv.iptvbox.repository.catalog

import com.hktnv.iptvbox.model.LoadedPlaylist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class XtreamCategoryEnrichmentQueue(
    private val syncQueuedCategories: suspend (LoadedPlaylist) -> Int,
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
        try {
            val changed = syncQueuedCategories(playlist)
            if (changed > 0) onChanged(changed)
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
