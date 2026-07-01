package com.hktnv.iptvbox

import com.hktnv.iptvbox.data.catalog.MetadataCleanupPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataCleanupPolicyTest {
    @Test
    fun cleanupStartsAfterTenSecondStartupDelay() {
        assertTrue(MetadataCleanupPolicy.StartupDelayMs == 10_000L)
    }

    @Test
    fun cleanupRunsWhenThereIsNoPreviousRun() {
        assertTrue(MetadataCleanupPolicy.shouldRun(nowMs = 1000L, lastRunAtMs = null))
    }

    @Test
    fun cleanupDoesNotRunBeforeWeeklyInterval() {
        val lastRun = 10_000L
        val sixDaysLater = lastRun + MetadataCleanupPolicy.WeeklyIntervalMs - 1L

        assertFalse(MetadataCleanupPolicy.shouldRun(sixDaysLater, lastRun))
    }

    @Test
    fun cleanupRunsAfterWeeklyInterval() {
        val lastRun = 10_000L
        val oneWeekLater = lastRun + MetadataCleanupPolicy.WeeklyIntervalMs

        assertTrue(MetadataCleanupPolicy.shouldRun(oneWeekLater, lastRun))
    }
}
