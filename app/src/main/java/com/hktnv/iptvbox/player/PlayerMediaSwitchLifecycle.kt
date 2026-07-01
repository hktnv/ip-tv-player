package com.hktnv.iptvbox.player

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@Composable
internal fun PlayerMediaSwitchLifecycle(
    player: ExoPlayer,
    queue: PlayerMediaQueue,
    targetItemId: String,
    diagnostics: PlayerDiagnosticLogger?,
    onBeforeSwitch: () -> Unit,
) {
    var pendingSwitchStartedAtMs by remember(player) { mutableLongStateOf(0L) }
    var activeQueueSignature by remember(player) { mutableStateOf<String?>(null) }
    val latestDiagnostics by rememberUpdatedState(diagnostics)

    LaunchedEffect(player, queue.signature, queue.currentIndex, targetItemId) {
        onBeforeSwitch()
        pendingSwitchStartedAtMs = SystemClock.elapsedRealtime()
        latestDiagnostics?.logChannelSwitchStart(targetItemId)
        val switchPlan = choosePlayerMediaSwitchPlan(
            activeQueueSignature = activeQueueSignature,
            playerMediaItemCount = player.mediaItemCount,
            currentMediaItemIndex = player.currentMediaItemIndex,
            currentMediaId = player.currentMediaItem?.mediaId,
            queue = queue,
        )
        val shouldPrepare = when (switchPlan) {
            PlayerMediaSwitchPlan.SetQueue,
            PlayerMediaSwitchPlan.ResetQueueAtTarget -> {
                latestDiagnostics?.logMediaSwitchPlan(
                    plan = switchPlan.name.lowercase(),
                    index = queue.currentIndex,
                    queueSize = queue.mediaItems.size,
                )
                player.setMediaItems(queue.mediaItems, queue.currentIndex, C.TIME_UNSET)
                activeQueueSignature = queue.signature
                true
            }
            PlayerMediaSwitchPlan.ReuseCurrent -> {
                latestDiagnostics?.logMediaSwitchPlan(
                    plan = "reuse_current_item",
                    index = queue.currentIndex,
                    queueSize = queue.mediaItems.size,
                )
                player.playbackState == Player.STATE_IDLE
            }
        }
        player.playWhenReady = true
        if (shouldPrepare) {
            player.prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (player.playbackState == Player.STATE_READY && pendingSwitchStartedAtMs > 0L) {
                    latestDiagnostics?.logChannelSwitchReady(
                        durationMs = SystemClock.elapsedRealtime() - pendingSwitchStartedAtMs,
                    )
                    pendingSwitchStartedAtMs = 0L
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
}
