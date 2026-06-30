package com.hktnv.iptvbox.player

import androidx.media3.common.Player

internal data class BufferingDiagnosticEvent(
    val durationMs: Long,
    val count: Int,
    val totalDurationMs: Long,
)

internal data class DroppedFrameDiagnosticEvent(
    val count: Int,
    val elapsedMs: Long,
    val windowMs: Long,
)

internal class PlayerDiagnosticSession {
    private var bufferingStartedAtMs: Long? = null
    private var bufferingCount = 0
    private var totalBufferingMs = 0L
    private var droppedFrameWindowStartedAtMs: Long? = null
    private var droppedFrameCount = 0
    private var droppedFrameElapsedMs = 0L

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

    fun onDroppedVideoFrames(
        droppedFrames: Int,
        elapsedMs: Long,
        nowMs: Long,
        minWindowMs: Long = DROPPED_FRAME_LOG_WINDOW_MS,
    ): DroppedFrameDiagnosticEvent? {
        val windowStart = droppedFrameWindowStartedAtMs ?: nowMs.also {
            droppedFrameWindowStartedAtMs = it
        }
        droppedFrameCount += droppedFrames
        droppedFrameElapsedMs += elapsedMs
        val windowMs = (nowMs - windowStart).coerceAtLeast(0L)
        if (windowMs < minWindowMs && droppedFrameCount < DROPPED_FRAME_URGENT_COUNT) {
            return null
        }
        return consumeDroppedFrames(windowMs)
    }

    fun flushDroppedVideoFrames(nowMs: Long): DroppedFrameDiagnosticEvent? {
        val windowStart = droppedFrameWindowStartedAtMs ?: return null
        val windowMs = (nowMs - windowStart).coerceAtLeast(0L)
        return consumeDroppedFrames(windowMs)
    }

    private fun consumeDroppedFrames(windowMs: Long): DroppedFrameDiagnosticEvent {
        val event = DroppedFrameDiagnosticEvent(
            count = droppedFrameCount,
            elapsedMs = droppedFrameElapsedMs,
            windowMs = windowMs,
        )
        droppedFrameWindowStartedAtMs = null
        droppedFrameCount = 0
        droppedFrameElapsedMs = 0L
        return event
    }

    companion object {
        const val DROPPED_FRAME_LOG_WINDOW_MS = 5_000L
        const val DROPPED_FRAME_URGENT_COUNT = 24
    }
}
