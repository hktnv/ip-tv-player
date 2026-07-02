package com.hktnv.iptvbox.player

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key

internal fun Int.isPlayerExitDialogKey(): Boolean {
    return this == KeyEvent.KEYCODE_DPAD_LEFT ||
        this == KeyEvent.KEYCODE_DPAD_RIGHT ||
        this == KeyEvent.KEYCODE_DPAD_CENTER ||
        this == KeyEvent.KEYCODE_ENTER ||
        this == KeyEvent.KEYCODE_NUMPAD_ENTER ||
        this == KeyEvent.KEYCODE_BACK
}

internal fun playerRemoteCommandForComposeKey(key: Key): PlayerRemoteCommand {
    return when (key) {
        Key.DirectionCenter,
        Key.Enter,
        Key.NumPadEnter -> PlayerRemoteCommand.TogglePlayPause
        Key.DirectionUp -> PlayerRemoteCommand.NextItem
        Key.DirectionDown -> PlayerRemoteCommand.PreviousItem
        Key.DirectionLeft -> PlayerRemoteCommand.Left
        Key.DirectionRight -> PlayerRemoteCommand.Right
        Key.Back -> PlayerRemoteCommand.Back
        else -> PlayerRemoteCommand.None
    }
}
