package com.hktnv.iptvbox.core.player

import org.junit.Assert.assertTrue
import org.junit.Test

class IptvRenderersFactoryTest {
    @Test
    fun lateDecoderInputDropThresholdStaysInsideFramePacingRange() {
        assertTrue(LATE_DECODER_INPUT_DROP_THRESHOLD_US in 16_000L..100_000L)
    }
}
