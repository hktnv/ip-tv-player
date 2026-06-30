package com.hktnv.iptvbox

import androidx.media3.common.Player
import com.hktnv.iptvbox.player.calculateSeekTarget
import com.hktnv.iptvbox.player.shouldPresentAsPlaying
import com.hktnv.iptvbox.player.shouldShowBufferingIndicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlaybackUiStateTest {
    @Test
    fun seekTargetIsClampedToMediaBounds() {
        assertEquals(0L, calculateSeekTarget(positionMs = 5_000L, durationMs = 60_000L, deltaMs = -10_000L))
        assertEquals(60_000L, calculateSeekTarget(positionMs = 55_000L, durationMs = 60_000L, deltaMs = 10_000L))
        assertEquals(25_000L, calculateSeekTarget(positionMs = 15_000L, durationMs = 60_000L, deltaMs = 10_000L))
        assertEquals(0L, calculateSeekTarget(positionMs = 15_000L, durationMs = 0L, deltaMs = 10_000L))
    }

    @Test
    fun bufferingIndicatorIgnoresManualPause() {
        assertTrue(shouldShowBufferingIndicator(Player.STATE_BUFFERING, manuallyPaused = false))
        assertFalse(shouldShowBufferingIndicator(Player.STATE_BUFFERING, manuallyPaused = true))
        assertFalse(shouldShowBufferingIndicator(Player.STATE_READY, manuallyPaused = false))
    }

    @Test
    fun playingPresentationFollowsUserIntent() {
        assertTrue(shouldPresentAsPlaying(playWhenReady = true, manuallyPaused = false))
        assertFalse(shouldPresentAsPlaying(playWhenReady = true, manuallyPaused = true))
        assertFalse(shouldPresentAsPlaying(playWhenReady = false, manuallyPaused = false))
    }
}
