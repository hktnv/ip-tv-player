package com.hktnv.iptvbox.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDiagnosticGateTest {
    @Test
    fun diagnosticsOnlyRunInDebugBuilds() {
        assertTrue(playerDiagnosticEnabledFor(isDebugBuild = true))
        assertFalse(playerDiagnosticEnabledFor(isDebugBuild = false))
    }
}
