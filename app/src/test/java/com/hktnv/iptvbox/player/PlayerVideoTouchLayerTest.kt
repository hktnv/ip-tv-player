package com.hktnv.iptvbox.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerVideoTouchLayerTest {
    @Test
    fun videoTapShowsControlsWhenWatching() {
        assertEquals(
            PlayerInputState.ControlsVisible,
            playerInputStateAfterVideoTap(PlayerInputState.Watching),
        )
    }

    @Test
    fun videoTapHidesControlsWhenControlsAreVisible() {
        assertEquals(
            PlayerInputState.Watching,
            playerInputStateAfterVideoTap(PlayerInputState.ControlsVisible),
        )
    }

    @Test
    fun videoTapDoesNotChangeBlockingOverlays() {
        assertEquals(
            PlayerInputState.ContentListVisible,
            playerInputStateAfterVideoTap(PlayerInputState.ContentListVisible),
        )
        assertEquals(
            PlayerInputState.ExitConfirmVisible,
            playerInputStateAfterVideoTap(PlayerInputState.ExitConfirmVisible),
        )
    }
}
