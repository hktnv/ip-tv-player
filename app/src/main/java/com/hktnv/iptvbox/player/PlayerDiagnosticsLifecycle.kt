package com.hktnv.iptvbox.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.media3.exoplayer.ExoPlayer

@Composable
internal fun PlayerDiagnosticsLifecycle(
    player: ExoPlayer,
    diagnostics: PlayerDiagnosticLogger?,
) {
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
        }
    }
}
