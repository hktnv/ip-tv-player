package com.hktnv.iptvbox

import com.hktnv.iptvbox.player.playerConnectionTimeoutUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerConnectionTimeoutStateTest {
    @Test
    fun firstFourSecondsShowOnlySpinner() {
        val state = playerConnectionTimeoutUiState(
            awaitingConnection = true,
            elapsedMs = 3_999L,
            timeoutDismissed = false,
        )

        assertTrue(state.showLoading)
        assertNull(state.message)
        assertFalse(state.showTimeoutDialog)
    }

    @Test
    fun slowSourceMessageAppearsAfterFourSeconds() {
        val state = playerConnectionTimeoutUiState(
            awaitingConnection = true,
            elapsedMs = 4_000L,
            timeoutDismissed = false,
        )

        assertEquals("Yayın kaynağı yavaş yanıt veriyor, bağlanmaya çalışıyoruz...", state.message)
        assertFalse(state.showTimeoutDialog)
    }

    @Test
    fun busyServerMessageAppearsAfterEightSeconds() {
        val state = playerConnectionTimeoutUiState(
            awaitingConnection = true,
            elapsedMs = 8_000L,
            timeoutDismissed = false,
        )

        assertEquals("Yayın sunucusu yoğun olabilir, bağlantı sürdürülüyor...", state.message)
        assertFalse(state.showTimeoutDialog)
    }

    @Test
    fun timeoutDialogAppearsAfterFifteenSeconds() {
        val state = playerConnectionTimeoutUiState(
            awaitingConnection = true,
            elapsedMs = 15_000L,
            timeoutDismissed = false,
        )

        assertTrue(state.showLoading)
        assertTrue(state.showTimeoutDialog)
    }

    @Test
    fun dismissKeepsLoadingButSuppressesTimeoutDialog() {
        val state = playerConnectionTimeoutUiState(
            awaitingConnection = true,
            elapsedMs = 20_000L,
            timeoutDismissed = true,
        )

        assertTrue(state.showLoading)
        assertFalse(state.showTimeoutDialog)
    }

    @Test
    fun connectedPlaybackHidesConnectionUi() {
        val state = playerConnectionTimeoutUiState(
            awaitingConnection = false,
            elapsedMs = 20_000L,
            timeoutDismissed = false,
        )

        assertFalse(state.showLoading)
        assertNull(state.message)
        assertFalse(state.showTimeoutDialog)
    }
}
