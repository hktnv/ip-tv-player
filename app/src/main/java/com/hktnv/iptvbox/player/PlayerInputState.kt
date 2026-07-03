package com.hktnv.iptvbox.player

internal enum class PlayerInputState {
    Watching,
    ControlsVisible,
    ExitConfirmVisible,
}

internal enum class PlayerInputAction {
    ControllerShown,
    ControllerHidden,
    OkPressed,
    UpPressed,
    DownPressed,
    LeftPressed,
    RightPressed,
    BackPressed,
    ContinueSelected,
    ExitSelected,
}

internal data class PlayerInputResult(
    val state: PlayerInputState,
    val consumeInput: Boolean = true,
    val showControls: Boolean = false,
    val showZappingInfo: Boolean = false,
    val togglePlayback: Boolean = false,
    val selectNextItem: Boolean = false,
    val selectPreviousItem: Boolean = false,
    val exitRequested: Boolean = false,
)

internal fun reducePlayerInput(
    state: PlayerInputState,
    action: PlayerInputAction,
): PlayerInputResult {
    return when (action) {
        PlayerInputAction.ControllerShown -> when (state) {
            PlayerInputState.Watching,
            PlayerInputState.ControlsVisible -> PlayerInputResult(PlayerInputState.ControlsVisible, consumeInput = false)
            PlayerInputState.ExitConfirmVisible -> PlayerInputResult(state, consumeInput = false)
        }
        PlayerInputAction.ControllerHidden -> when (state) {
            PlayerInputState.ControlsVisible -> PlayerInputResult(PlayerInputState.Watching, consumeInput = false)
            else -> PlayerInputResult(state, consumeInput = false)
        }
        PlayerInputAction.OkPressed -> when (state) {
            PlayerInputState.Watching -> PlayerInputResult(
                state = PlayerInputState.ControlsVisible,
                showControls = true,
                togglePlayback = true,
            )
            PlayerInputState.ControlsVisible -> PlayerInputResult(
                state = PlayerInputState.ControlsVisible,
                consumeInput = false,
            )
            else -> PlayerInputResult(state, consumeInput = false)
        }
        PlayerInputAction.UpPressed -> when (state) {
            PlayerInputState.Watching -> PlayerInputResult(
                state = PlayerInputState.Watching,
                showZappingInfo = true,
                selectNextItem = true,
            )
            PlayerInputState.ControlsVisible -> PlayerInputResult(
                state = PlayerInputState.ControlsVisible,
                consumeInput = false,
            )
            else -> PlayerInputResult(state, consumeInput = false)
        }
        PlayerInputAction.DownPressed -> when (state) {
            PlayerInputState.Watching -> PlayerInputResult(
                state = PlayerInputState.Watching,
                showZappingInfo = true,
                selectPreviousItem = true,
            )
            PlayerInputState.ControlsVisible -> PlayerInputResult(
                state = PlayerInputState.ControlsVisible,
                consumeInput = false,
            )
            else -> PlayerInputResult(state, consumeInput = false)
        }
        PlayerInputAction.LeftPressed -> when (state) {
            PlayerInputState.Watching,
            PlayerInputState.ControlsVisible -> PlayerInputResult(state, consumeInput = false)
            else -> PlayerInputResult(state, consumeInput = false)
        }
        PlayerInputAction.RightPressed -> when (state) {
            PlayerInputState.Watching -> PlayerInputResult(
                state = PlayerInputState.ControlsVisible,
                showControls = true,
            )
            PlayerInputState.ControlsVisible -> PlayerInputResult(
                state = PlayerInputState.ControlsVisible,
                consumeInput = false,
            )
            else -> PlayerInputResult(state, consumeInput = false)
        }
        PlayerInputAction.BackPressed -> when (state) {
            PlayerInputState.ControlsVisible -> PlayerInputResult(PlayerInputState.Watching)
            PlayerInputState.Watching -> PlayerInputResult(PlayerInputState.ExitConfirmVisible)
            PlayerInputState.ExitConfirmVisible -> PlayerInputResult(
                state = PlayerInputState.Watching,
                exitRequested = true,
            )
        }
        PlayerInputAction.ContinueSelected -> PlayerInputResult(PlayerInputState.Watching)
        PlayerInputAction.ExitSelected -> PlayerInputResult(
            state = PlayerInputState.Watching,
            exitRequested = true,
        )
    }
}

internal fun PlayerRemoteCommand.toInputAction(): PlayerInputAction? {
    return when (this) {
        PlayerRemoteCommand.TogglePlayPause -> PlayerInputAction.OkPressed
        PlayerRemoteCommand.NextItem -> PlayerInputAction.UpPressed
        PlayerRemoteCommand.PreviousItem -> PlayerInputAction.DownPressed
        PlayerRemoteCommand.Left -> PlayerInputAction.LeftPressed
        PlayerRemoteCommand.Right -> PlayerInputAction.RightPressed
        PlayerRemoteCommand.None -> null
    }
}
