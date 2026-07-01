package com.hktnv.iptvbox.player

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerMediaSwitchPlanTest {
    @Test
    fun resetsQueueAtTargetWhenOnlyCurrentIndexChanges() {
        val queue = PlayerMediaQueue(
            signature = "same-queue",
            currentIndex = 1,
            mediaItems = listOf(mediaItem("first"), mediaItem("second")),
        )

        val plan = choosePlayerMediaSwitchPlan(
            activeQueueSignature = "same-queue",
            playerMediaItemCount = 2,
            currentMediaItemIndex = 0,
            currentMediaId = "first",
            queue = queue,
        )

        assertEquals(PlayerMediaSwitchPlan.ResetQueueAtTarget, plan)
    }

    @Test
    fun reusesCurrentWhenIndexAndMediaIdMatch() {
        val queue = PlayerMediaQueue(
            signature = "same-queue",
            currentIndex = 0,
            mediaItems = listOf(mediaItem("first")),
        )

        val plan = choosePlayerMediaSwitchPlan(
            activeQueueSignature = "same-queue",
            playerMediaItemCount = 1,
            currentMediaItemIndex = 0,
            currentMediaId = "first",
            queue = queue,
        )

        assertEquals(PlayerMediaSwitchPlan.ReuseCurrent, plan)
    }

    @Test
    fun setsQueueWhenSignatureChanges() {
        val queue = PlayerMediaQueue(
            signature = "new-queue",
            currentIndex = 0,
            mediaItems = listOf(mediaItem("first")),
        )

        val plan = choosePlayerMediaSwitchPlan(
            activeQueueSignature = "old-queue",
            playerMediaItemCount = 1,
            currentMediaItemIndex = 0,
            currentMediaId = "first",
            queue = queue,
        )

        assertEquals(PlayerMediaSwitchPlan.SetQueue, plan)
    }

    private fun mediaItem(id: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .build()
    }
}
