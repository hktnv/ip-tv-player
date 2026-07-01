package com.hktnv.iptvbox.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerUiModeTest {
    @Test
    fun unknownPreferenceFallsBackToCustomOsd() {
        assertEquals(PlayerUiMode.CustomOsd, PlayerUiMode.fromPreferenceValue("missing"))
    }

    @Test
    fun storedStandardPreferenceFallsBackToCustomOsd() {
        assertEquals(
            PlayerUiMode.CustomOsd,
            PlayerUiMode.fromPreferenceValue("standard_media3"),
        )
    }
}
