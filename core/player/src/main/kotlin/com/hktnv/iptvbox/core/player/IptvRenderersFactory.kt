package com.hktnv.iptvbox.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory

private const val VIDEO_JOINING_TIME_MS = 5_000L

@UnstableApi
internal fun createIptvRenderersFactory(context: Context): DefaultRenderersFactory {
    return DefaultRenderersFactory(context)
        .setAllowedVideoJoiningTimeMs(VIDEO_JOINING_TIME_MS)
        .setEnableDecoderFallback(true)
}
