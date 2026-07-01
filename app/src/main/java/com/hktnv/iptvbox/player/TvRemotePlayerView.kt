package com.hktnv.iptvbox.player

import android.content.Context
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

@Suppress("UnstableApiUsage")
internal class TvRemotePlayerView(context: Context) : PlayerView(context) {
    var onOverlayKeyEvent: (KeyEvent) -> Boolean = { false }
    var shouldHandleKeyCode: (Int) -> Boolean = { false }
    var onRemoteCommand: (PlayerRemoteCommand) -> Boolean = { false }
    private var boundSurfaceKey: String? = null

    init {
        useController = false
        keepScreenOn = true
        setEnableComposeSurfaceSyncWorkaround(true)
    }

    fun bindPlayer(nextPlayer: Player, surfaceKey: String) {
        if (player === nextPlayer && boundSurfaceKey == surfaceKey) return
        setPlayer(null)
        setPlayer(nextPlayer)
        boundSurfaceKey = surfaceKey
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
