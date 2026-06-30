package com.hktnv.iptvbox.player

import androidx.media3.common.Player

internal data class BufferingDiagnosticEvent(
    val durationMs: Long,
    val count: Int,
    val totalDurationMs: Long,
)

internal class PlayerDiagnosticSession {
    private var bufferingStartedAtMs: Long? = null
    private var bufferingCount = 0
    private var totalBufferingMs = 0L

    fun onPlaybackStateChanged(
        state: Int,
        nowMs: Long,
    ): BufferingDiagnosticEvent? {
        if (state == Player.STATE_BUFFERING && bufferingStartedAtMs == null) {
            bufferingStartedAtMs = nowMs
            return null
        }
        if (state == Player.STATE_READY || state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
            val startedAt = bufferingStartedAtMs ?: return null
            val duration = (nowMs - startedAt).coerceAtLeast(0L)
            bufferingStartedAtMs = null
            bufferingCount += 1
            totalBufferingMs += duration
            return BufferingDiagnosticEvent(duration, bufferingCount, totalBufferingMs)
        }
        return null
    }
}
