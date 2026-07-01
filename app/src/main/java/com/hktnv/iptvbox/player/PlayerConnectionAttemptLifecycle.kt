package com.hktnv.iptvbox.player

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player

@Composable
internal fun rememberPlayerConnectionAttemptSnapshot(
    player: Player,
    itemId: String,
    retryRevision: Int,
    manuallyPaused: Boolean,
): PlayerConnectionAttemptSnapshot {
    val attemptStartedAtMs = remember(player, itemId, retryRevision) {
        SystemClock.elapsedRealtime()
    }
    var elapsedMs by remember(player, itemId, retryRevision) { mutableLongStateOf(0L) }
    var connectionEstablished by remember(player, itemId, retryRevision) { mutableStateOf(false) }

    DisposableEffect(player, itemId, retryRevision) {
        fun markReadyIfCurrentItemIsReady() {
            if (
                player.playbackState == Player.STATE_READY &&
                player.currentMediaItem?.mediaId == itemId
            ) {
                connectionEstablished = true
                elapsedMs = 0L
            }
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                markReadyIfCurrentItemIsReady()
            }
        }
        player.addListener(listener)
        markReadyIfCurrentItemIsReady()
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, itemId, retryRevision, connectionEstablished, manuallyPaused) {
        if (connectionEstablished || manuallyPaused) {
            elapsedMs = 0L
            return@LaunchedEffect
        }
        while (!connectionEstablished && !manuallyPaused) {
            elapsedMs = SystemClock.elapsedRealtime() - attemptStartedAtMs
            kotlinx.coroutines.delay(ConnectionAttemptTickMs)
        }
    }

    return PlayerConnectionAttemptSnapshot(
        awaitingConnection = !connectionEstablished && !manuallyPaused,
        elapsedMs = elapsedMs,
    )
}

private const val ConnectionAttemptTickMs = 250L
