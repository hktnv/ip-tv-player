package com.hktnv.iptvbox

import androidx.media3.common.Player
import com.hktnv.iptvbox.player.PlayerTimelineKeyAction
import com.hktnv.iptvbox.player.PlayerTimelineRemoteKey
import com.hktnv.iptvbox.player.calculateSeekTarget
import com.hktnv.iptvbox.player.resolvePlayerTimelineKeyAction
import com.hktnv.iptvbox.player.shouldAutoHidePlayerOsd
import com.hktnv.iptvbox.player.shouldPresentAsPlaying
import com.hktnv.iptvbox.player.shouldShowBufferingIndicator
import com.hktnv.iptvbox.player.shouldShowPlayerLoadingIndicator
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
    fun loadingIndicatorIncludesSeekAndConnectionFeedback() {
        assertTrue(
            shouldShowPlayerLoadingIndicator(
                playbackState = Player.STATE_READY,
                manuallyPaused = false,
                connectionLoading = false,
                seekLoading = true,
            ),
        )
        assertTrue(
            shouldShowPlayerLoadingIndicator(
                playbackState = Player.STATE_READY,
                manuallyPaused = false,
                connectionLoading = true,
                seekLoading = false,
            ),
        )
        assertFalse(
            shouldShowPlayerLoadingIndicator(
                playbackState = Player.STATE_READY,
                manuallyPaused = false,
                connectionLoading = false,
                seekLoading = false,
            ),
        )
    }

    @Test
    fun timelineVerticalKeysExitInsteadOfSeeking() {
        assertEquals(
            PlayerTimelineKeyAction.ExitTimeline,
            resolvePlayerTimelineKeyAction(PlayerTimelineRemoteKey.Up, canSeek = true),
        )
        assertEquals(
            PlayerTimelineKeyAction.ExitTimeline,
            resolvePlayerTimelineKeyAction(PlayerTimelineRemoteKey.Down, canSeek = true),
        )
        assertEquals(
            PlayerTimelineKeyAction.PreviewBack,
            resolvePlayerTimelineKeyAction(PlayerTimelineRemoteKey.Left, canSeek = true),
        )
        assertEquals(
            PlayerTimelineKeyAction.PreviewForward,
            resolvePlayerTimelineKeyAction(PlayerTimelineRemoteKey.Right, canSeek = true),
        )
    }

    @Test
    fun timelineKeysAreIgnoredWhenSeekIsDisabled() {
        assertEquals(
            PlayerTimelineKeyAction.None,
            resolvePlayerTimelineKeyAction(PlayerTimelineRemoteKey.Up, canSeek = false),
        )
        assertEquals(
            PlayerTimelineKeyAction.None,
            resolvePlayerTimelineKeyAction(PlayerTimelineRemoteKey.Right, canSeek = false),
        )
    }

    @Test
    fun playingPresentationFollowsUserIntent() {
        assertTrue(shouldPresentAsPlaying(playWhenReady = true, manuallyPaused = false))
        assertFalse(shouldPresentAsPlaying(playWhenReady = true, manuallyPaused = true))
        assertFalse(shouldPresentAsPlaying(playWhenReady = false, manuallyPaused = false))
    }

    @Test
    fun osdAutoHideOnlyRunsWhileContentIsPlaying() {
        assertTrue(
            shouldAutoHidePlayerOsd(
                controlsVisible = true,
                playWhenReady = true,
                manuallyPaused = false,
            ),
        )
        assertFalse(
            shouldAutoHidePlayerOsd(
                controlsVisible = true,
                playWhenReady = true,
                manuallyPaused = true,
            ),
        )
        assertFalse(
            shouldAutoHidePlayerOsd(
                controlsVisible = true,
                playWhenReady = false,
                manuallyPaused = false,
            ),
        )
        assertFalse(
            shouldAutoHidePlayerOsd(
                controlsVisible = false,
                playWhenReady = true,
                manuallyPaused = false,
            ),
        )
    }
}
