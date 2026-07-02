package com.hktnv.iptvbox.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
internal fun PlayerVideoTouchLayer(
    inputState: PlayerInputState,
    onInputStateChange: (PlayerInputState) -> Unit,
    onControlsShown: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(inputState) {
                detectTapGestures {
                    val nextState = playerInputStateAfterVideoTap(inputState)
                    if (nextState != inputState) {
                        onInputStateChange(nextState)
                        if (nextState == PlayerInputState.ControlsVisible) onControlsShown()
                    }
                }
            },
    )
}

internal fun playerInputStateAfterVideoTap(inputState: PlayerInputState): PlayerInputState {
    return when (inputState) {
        PlayerInputState.Watching -> PlayerInputState.ControlsVisible
        PlayerInputState.ControlsVisible -> PlayerInputState.Watching
        PlayerInputState.ExitConfirmVisible -> inputState
    }
}
