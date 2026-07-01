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
        assertTrue(watchingResult.consumeInput)
        assertTrue(controlsResult.consumeInput)
        assertTrue(controlsResult.seekBack)
        assertFalse(controlsResult.togglePlayback)
        assertFalse(controlsResult.selectNextItem)
        assertFalse(controlsResult.selectPreviousItem)
    }

    @Test
    fun okShowsControlsAndTogglesPlaybackOnlyWhileWatching() {
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
        assertTrue(watchingResult.togglePlayback)
        assertFalse(controlsResult.consumeInput)
        assertFalse(controlsResult.showControls)
        assertFalse(controlsResult.togglePlayback)
    }

    @Test
    fun backClosesListBeforeShowingExitConfirmation() {
        val listResult = reducePlayerInput(
            state = PlayerInputState.ContentListVisible,
            action = PlayerInputAction.BackPressed,
        )
        val controlsResult = reducePlayerInput(
            state = PlayerInputState.ControlsVisible,
            action = PlayerInputAction.BackPressed,
        )
        val watchingResult = reducePlayerInput(
            state = PlayerInputState.Watching,
            action = PlayerInputAction.BackPressed,
        )

        assertEquals(PlayerInputState.Watching, listResult.state)
        assertEquals(PlayerInputState.Watching, controlsResult.state)
        assertFalse(listResult.exitRequested)
        assertFalse(controlsResult.exitRequested)
        assertEquals(PlayerInputState.ExitConfirmVisible, watchingResult.state)
        assertFalse(watchingResult.exitRequested)
    }

    @Test
    fun rightSeeksForwardWhenControlsAreVisible() {
        val watchingResult = reducePlayerInput(
            state = PlayerInputState.Watching,
            action = PlayerInputAction.RightPressed,
        )
        val controlsResult = reducePlayerInput(
            state = PlayerInputState.ControlsVisible,
            action = PlayerInputAction.RightPressed,
        )

        assertEquals(PlayerInputState.ControlsVisible, watchingResult.state)
        assertTrue(watchingResult.showControls)
        assertFalse(watchingResult.seekForward)
        assertEquals(PlayerInputState.ControlsVisible, controlsResult.state)
        assertTrue(controlsResult.seekForward)
    }

    @Test
    fun controllerHiddenReturnsToWatching() {
        val result = reducePlayerInput(
            state = PlayerInputState.ControlsVisible,
            action = PlayerInputAction.ControllerHidden,
        )

        assertEquals(PlayerInputState.Watching, result.state)
        assertFalse(result.consumeInput)
    }

    @Test
    fun upAndDownMoveQueueWithoutOpeningControlsWhileWatching() {
        val nextResult = reducePlayerInput(
            state = PlayerInputState.Watching,
            action = PlayerInputAction.UpPressed,
        )
        val previousResult = reducePlayerInput(
            state = PlayerInputState.ControlsVisible,
            action = PlayerInputAction.DownPressed,
        )

        assertEquals(PlayerInputState.Watching, nextResult.state)
        assertEquals(PlayerInputState.ControlsVisible, previousResult.state)
        assertFalse(nextResult.showControls)
        assertTrue(nextResult.showZappingInfo)
        assertTrue(nextResult.selectNextItem)
        assertFalse(previousResult.consumeInput)
        assertFalse(previousResult.showControls)
        assertFalse(previousResult.showZappingInfo)
        assertFalse(previousResult.selectPreviousItem)
    }
}
