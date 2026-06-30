package com.hktnv.iptvbox

import android.view.KeyEvent
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.player.PlayerRemoteCommand
import com.hktnv.iptvbox.player.buildPlayerPlaybackQueue
import com.hktnv.iptvbox.player.playerRemoteCommandForKeyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerPlaybackQueueTest {
    @Test
    fun buildsQueueWithPreviousAndNextItems() {
        val first = item("1")
        val second = item("2")
        val third = item("3")
        val queue = buildPlayerPlaybackQueue(
            contextItems = listOf(first, second, third),
            currentItem = second,
        )

        assertEquals(second, queue.current)
        assertEquals(first, queue.previous)
        assertEquals(third, queue.next)
    }

    @Test
    fun keepsCurrentItemWhenContextDoesNotContainIt() {
        val current = item("current")
        val queue = buildPlayerPlaybackQueue(
            contextItems = listOf(item("other")),
            currentItem = current,
        )

        assertEquals(current, queue.current)
    }

    @Test
    fun keepsBoundsSafeAtQueueEdges() {
        val first = item("1")
        val second = item("2")

        val firstQueue = buildPlayerPlaybackQueue(listOf(first, second), first)
        val lastQueue = buildPlayerPlaybackQueue(listOf(first, second), second)

        assertNull(firstQueue.previous)
        assertEquals(second, firstQueue.next)
        assertEquals(first, lastQueue.previous)
        assertNull(lastQueue.next)
    }

    @Test
    fun filtersQueueToCurrentPlaybackContext() {
        val firstEpisode = episode("episode-1", "Dune", "Dune S1 E1")
        val secondEpisode = episode("episode-2", "Dune", "Dune S1 E2")
        val otherSeries = episode("episode-3", "Prens", "Prens S1 E1")
        val liveChannel = item("live-1")
        val movie = movie("movie-1")

        val queue = buildPlayerPlaybackQueue(
            contextItems = listOf(firstEpisode, liveChannel, secondEpisode, movie, otherSeries),
            currentItem = firstEpisode,
        )

        assertEquals(firstEpisode, queue.current)
        assertNull(queue.previous)
        assertEquals(secondEpisode, queue.next)
        assertEquals(listOf(firstEpisode, secondEpisode), queue.items)
    }

    @Test
    fun mapsTvRemoteKeysToPlayerCommands() {
        assertEquals(
            PlayerRemoteCommand.TogglePlayPause,
            playerRemoteCommandForKeyCode(KeyEvent.KEYCODE_DPAD_CENTER),
        )
        assertEquals(
            PlayerRemoteCommand.NextItem,
            playerRemoteCommandForKeyCode(KeyEvent.KEYCODE_DPAD_UP),
        )
        assertEquals(
            PlayerRemoteCommand.PreviousItem,
            playerRemoteCommandForKeyCode(KeyEvent.KEYCODE_DPAD_DOWN),
        )
        assertEquals(
            PlayerRemoteCommand.OpenContentList,
            playerRemoteCommandForKeyCode(KeyEvent.KEYCODE_DPAD_LEFT),
        )
    }

    private fun item(id: String): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.LIVE_CHANNEL,
            title = "Kanal $id",
            streamUrl = "https://example.test/$id",
            category = "Test",
        )
    }

    private fun movie(id: String): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.MOVIE,
            title = "Film $id",
            streamUrl = "https://example.test/$id",
            category = "Film",
        )
    }

    private fun episode(
        id: String,
        seriesTitle: String,
        title: String,
    ): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.EPISODE,
            title = title,
            streamUrl = "https://example.test/$id",
            category = "Dizi",
            seriesTitle = seriesTitle,
        )
    }
}
