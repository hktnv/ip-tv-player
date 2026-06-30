package com.hktnv.iptvbox.player

import android.content.res.Configuration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOrientationLockTest {
    @Test
    fun mobileAndTabletLockToLandscape() {
        assertTrue(shouldLockPlayerToLandscape(Configuration.UI_MODE_TYPE_NORMAL))
    }

    @Test
    fun televisionDoesNotForceLandscape() {
        assertFalse(shouldLockPlayerToLandscape(Configuration.UI_MODE_TYPE_TELEVISION))
    }
}
