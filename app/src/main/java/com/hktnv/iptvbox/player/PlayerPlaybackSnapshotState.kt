package com.hktnv.iptvbox.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player

internal class PlayerPlaybackSnapshotState(player: Player) {
    var playbackSpeed by mutableStateOf(player.playbackParameters.speed)
        private set

    var currentPositionMs by mutableStateOf(0L)
        private set

    var durationMs by mutableStateOf(0L)
        private set

    var canSeek by mutableStateOf(false)
        private set

    var playbackState by mutableStateOf(player.playbackState)
        private set

    fun reset(player: Player) {
        currentPositionMs = 0L
        durationMs = 0L
        canSeek = false
        playbackState = player.playbackState
        update(player)
    }

    fun update(player: Player) {
        playbackSpeed = player.playbackParameters.speed
        currentPositionMs = player.currentPosition.coerceAtLeast(0L)
        val resolvedDuration = player.duration
            .takeIf { it != C.TIME_UNSET }
            ?.coerceAtLeast(0L)
            ?: 0L
        durationMs = resolvedDuration
        canSeek = player.isCurrentMediaItemSeekable && resolvedDuration > 0L
        playbackState = player.playbackState
    }
}

@Composable
internal fun rememberPlayerPlaybackSnapshot(player: Player): PlayerPlaybackSnapshotState =
    remember(player) { PlayerPlaybackSnapshotState(player) }
