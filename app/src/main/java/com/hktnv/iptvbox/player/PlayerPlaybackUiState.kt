package com.hktnv.iptvbox.player

import androidx.media3.common.Player

internal fun calculateSeekTarget(
    positionMs: Long,
    durationMs: Long,
    deltaMs: Long,
): Long {
    if (durationMs <= 0L) return 0L
    return (positionMs + deltaMs).coerceIn(0L, durationMs)
}

internal fun shouldShowBufferingIndicator(
    playbackState: Int,
    manuallyPaused: Boolean,
): Boolean {
    return playbackState == Player.STATE_BUFFERING && !manuallyPaused
}

internal fun shouldPresentAsPlaying(
    playWhenReady: Boolean,
    manuallyPaused: Boolean,
): Boolean {
    return playWhenReady && !manuallyPaused
}

internal fun formatPlayerTime(valueMs: Long): String {
    if (valueMs <= 0L) return "00:00"
    val totalSeconds = valueMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
