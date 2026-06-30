package com.hktnv.iptvbox.player

import android.view.KeyEvent

internal fun handlePlayerExitDialogKeyEvent(
    event: KeyEvent,
    visible: Boolean,
    onChoiceChange: (PlayerExitChoice) -> Unit,
    onConfirmChoice: () -> Unit,
    onBackExit: () -> Unit,
): Boolean {
    if (!visible || !event.keyCode.isPlayerExitDialogKey()) return false
    if (event.action == KeyEvent.ACTION_DOWN) return true
    if (event.action != KeyEvent.ACTION_UP) return true
    when (event.keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> onChoiceChange(PlayerExitChoice.Exit)
        KeyEvent.KEYCODE_DPAD_RIGHT -> onChoiceChange(PlayerExitChoice.Continue)
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> onConfirmChoice()
        KeyEvent.KEYCODE_BACK -> onBackExit()
    }
    return true
}
