package com.hktnv.iptvbox.player

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player

@Composable
internal fun PlayerSurfaceView(
    player: Player,
    surfaceKey: String,
    controlsVisible: Boolean,
    contentListVisible: Boolean,
    exitConfirmVisible: Boolean,
    playerFocusRequester: FocusRequester,
    onOverlayKeyEvent: (KeyEvent) -> Boolean,
    shouldHandleKeyCode: (Int) -> Boolean,
    onRemoteCommand: (PlayerRemoteCommand) -> Boolean,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            TvRemotePlayerView(context).apply {
                bindPlayer(player, surfaceKey)
                this.onOverlayKeyEvent = onOverlayKeyEvent
                this.shouldHandleKeyCode = shouldHandleKeyCode
                this.onRemoteCommand = onRemoteCommand
                useController = false
                keepScreenOn = true
                isFocusable = true
                isFocusableInTouchMode = true
            }
        },
        update = { view ->
            view.bindPlayer(player, surfaceKey)
            view.onOverlayKeyEvent = onOverlayKeyEvent
            view.shouldHandleKeyCode = shouldHandleKeyCode
            view.onRemoteCommand = onRemoteCommand
            if (!contentListVisible && !exitConfirmVisible && !controlsVisible) {
                runCatching { playerFocusRequester.requestFocus() }
            }
        },
        modifier = modifier,
    )
}
