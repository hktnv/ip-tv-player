package com.hktnv.iptvbox.core.player

import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IptvPlayerBufferProfileTest {
    @Test
    fun defaultProfileWaitsLongerAfterRebufferWithoutUnboundedMemoryGrowth() {
        val profile = defaultIptvPlayerBufferProfile()

        assertTrue(profile.bufferForPlaybackAfterRebufferMs > profile.bufferForPlaybackMs)
        assertTrue(profile.maxBufferMs <= 90_000)
        assertTrue(profile.minBufferMs >= 30_000)
        assertTrue(profile.backBufferMs <= 15_000)
        assertEquals(false, profile.prioritizeTimeOverSizeThresholds)
    }

    @Test
    fun blankPlaybackHeadersAreRemovedBeforeHttpPlayback() {
        val headers = cleanPlaybackHeaders(
            mapOf(
                "User-Agent" to "Demo",
                "Referer" to "",
                "" to "ignored",
            ),
        )

        assertEquals(mapOf("User-Agent" to "Demo"), headers)
    }

    @Test
    fun tsExtractorFlagsSupportIptvAccessUnitDetection() {
        val flags = iptvTsExtractorFlags()

        assertTrue(flags and DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS != 0)
        assertTrue(flags and DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES != 0)
    }
}
