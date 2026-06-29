package com.hktnv.iptvbox.player

internal enum class PlayerExitDialogState {
    Hidden,
    Visible,
}

internal enum class PlayerExitAction {
    BackPressed,
    ContinueSelected,
    ExitSelected,
}

internal data class PlayerExitResult(
    val state: PlayerExitDialogState,
    val exitRequested: Boolean,
)

internal fun reducePlayerExitDialog(
    state: PlayerExitDialogState,
    action: PlayerExitAction,
): PlayerExitResult {
    return when (action) {
        PlayerExitAction.BackPressed -> when (state) {
            PlayerExitDialogState.Hidden -> PlayerExitResult(PlayerExitDialogState.Visible, exitRequested = false)
            PlayerExitDialogState.Visible -> PlayerExitResult(PlayerExitDialogState.Hidden, exitRequested = true)
        }
        PlayerExitAction.ContinueSelected -> PlayerExitResult(PlayerExitDialogState.Hidden, exitRequested = false)
        PlayerExitAction.ExitSelected -> PlayerExitResult(PlayerExitDialogState.Hidden, exitRequested = true)
    }
}
