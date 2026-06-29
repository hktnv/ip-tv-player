package com.hktnv.iptvbox

import com.hktnv.iptvbox.player.PlayerExitAction
import com.hktnv.iptvbox.player.PlayerExitDialogState
import com.hktnv.iptvbox.player.reducePlayerExitDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerExitStateTest {
    @Test
    fun firstBackPressShowsExitDialogWithoutLeavingPlayer() {
        val result = reducePlayerExitDialog(
            state = PlayerExitDialogState.Hidden,
            action = PlayerExitAction.BackPressed,
        )

        assertEquals(PlayerExitDialogState.Visible, result.state)
        assertFalse(result.exitRequested)
    }

    @Test
    fun secondBackPressFromDialogRequestsExit() {
        val result = reducePlayerExitDialog(
            state = PlayerExitDialogState.Visible,
            action = PlayerExitAction.BackPressed,
        )

        assertEquals(PlayerExitDialogState.Hidden, result.state)
        assertTrue(result.exitRequested)
    }

    @Test
    fun continueClosesDialogAndKeepsPlayback() {
        val result = reducePlayerExitDialog(
            state = PlayerExitDialogState.Visible,
            action = PlayerExitAction.ContinueSelected,
        )

        assertEquals(PlayerExitDialogState.Hidden, result.state)
        assertFalse(result.exitRequested)
    }

    @Test
    fun exitActionRequestsNavigationBack() {
        val result = reducePlayerExitDialog(
            state = PlayerExitDialogState.Visible,
            action = PlayerExitAction.ExitSelected,
        )

        assertEquals(PlayerExitDialogState.Hidden, result.state)
        assertTrue(result.exitRequested)
    }
}
