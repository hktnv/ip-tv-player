package com.hktnv.iptvbox.player

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hktnv.iptvbox.core.model.CatalogItem

@Composable
internal fun PlayerMediaSwitchLifecycle(
    player: ExoPlayer,
    item: CatalogItem,
    diagnostics: PlayerDiagnosticLogger?,
    onBeforeSwitch: () -> Unit,
) {
    var pendingSwitchStartedAtMs by remember(player) { mutableLongStateOf(0L) }
    val latestDiagnostics by rememberUpdatedState(diagnostics)

    LaunchedEffect(player, item.id, item.streamUrl) {
        onBeforeSwitch()
        pendingSwitchStartedAtMs = SystemClock.elapsedRealtime()
        latestDiagnostics?.logChannelSwitchStart(item.id)
        player.setMediaItem(item.toPlayerMediaItem())
        player.playWhenReady = true
        player.prepare()
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
