package com.hktnv.iptvbox

import com.hktnv.iptvbox.player.PlayerBackPressGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerBackPressGuardTest {
    @Test
    fun duplicateBackAfterControlsCloseIsSuppressedWithinWindow() {
        val guard = PlayerBackPressGuard(duplicateWindowMs = 350L)

        guard.markOverlayBackHandled(nowMs = 1_000L)

        assertTrue(guard.shouldSuppressExitBack(nowMs = 1_050L))
        assertTrue(guard.shouldSuppressExitBack(nowMs = 1_060L))
        assertFalse(guard.shouldSuppressExitBack(nowMs = 1_351L))
    }

    @Test
    fun laterBackAfterControlsCloseCanOpenExitFlow() {
        val guard = PlayerBackPressGuard(duplicateWindowMs = 350L)

        guard.markOverlayBackHandled(nowMs = 1_000L)

        assertFalse(guard.shouldSuppressExitBack(nowMs = 1_400L))
    }
}
