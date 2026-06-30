package com.hktnv.iptvbox.core.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

internal data class IptvPlayerBufferProfile(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
    val backBufferMs: Int,
    val prioritizeTimeOverSizeThresholds: Boolean,
)

internal fun defaultIptvPlayerBufferProfile(): IptvPlayerBufferProfile {
    return IptvPlayerBufferProfile(
        minBufferMs = 30_000,
        maxBufferMs = 90_000,
        bufferForPlaybackMs = 1_500,
        bufferForPlaybackAfterRebufferMs = 8_000,
        backBufferMs = 15_000,
        prioritizeTimeOverSizeThresholds = false,
    )
}

@UnstableApi
internal fun IptvPlayerBufferProfile.toLoadControl(): DefaultLoadControl {
    return DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            minBufferMs,
            maxBufferMs,
            bufferForPlaybackMs,
            bufferForPlaybackAfterRebufferMs,
        )
        .setPrioritizeTimeOverSizeThresholds(prioritizeTimeOverSizeThresholds)
        .setBackBuffer(backBufferMs, true)
        .build()
}
