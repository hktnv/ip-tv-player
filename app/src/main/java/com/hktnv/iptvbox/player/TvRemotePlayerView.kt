package com.hktnv.iptvbox.player

import android.content.Context
import android.view.KeyEvent
import androidx.media3.ui.PlayerView

@Suppress("UnstableApiUsage")
internal class TvRemotePlayerView(context: Context) : PlayerView(context) {
    var onOverlayKeyEvent: (KeyEvent) -> Boolean = { false }
    var shouldHandleKeyCode: (Int) -> Boolean = { false }
    var onRemoteCommand: (PlayerRemoteCommand) -> Boolean = { false }

    init {
        useController = false
        keepScreenOn = true
        setEnableComposeSurfaceSyncWorkaround(true)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (onOverlayKeyEvent(event)) {
            return true
        }
        val command = playerRemoteCommandForKeyCode(event.keyCode)
        if (command != PlayerRemoteCommand.None && shouldHandleKeyCode(event.keyCode)) {
            return when (event.action) {
                KeyEvent.ACTION_DOWN -> true
                KeyEvent.ACTION_UP -> onRemoteCommand(command)
                else -> true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
