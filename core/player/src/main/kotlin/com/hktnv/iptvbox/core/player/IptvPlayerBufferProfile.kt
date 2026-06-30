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
        minBufferMs = 45_000,
        maxBufferMs = 120_000,
        bufferForPlaybackMs = 4_000,
        bufferForPlaybackAfterRebufferMs = 12_000,
        backBufferMs = 5_000,
        prioritizeTimeOverSizeThresholds = true,
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
