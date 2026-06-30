package com.hktnv.iptvbox.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
internal fun StandardMedia3PlayerScreen(
    player: ExoPlayer,
    diagnosticsContext: PlayerDiagnosticContext?,
) {
    val diagnostics = remember(player, diagnosticsContext) {
        diagnosticsContext?.let { context ->
            PlayerDiagnosticLogger(
                context = context,
                manualPauseProvider = { false },
                bufferSnapshotProvider = { player.toBufferDiagnosticSnapshot() },
            )
        }
    }
    DisposableEffect(player, diagnostics) {
        if (diagnostics != null) {
            player.addAnalyticsListener(diagnostics)
            diagnostics.syncPlaybackState(player.playWhenReady)
            diagnostics.logAttached()
        }
        onDispose {
            if (diagnostics != null) {
                diagnostics.logDetached()
                player.removeAnalyticsListener(diagnostics)
            }
            player.release()
        }
    }
    StandardMedia3PlayerSurface(player = player)
}

@Composable
private fun StandardMedia3PlayerSurface(
    player: Player,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = true
                    controllerShowTimeoutMs = CONTROLLER_TIMEOUT_MS
                    keepScreenOn = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setEnableComposeSurfaceSyncWorkaround(true)
                    showController()
                    requestFocus()
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            },
        )
    }
}

private const val CONTROLLER_TIMEOUT_MS = 3_500
