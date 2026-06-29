package com.hktnv.iptvbox

import com.hktnv.iptvbox.ui.common.TvLongPressClickGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvLongPressClickGuardTest {
    @Test
    fun longPressSuppressesClickInTheSameInputCycle() {
        val guard = TvLongPressClickGuard(suppressWindowMs = 900L)

        guard.markLongClick(nowMs = 1_000L)

        assertFalse(guard.consumeClick(nowMs = 1_200L))
        assertFalse(guard.consumeClick(nowMs = 1_500L))
        assertTrue(guard.consumeClick(nowMs = 2_000L))
    }

    @Test
    fun normalClickIsAllowedWithoutLongPress() {
        val guard = TvLongPressClickGuard()

        assertTrue(guard.consumeClick(nowMs = 1_000L))
    }
}
