package com.hktnv.iptvbox

import com.hktnv.iptvbox.player.PlayerInputAction
import com.hktnv.iptvbox.player.PlayerInputState
import com.hktnv.iptvbox.player.reducePlayerInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerInputStateTest {
    @Test
    fun leftOpensContentListOnlyWhileWatching() {
        val watchingResult = reducePlayerInput(
            state = PlayerInputState.Watching,
            action = PlayerInputAction.LeftPressed,
        )
        val controlsResult = reducePlayerInput(
            state = PlayerInputState.ControlsVisible,
            action = PlayerInputAction.LeftPressed,
        )

        assertEquals(PlayerInputState.ContentListVisible, watchingResult.state)
        assertEquals(PlayerInputState.ControlsVisible, controlsResult.state)
        assertFalse(controlsResult.togglePlayback)
        assertFalse(controlsResult.selectNextItem)
        assertFalse(controlsResult.selectPreviousItem)
    }

    @Test
    fun okAlwaysShowsControlsAndTogglesPlaybackInPlaybackStates() {
        val watchingResult = reducePlayerInput(
            state = PlayerInputState.Watching,
            action = PlayerInputAction.OkPressed,
        )
        val controlsResult = reducePlayerInput(
            state = PlayerInputState.ControlsVisible,
            action = PlayerInputAction.OkPressed,
        )

        assertEquals(PlayerInputState.ControlsVisible, watchingResult.state)
        assertEquals(PlayerInputState.ControlsVisible, controlsResult.state)
        assertTrue(watchingResult.showControls)
        assertTrue(controlsResult.showControls)
        assertTrue(watchingResult.togglePlayback)
        assertTrue(controlsResult.togglePlayback)
    }

    @Test
    fun backClosesListBeforeShowingExitConfirmation() {
        val listResult = reducePlayerInput(
            state = PlayerInputState.ContentListVisible,
            action = PlayerInputAction.BackPressed,
        )
        val watchingResult = reducePlayerInput(
            state = PlayerInputState.Watching,
            action = PlayerInputAction.BackPressed,
        )

        assertEquals(PlayerInputState.Watching, listResult.state)
        assertFalse(listResult.exitRequested)
        assertEquals(PlayerInputState.ExitConfirmVisible, watchingResult.state)
        assertFalse(watchingResult.exitRequested)
    }

    @Test
    fun upAndDownKeepControlsVisibleAndRequestQueueMove() {
        val nextResult = reducePlayerInput(
            state = PlayerInputState.Watching,
            action = PlayerInputAction.UpPressed,
        )
        val previousResult = reducePlayerInput(
            state = PlayerInputState.ControlsVisible,
            action = PlayerInputAction.DownPressed,
        )

        assertEquals(PlayerInputState.ControlsVisible, nextResult.state)
        assertEquals(PlayerInputState.ControlsVisible, previousResult.state)
        assertTrue(nextResult.showControls)
        assertTrue(previousResult.showControls)
        assertTrue(nextResult.selectNextItem)
        assertTrue(previousResult.selectPreviousItem)
    }
}
