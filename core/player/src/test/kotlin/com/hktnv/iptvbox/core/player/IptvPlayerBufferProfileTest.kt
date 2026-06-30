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
        assertTrue(profile.bufferForPlaybackMs >= 4_000)
        assertTrue(profile.bufferForPlaybackAfterRebufferMs >= 12_000)
        assertTrue(profile.maxBufferMs <= 120_000)
        assertTrue(profile.minBufferMs >= 45_000)
        assertTrue(profile.backBufferMs <= 5_000)
        assertEquals(true, profile.prioritizeTimeOverSizeThresholds)
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
        assertTrue(flags and DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM != 0)
        assertEquals(64 * 1024, IPTV_TS_TIMESTAMP_SEARCH_BYTES)
    }
}
