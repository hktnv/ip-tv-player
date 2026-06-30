package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlayerMediaQueueTest {
    @Test
    fun mediaQueueSignatureKeepsItemOrderAndUrls() {
        val queue = PlayerPlaybackQueue(
            items = listOf(item("one"), item("two"), item("three")),
            currentIndex = 1,
        )
        val sameItemsDifferentIndex = queue.copy(currentIndex = 2)

        assertEquals(1, queue.currentIndex)
        assertEquals(queue.toMediaQueueSignature(), sameItemsDifferentIndex.toMediaQueueSignature())
        assertNotEquals("", queue.toMediaQueueSignature())
    }

    @Test
    fun mediaQueueSignatureChangesWhenUrlChanges() {
        val first = PlayerPlaybackQueue(
            items = listOf(item("one", url = "https://example.test/one")),
            currentIndex = 0,
        )
        val second = PlayerPlaybackQueue(
            items = listOf(item("one", url = "https://example.test/other")),
            currentIndex = 0,
        )

        assertNotEquals(first.toMediaQueueSignature(), second.toMediaQueueSignature())
    }

    @Test
    fun mediaQueueSignatureChangesWhenOrderChanges() {
        val first = PlayerPlaybackQueue(
            items = listOf(item("one"), item("two")),
            currentIndex = 0,
        )
        val second = PlayerPlaybackQueue(
            items = listOf(item("two"), item("one")),
            currentIndex = 0,
        )

        assertNotEquals(first.toMediaQueueSignature(), second.toMediaQueueSignature())
    }

    private fun item(
        id: String,
        url: String = "https://example.test/$id",
    ): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = ContentKind.LIVE_CHANNEL,
            title = id,
            streamUrl = url,
        )
    }
}
