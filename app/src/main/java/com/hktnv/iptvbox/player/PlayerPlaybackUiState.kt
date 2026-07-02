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

internal fun shouldShowPlayerLoadingIndicator(
    playbackState: Int,
    manuallyPaused: Boolean,
    connectionLoading: Boolean,
    seekLoading: Boolean,
): Boolean {
    return seekLoading || connectionLoading || shouldShowBufferingIndicator(playbackState, manuallyPaused)
}

internal fun shouldPresentAsPlaying(
    playWhenReady: Boolean,
    manuallyPaused: Boolean,
): Boolean {
    return playWhenReady && !manuallyPaused
}

internal fun shouldAutoHidePlayerOsd(
    controlsVisible: Boolean,
    playWhenReady: Boolean,
    manuallyPaused: Boolean,
): Boolean {
    return controlsVisible && shouldPresentAsPlaying(playWhenReady, manuallyPaused)
}

internal enum class PlayerTimelineRemoteKey {
    Left,
    Right,
    Up,
    Down,
    Other,
}

internal enum class PlayerTimelineKeyAction {
    None,
    PreviewBack,
    PreviewForward,
    ExitTimeline,
}

internal sealed interface PlayerTimelineLabel {
    data object Live : PlayerTimelineLabel
    data object Stream : PlayerTimelineLabel
    data class Text(val value: String) : PlayerTimelineLabel
}

internal data class PlayerTimelinePresentation(
    val progress: Float,
    val startLabel: PlayerTimelineLabel,
    val endLabel: PlayerTimelineLabel,
)

internal fun resolvePlayerTimelinePresentation(
    positionMs: Long,
    durationMs: Long,
    canSeek: Boolean,
    liveContent: Boolean,
): PlayerTimelinePresentation {
    if (liveContent) {
        return PlayerTimelinePresentation(
            progress = 1f,
            startLabel = PlayerTimelineLabel.Live,
            endLabel = PlayerTimelineLabel.Stream,
        )
    }
    if (canSeek && durationMs > 0L) {
        return PlayerTimelinePresentation(
            progress = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
            startLabel = PlayerTimelineLabel.Text(formatPlayerTime(positionMs)),
            endLabel = PlayerTimelineLabel.Text(formatPlayerTime(durationMs)),
        )
    }
    return PlayerTimelinePresentation(
        progress = 0f,
        startLabel = PlayerTimelineLabel.Text(formatPlayerTime(positionMs)),
        endLabel = PlayerTimelineLabel.Text("--:--"),
    )
}

internal fun resolvePlayerTimelineKeyAction(
    key: PlayerTimelineRemoteKey,
    canSeek: Boolean,
): PlayerTimelineKeyAction {
    if (!canSeek) return PlayerTimelineKeyAction.None
    return when (key) {
        PlayerTimelineRemoteKey.Left -> PlayerTimelineKeyAction.PreviewBack
        PlayerTimelineRemoteKey.Right -> PlayerTimelineKeyAction.PreviewForward
        PlayerTimelineRemoteKey.Up,
        PlayerTimelineRemoteKey.Down -> PlayerTimelineKeyAction.ExitTimeline
        PlayerTimelineRemoteKey.Other -> PlayerTimelineKeyAction.None
    }
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
