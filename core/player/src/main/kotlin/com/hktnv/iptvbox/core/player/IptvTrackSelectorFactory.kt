package com.hktnv.iptvbox.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@UnstableApi
internal fun createIptvTrackSelector(context: Context): DefaultTrackSelector {
    return DefaultTrackSelector(context).apply {
        setParameters(
            buildUponParameters()
                .setTunnelingEnabled(true)
                .setViewportSizeToPhysicalDisplaySize(true),
        )
    }
}
