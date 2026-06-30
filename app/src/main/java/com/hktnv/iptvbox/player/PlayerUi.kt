package com.hktnv.iptvbox.player

import android.content.Context
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.player.MediaPlayerFactory
import kotlinx.coroutines.delay

@Composable
internal fun PlayerScreen(
    item: CatalogItem,
    headers: Map<String, String>,
    playbackItems: List<CatalogItem>,
    onSelectItem: (CatalogItem) -> Unit,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val queue = remember(playbackItems, item.id) {
        buildPlayerPlaybackQueue(playbackItems, item)
    }
    val contentInfo = remember(item.id, queue.previous?.id, queue.next?.id) {
        item.toPlayerContentInfo(
            previousItem = queue.previous,
            nextItem = queue.next,
        )
    }
    val player = remember(item.id, headers) {
        MediaPlayerFactory.create(context, headers).apply {
            setMediaItem(MediaItem.fromUri(item.streamUrl))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var playbackSpeed by remember(player) { mutableStateOf(player.playbackParameters.speed) }
    var currentPositionMs by remember(player) { mutableStateOf(0L) }
    var durationMs by remember(player) { mutableStateOf(0L) }
    var canSeek by remember(player) { mutableStateOf(false) }
    fun updatePlaybackSnapshot() {
        isPlaying = player.isPlaying
        playbackSpeed = player.playbackParameters.speed
        currentPositionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        canSeek = player.isCurrentMediaItemSeekable && durationMs > 0L
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                updatePlaybackSnapshot()
            }
        }
        player.addListener(listener)
        updatePlaybackSnapshot()
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    var inputState by remember(item.id) { mutableStateOf(PlayerInputState.Watching) }
    var exitChoice by remember(item.id) { mutableStateOf(PlayerExitChoice.Exit) }
    val controlsVisible = inputState == PlayerInputState.ControlsVisible
    val contentListVisible = inputState == PlayerInputState.ContentListVisible
    val exitConfirmVisible = inputState == PlayerInputState.ExitConfirmVisible
    val playerFocusRequester = remember { FocusRequester() }
    LaunchedEffect(item.id, inputState) {
        if (inputState == PlayerInputState.Watching) {
            runCatching { playerFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(player, controlsVisible) {
        while (controlsVisible) {
            updatePlaybackSnapshot()
            delay(500L)
        }
    }

    fun switchTo(itemToPlay: CatalogItem) {
        inputState = PlayerInputState.ControlsVisible
        onSelectItem(itemToPlay)
    }

    fun applyInputResult(result: PlayerInputResult): Boolean {
        val previousState = inputState
        inputState = result.state
        if (result.state == PlayerInputState.ExitConfirmVisible &&
            previousState != PlayerInputState.ExitConfirmVisible
        ) {
            exitChoice = PlayerExitChoice.Exit
        }
        if (result.togglePlayback) {
            if (player.isPlaying) player.pause() else player.play()
            updatePlaybackSnapshot()
        }
        if (result.selectNextItem) queue.next?.let(::switchTo)
        if (result.selectPreviousItem) queue.previous?.let(::switchTo)
        if (result.exitRequested) onBack()
        return result.consumeInput
    }

    fun performExitChoice() {
        val action = when (exitChoice) {
            PlayerExitChoice.Exit -> PlayerInputAction.ExitSelected
            PlayerExitChoice.Continue -> PlayerInputAction.ContinueSelected
        }
        applyInputResult(reducePlayerInput(inputState, action))
    }

    fun seekBy(deltaMs: Long) {
        if (!canSeek) return
        val target = (player.currentPosition + deltaMs).coerceIn(0L, durationMs)
        player.seekTo(target)
        updatePlaybackSnapshot()
    }

    fun cycleSpeed() {
        val speeds = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)
        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - playbackSpeed) < 0.01f }
        val nextIndex = ((currentIndex.takeIf { it >= 0 } ?: 0) + 1) % speeds.size
        val nextSpeed = speeds[nextIndex]
        player.setPlaybackSpeed(nextSpeed)
        updatePlaybackSnapshot()
    }

    fun handleRemoteCommand(command: PlayerRemoteCommand): Boolean {
        val action = command.toInputAction() ?: return false
        return applyInputResult(reducePlayerInput(inputState, action))
    }

    fun shouldHandleRemoteKey(keyCode: Int): Boolean {
        val action = playerRemoteCommandForKeyCode(keyCode).toInputAction() ?: return false
        return reducePlayerInput(inputState, action).consumeInput
    }

    fun handleExitDialogKeyEvent(event: KeyEvent): Boolean {
        if (!exitConfirmVisible || !event.keyCode.isPlayerExitDialogKey()) return false
        if (event.action == KeyEvent.ACTION_DOWN) return true
        if (event.action != KeyEvent.ACTION_UP) return true
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> exitChoice = PlayerExitChoice.Exit
            KeyEvent.KEYCODE_DPAD_RIGHT -> exitChoice = PlayerExitChoice.Continue
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> performExitChoice()
            KeyEvent.KEYCODE_BACK -> applyInputResult(
                reducePlayerInput(inputState, PlayerInputAction.ExitSelected),
            )
        }
        return true
    }

    BackHandler(enabled = contentListVisible) {
        applyInputResult(reducePlayerInput(inputState, PlayerInputAction.BackPressed))
    }
    BackHandler(enabled = !contentListVisible && !exitConfirmVisible) {
        applyInputResult(reducePlayerInput(inputState, PlayerInputAction.BackPressed))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(playerFocusRequester)
            .focusable(enabled = !controlsVisible && !contentListVisible && !exitConfirmVisible)
            .onPreviewKeyEvent { event ->
                if (inputState != PlayerInputState.Watching) {
                    return@onPreviewKeyEvent false
                }
                val command = playerRemoteCommandForComposeKey(event.key)
                if (command == PlayerRemoteCommand.None) {
                    return@onPreviewKeyEvent false
                }
                val action = command.toInputAction() ?: return@onPreviewKeyEvent false
                if (!reducePlayerInput(inputState, action).consumeInput) {
                    return@onPreviewKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown) {
                    true
                } else {
                    handleRemoteCommand(command)
                }
            },
    ) {
        AndroidView(
            factory = { ctx ->
                TvRemotePlayerView(ctx).apply {
                    this.player = player
                    onOverlayKeyEvent = ::handleExitDialogKeyEvent
                    shouldHandleKeyCode = ::shouldHandleRemoteKey
                    onRemoteCommand = ::handleRemoteCommand
                    useController = false
                    keepScreenOn = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                }
            },
            update = { view ->
                view.player = player
                view.onOverlayKeyEvent = ::handleExitDialogKeyEvent
                view.shouldHandleKeyCode = ::shouldHandleRemoteKey
                view.onRemoteCommand = ::handleRemoteCommand
                if (!contentListVisible && !exitConfirmVisible && !controlsVisible) {
                    runCatching { playerFocusRequester.requestFocus() }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (shouldShowPlayerContentInfo(controlsVisible, PlayerExitDialogState.Hidden) && !contentListVisible) {
            PlayerInfoOverlay(
                info = contentInfo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 28.dp, top = 28.dp),
            )
        }
        if (controlsVisible && !contentListVisible && !exitConfirmVisible) {
            PlayerControlsOverlay(
                isPlaying = isPlaying,
                positionMs = currentPositionMs,
                durationMs = durationMs,
                speed = playbackSpeed,
                canSeek = canSeek,
                onSeekBack = { seekBy(-10_000L) },
                onTogglePlayback = {
                    if (player.isPlaying) player.pause() else player.play()
                    updatePlaybackSnapshot()
                },
                onSeekForward = { seekBy(10_000L) },
                onCycleSpeed = ::cycleSpeed,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 42.dp),
            )
        }
        if (contentListVisible) {
            PlayerContentListOverlay(
                queue = queue,
                onSelectItem = ::switchTo,
                onDismiss = {
                    applyInputResult(reducePlayerInput(inputState, PlayerInputAction.BackPressed))
                },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        if (exitConfirmVisible) {
            PlayerExitConfirmationDialog(
                selectedChoice = exitChoice,
                onChoiceChange = { exitChoice = it },
                onExit = {
                    applyInputResult(reducePlayerInput(inputState, PlayerInputAction.ExitSelected))
                },
                onContinue = {
                    applyInputResult(reducePlayerInput(inputState, PlayerInputAction.ContinueSelected))
                },
            )
        }
    }
}

private class TvRemotePlayerView(context: Context) : PlayerView(context) {
    var onOverlayKeyEvent: (KeyEvent) -> Boolean = { false }
    var shouldHandleKeyCode: (Int) -> Boolean = { false }
    var onRemoteCommand: (PlayerRemoteCommand) -> Boolean = { false }

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

private fun Int.isPlayerExitDialogKey(): Boolean {
    return this == KeyEvent.KEYCODE_DPAD_LEFT ||
        this == KeyEvent.KEYCODE_DPAD_RIGHT ||
        this == KeyEvent.KEYCODE_DPAD_CENTER ||
        this == KeyEvent.KEYCODE_ENTER ||
        this == KeyEvent.KEYCODE_NUMPAD_ENTER ||
        this == KeyEvent.KEYCODE_BACK
}

private fun playerRemoteCommandForComposeKey(key: Key): PlayerRemoteCommand {
    return when (key) {
        Key.DirectionCenter,
        Key.Enter,
        Key.NumPadEnter -> PlayerRemoteCommand.TogglePlayPause
        Key.DirectionUp -> PlayerRemoteCommand.NextItem
        Key.DirectionDown -> PlayerRemoteCommand.PreviousItem
        Key.DirectionLeft -> PlayerRemoteCommand.OpenContentList
        else -> PlayerRemoteCommand.None
    }
}
