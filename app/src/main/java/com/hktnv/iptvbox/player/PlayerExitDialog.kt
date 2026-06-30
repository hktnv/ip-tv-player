package com.hktnv.iptvbox.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift

internal enum class PlayerExitChoice {
    Exit,
    Continue,
}

@Composable
internal fun PlayerExitConfirmationDialog(
    selectedChoice: PlayerExitChoice,
    onChoiceChange: (PlayerExitChoice) -> Unit,
    onExit: () -> Unit,
    onContinue: () -> Unit,
) {
    val exitFocusRequester = remember { FocusRequester() }
    val continueFocusRequester = remember { FocusRequester() }

    BackHandler { onExit() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { exitFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onExit) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
            tonalElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "İçerikten çıkmak istiyor musunuz?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PlayerExitButton(
                        text = "Çık",
                        onClick = onExit,
                        modifier = Modifier.weight(1f),
                        focusRequester = exitFocusRequester,
                        selected = selectedChoice == PlayerExitChoice.Exit,
                        onFocused = { onChoiceChange(PlayerExitChoice.Exit) },
                        onMoveLeft = { },
                        onMoveRight = {
                            onChoiceChange(PlayerExitChoice.Continue)
                            runCatching { continueFocusRequester.requestFocus() }
                        },
                        primary = false,
                    )
                    PlayerExitButton(
                        text = "Devam Et",
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        focusRequester = continueFocusRequester,
                        selected = selectedChoice == PlayerExitChoice.Continue,
                        onFocused = { onChoiceChange(PlayerExitChoice.Continue) },
                        onMoveLeft = {
                            onChoiceChange(PlayerExitChoice.Exit)
                            runCatching { exitFocusRequester.requestFocus() }
                        },
                        onMoveRight = { },
                        primary = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerExitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    selected: Boolean,
    onFocused: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    primary: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        focused -> if (primary) MaterialTheme.colorScheme.primaryContainer else TvFocusPanel
        selected -> if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        primary -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onPreviewKeyEvent { event ->
                handlePlayerExitKey(event, onClick, onMoveLeft, onMoveRight)
            }
            .onKeyEvent { event ->
                handlePlayerExitKey(event, onClick, onMoveLeft, onMoveRight)
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .focusable()
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .tvClickable(onClick = onClick),
        color = background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) TvFocusBorder else MaterialTheme.colorScheme.surfaceBorder,
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Text(
            text = text,
            color = if (primary) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private fun handlePlayerExitKey(
    event: androidx.compose.ui.input.key.KeyEvent,
    onClick: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
): Boolean {
    return when {
        event.key.isPlayerExitSelectKey() -> {
            if (event.type == KeyEventType.KeyUp) onClick()
            true
        }
        event.key == Key.DirectionLeft -> {
            if (event.type == KeyEventType.KeyUp) onMoveLeft()
            true
        }
        event.key == Key.DirectionRight -> {
            if (event.type == KeyEventType.KeyUp) onMoveRight()
            true
        }
        else -> false
    }
}

private fun Key.isPlayerExitSelectKey(): Boolean {
    return this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter
}
