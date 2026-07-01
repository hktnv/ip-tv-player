package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.repository.catalog.CatalogSyncStatusReporter
import com.hktnv.iptvbox.repository.catalog.XtreamCategoryEnrichmentQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamCategoryEnrichmentQueueTest {
    @Test
    fun skipsDuplicateTriggersWhilePlaylistQueueIsRunning() = runBlocking {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val runCount = AtomicInteger(0)
        val queue = XtreamCategoryEnrichmentQueue(syncQueuedCategories = { _, _ ->
            runCount.incrementAndGet()
            firstStarted.complete(Unit)
            releaseFirst.await()
            0
        })

        val firstJob = queue.start(this, playlist("playlist-a"))
        firstStarted.await()
        val duplicateJob = queue.start(this, playlist("playlist-a"))

        duplicateJob.join()
        assertEquals(1, runCount.get())

        releaseFirst.complete(Unit)
        firstJob.join()
    }

    @Test
    fun allowsPlaylistQueueToRunAgainAfterCompletion() = runBlocking {
        val runCount = AtomicInteger(0)
        val queue = XtreamCategoryEnrichmentQueue(syncQueuedCategories = { _, _ ->
            runCount.incrementAndGet()
            0
        })

        queue.start(this, playlist("playlist-a")).join()
        queue.start(this, playlist("playlist-a")).join()

        assertEquals(2, runCount.get())
    }

    @Test
    fun notifiesOnlyWhenQueuedCategorySyncChangesItems() = runBlocking {
        var refreshCount = 0
        val queue = XtreamCategoryEnrichmentQueue(syncQueuedCategories = { playlist, _ ->
            if (playlist.id == "changed") 3 else 0
        })

        queue.start(this, playlist("unchanged")) { refreshCount += 1 }.join()
        queue.start(this, playlist("changed")) { refreshCount += 1 }.join()

        assertEquals(1, refreshCount)
    }

    @Test
    fun reportsWaitingActiveAndCompletedStates() = runBlocking {
        val reporter = RecordingStatusReporter()
        val queue = XtreamCategoryEnrichmentQueue(
            syncQueuedCategories = { _, onCategory ->
                onCategory("Aksiyon Filmleri", "MOVIE")
                4
            },
            statusReporter = reporter,
        )

        queue.start(this, playlist("playlist-a")).join()

        assertEquals(
            listOf(
                "waiting:playlist-a",
                "active:playlist-a:Aksiyon Filmleri:MOVIE",
                "completed:playlist-a:4",
            ),
            reporter.events,
        )
    }

    private fun playlist(id: String): LoadedPlaylist {
        return LoadedPlaylist(
            id = id,
            name = "Test list",
            type = PlaylistSourceType.M3U_URL,
            endpoint = "http://example.com/get.php?username=u&password=p&type=m3u_plus",
            headers = emptyMap(),
            items = emptyList(),
            epgUrls = emptyList(),
            warnings = emptyList(),
        )
    }

    private class RecordingStatusReporter : CatalogSyncStatusReporter {
        val events = mutableListOf<String>()

        override fun markWaiting(playlist: LoadedPlaylist) {
            events += "waiting:${playlist.id}"
        }

        override fun markActive(playlist: LoadedPlaylist, categoryName: String, categoryKind: String) {
            events += "active:${playlist.id}:$categoryName:$categoryKind"
        }

        override fun markCompleted(playlist: LoadedPlaylist, changedItemCount: Int) {
            events += "completed:${playlist.id}:$changedItemCount"
        }

        override fun markFailed(playlist: LoadedPlaylist, message: String?) {
            events += "failed:${playlist.id}:$message"
        }
    }
}
