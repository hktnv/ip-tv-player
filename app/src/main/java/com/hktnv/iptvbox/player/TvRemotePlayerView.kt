package com.hktnv.iptvbox.player

import android.content.Context
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
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
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        setEnableComposeSurfaceSyncWorkaround(true)
    }

    fun bindPlayer(nextPlayer: Player, surfaceKey: String) {
        if (player === nextPlayer && boundSurfaceKey == surfaceKey) return
        setPlayer(null)
        setPlayer(nextPlayer)
        boundSurfaceKey = surfaceKey
        requestVideoLayout()
    }

    private fun requestVideoLayout() {
        requestLayout()
        invalidate()
        post {
            requestLayout()
            invalidate()
        }
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
