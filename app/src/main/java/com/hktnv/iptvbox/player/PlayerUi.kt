package com.hktnv.iptvbox.player

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.media3.common.Player
import com.hktnv.iptvbox.core.model.CatalogItem
import kotlinx.coroutines.delay

@Composable
internal fun PlayerScreen(
    item: CatalogItem,
    headers: Map<String, String>,
    playbackItems: List<CatalogItem>,
    playerUiMode: PlayerUiMode,
    onSelectItem: (CatalogItem) -> Unit,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    PlayerOrientationLock()
    val player = rememberIptvPlayerSession(context, headers, item)
    val diagnosticsContext = remember(item.id, item.streamUrl, playerUiMode) {
        if (isPlayerDiagnosticEnabled) {
            item.toPlayerDiagnosticContext(playerUiMode)
        } else {
            null
        }
    }
    val queue = remember(playbackItems, item.id) {
        buildPlayerPlaybackQueue(playbackItems, item)
    }
    val playerMediaItems = remember(queue.items) {
        queue.items.map { it.toPlayerMediaItem() }
    }
    val mediaQueue = remember(queue.currentIndex, queue.items, playerMediaItems) {
        queue.toPlayerMediaQueue(playerMediaItems)
    }
    var manuallyPaused by remember(player) { mutableStateOf(false) }
    var connectionRetryRevision by remember(item.id) { mutableIntStateOf(0) }
    val diagnostics = remember(player, diagnosticsContext) {
        diagnosticsContext?.let { context ->
            PlayerDiagnosticLogger(
                context = context,
                manualPauseProvider = { manuallyPaused },
                bufferSnapshotProvider = { player.toBufferDiagnosticSnapshot() },
            )
        }
    }
    PlayerMediaSwitchLifecycle(
        player = player,
        queue = mediaQueue,
        targetItemId = item.id,
        diagnostics = diagnostics,
        onBeforeSwitch = { manuallyPaused = false },
    )

    val contentInfo = remember(item.id, queue.previous?.id, queue.next?.id) {
        item.toPlayerContentInfo(
            previousItem = queue.previous,
            nextItem = queue.next,
        )
    }
    val playbackSnapshot = rememberPlayerPlaybackSnapshot(player)
    LaunchedEffect(player, item.id) {
        playbackSnapshot.reset(player)
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                playbackSnapshot.update(player)
            }
        }
        player.addListener(listener)
        playbackSnapshot.update(player)
        onDispose {
            player.removeListener(listener)
        }
    }
    PlayerDiagnosticsLifecycle(player = player, diagnostics = diagnostics)

    var connectionTimeoutDismissed by remember(player, item.id, connectionRetryRevision) {
        mutableStateOf(false)
    }
    val connectionAttempt = rememberPlayerConnectionAttemptSnapshot(
        player = player,
        itemId = item.id,
        retryRevision = connectionRetryRevision,
        manuallyPaused = manuallyPaused,
    )
    val connectionTimeoutUi = playerConnectionTimeoutUiState(
        awaitingConnection = connectionAttempt.awaitingConnection,
        elapsedMs = connectionAttempt.elapsedMs,
        timeoutDismissed = connectionTimeoutDismissed,
    )

    var inputState by remember { mutableStateOf(PlayerInputState.Watching) }
    var exitChoice by remember { mutableStateOf(PlayerExitChoice.Exit) }
    var controlsRevision by remember { mutableIntStateOf(0) }
    var zappingInfoVisible by remember { mutableStateOf(false) }
    val backPressGuard = remember { PlayerBackPressGuard() }
    val controlsVisible = inputState == PlayerInputState.ControlsVisible
    val contentListVisible = inputState == PlayerInputState.ContentListVisible
    val exitConfirmVisible = inputState == PlayerInputState.ExitConfirmVisible
    val connectionTimeoutVisible = connectionTimeoutUi.showTimeoutDialog
    val osdVisible = controlsVisible && !contentListVisible && !exitConfirmVisible && !connectionTimeoutVisible
    val zappingInfoActive = zappingInfoVisible && inputState == PlayerInputState.Watching && !connectionTimeoutVisible
    val playerFocusRequester = remember { FocusRequester() }
    LaunchedEffect(item.id, inputState) {
        if (inputState == PlayerInputState.Watching) runCatching { playerFocusRequester.requestFocus() }
    }
    LaunchedEffect(player, controlsVisible) {
        while (controlsVisible) {
            playbackSnapshot.update(player)
            delay(500L)
        }
    }
    LaunchedEffect(inputState, controlsRevision, item.id) {
        if (inputState == PlayerInputState.ControlsVisible) {
            delay(3_500L)
            if (inputState == PlayerInputState.ControlsVisible) {
                inputState = PlayerInputState.Watching
            }
        }
    }
    LaunchedEffect(zappingInfoVisible, item.id) {
        if (!zappingInfoVisible) return@LaunchedEffect
        delay(1_900L)
        zappingInfoVisible = false
    }

    fun switchTo(itemToPlay: CatalogItem, revealControls: Boolean = true) {
        manuallyPaused = false
        zappingInfoVisible = !revealControls
        inputState = if (revealControls) PlayerInputState.ControlsVisible else PlayerInputState.Watching
        if (revealControls) controlsRevision++
        onSelectItem(itemToPlay)
    }
    fun seekBy(deltaMs: Long) {
        val target = calculateSeekTarget(player.currentPosition, playbackSnapshot.durationMs, deltaMs)
        diagnostics?.logSeekRequest(targetMs = target, canSeek = playbackSnapshot.canSeek, source = "remote")
        if (!playbackSnapshot.canSeek) return
        player.seekTo(target)
        playbackSnapshot.update(player)
    }
    fun seekTo(targetMs: Long) {
        val target = targetMs.coerceIn(0L, playbackSnapshot.durationMs)
        diagnostics?.logSeekRequest(targetMs = target, canSeek = playbackSnapshot.canSeek, source = "timeline")
        if (!playbackSnapshot.canSeek) return
        player.seekTo(target)
        playbackSnapshot.update(player)
    }

    fun retryCurrentContent() {
        connectionTimeoutDismissed = false
        connectionRetryRevision++
        manuallyPaused = false
        inputState = PlayerInputState.Watching
        zappingInfoVisible = false
        if (player.mediaItemCount > 0) {
            player.seekToDefaultPosition(player.currentMediaItemIndex.coerceAtLeast(0))
        }
        player.prepare()
        player.play()
        playbackSnapshot.update(player)
    }

    fun applyInputResult(result: PlayerInputResult): Boolean {
        val previousState = inputState
        inputState = result.state
        if (result.state == PlayerInputState.ControlsVisible) {
            zappingInfoVisible = false
            controlsRevision++
            backPressGuard.markOverlayBackHandled(SystemClock.uptimeMillis())
        } else if (result.state != PlayerInputState.Watching) {
            zappingInfoVisible = false
        }
        if (result.state == PlayerInputState.ExitConfirmVisible && previousState != PlayerInputState.ExitConfirmVisible) {
            exitChoice = PlayerExitChoice.Exit
        }
        if (result.togglePlayback) {
            if (manuallyPaused || !player.playWhenReady) {
                manuallyPaused = false
                player.play()
            } else {
                manuallyPaused = true
                player.pause()
            }
            playbackSnapshot.update(player)
        }
        if (result.seekBack) seekBy(-10_000L)
        if (result.seekForward) seekBy(10_000L)
        if (result.selectNextItem) queue.next?.let { switchTo(it, revealControls = !result.showZappingInfo) }
        if (result.selectPreviousItem) queue.previous?.let { switchTo(it, revealControls = !result.showZappingInfo) }
        if (result.exitRequested) onBack()
        return result.consumeInput
    }
    fun keepControlsAlive() {
        if (inputState == PlayerInputState.ControlsVisible) controlsRevision++
    }
    fun performExitChoice() {
        val action = when (exitChoice) {
            PlayerExitChoice.Exit -> PlayerInputAction.ExitSelected
            PlayerExitChoice.Continue -> PlayerInputAction.ContinueSelected
        }
        applyInputResult(reducePlayerInput(inputState, action))
    }
    fun cycleSpeed() {
        val speeds = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)
        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - playbackSnapshot.playbackSpeed) < 0.01f }
        val nextIndex = ((currentIndex.takeIf { it >= 0 } ?: 0) + 1) % speeds.size
        val nextSpeed = speeds[nextIndex]
        player.setPlaybackSpeed(nextSpeed)
        playbackSnapshot.update(player)
    }

    fun handleRemoteCommand(command: PlayerRemoteCommand): Boolean {
        val action = command.toInputAction() ?: return false
        if (
            command == PlayerRemoteCommand.Back &&
            (inputState == PlayerInputState.ControlsVisible || inputState == PlayerInputState.ContentListVisible)
        ) {
            backPressGuard.markOverlayBackHandled(SystemClock.uptimeMillis())
        } else if (
            command == PlayerRemoteCommand.Back &&
            inputState == PlayerInputState.Watching &&
            backPressGuard.shouldSuppressExitBack(SystemClock.uptimeMillis())
        ) {
            return true
        }
        return applyInputResult(reducePlayerInput(inputState, action))
    }

    fun shouldHandleRemoteKey(keyCode: Int): Boolean {
        val action = playerRemoteCommandForKeyCode(keyCode).toInputAction() ?: return false
        return reducePlayerInput(inputState, action).consumeInput
    }

    fun handleBackPressed() {
        val nowMs = SystemClock.uptimeMillis()
        if (inputState == PlayerInputState.ControlsVisible || inputState == PlayerInputState.ContentListVisible) {
            backPressGuard.markOverlayBackHandled(nowMs)
        } else if (inputState == PlayerInputState.Watching && backPressGuard.shouldSuppressExitBack(nowMs)) {
            return
        }
        applyInputResult(reducePlayerInput(inputState, PlayerInputAction.BackPressed))
    }

    fun handleExitDialogKeyEvent(event: KeyEvent): Boolean = handlePlayerExitDialogKeyEvent(
        event = event,
        visible = exitConfirmVisible,
        onChoiceChange = { exitChoice = it },
        onConfirmChoice = ::performExitChoice,
        onBackExit = {
            applyInputResult(reducePlayerInput(inputState, PlayerInputAction.ExitSelected))
        },
    )

    BackHandler(enabled = !connectionTimeoutVisible) { handleBackPressed() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(playerFocusRequester)
            .focusable(
                enabled = !controlsVisible && !contentListVisible &&
                    !exitConfirmVisible && !connectionTimeoutVisible,
            )
            .onPreviewKeyEvent { event ->
                if (inputState != PlayerInputState.Watching || connectionTimeoutVisible) {
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
        PlayerSurfaceView(
            player = player,
            surfaceKey = item.id,
            controlsVisible = controlsVisible,
            contentListVisible = contentListVisible,
            exitConfirmVisible = exitConfirmVisible || connectionTimeoutVisible,
            playerFocusRequester = playerFocusRequester,
            onOverlayKeyEvent = ::handleExitDialogKeyEvent,
            shouldHandleKeyCode = ::shouldHandleRemoteKey,
            onRemoteCommand = ::handleRemoteCommand,
            modifier = Modifier.fillMaxSize(),
        )
        if (!contentListVisible && !exitConfirmVisible && !connectionTimeoutVisible) {
            PlayerVideoTouchLayer(
                inputState = inputState,
                onInputStateChange = { nextState -> inputState = nextState },
                onControlsShown = {
                    controlsRevision++
                    backPressGuard.markOverlayBackHandled(SystemClock.uptimeMillis())
                },
            )
        }
        AnimatedVisibility(
            visible = osdVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerOsdScrims()
        }
        AnimatedVisibility(
            visible = (shouldShowBufferingIndicator(playbackSnapshot.playbackState, manuallyPaused) ||
                connectionTimeoutUi.showLoading) && !connectionTimeoutVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            PlayerConnectionLoadingOverlay(message = connectionTimeoutUi.message)
        }
        AnimatedVisibility(
            visible = shouldShowPlayerContentInfo(controlsVisible, PlayerExitDialogState.Hidden) &&
                !contentListVisible && !exitConfirmVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            PlayerInfoOverlay(info = contentInfo)
        }
        AnimatedVisibility(
            visible = zappingInfoActive && !contentListVisible && !exitConfirmVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            PlayerZappingInfoOverlay(info = contentInfo)
        }
        AnimatedVisibility(
            visible = osdVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PlayerControlsOverlay(
                isPlaying = shouldPresentAsPlaying(player.playWhenReady, manuallyPaused),
                positionMs = playbackSnapshot.currentPositionMs,
                durationMs = playbackSnapshot.durationMs,
                speed = playbackSnapshot.playbackSpeed,
                canSeek = playbackSnapshot.canSeek,
                onSeekBack = { seekBy(-10_000L) },
                onSeekTo = ::seekTo,
                onTogglePlayback = {
                    if (manuallyPaused || !player.playWhenReady) {
                        manuallyPaused = false
                        player.play()
                    } else {
                        manuallyPaused = true
                        player.pause()
                    }
                    playbackSnapshot.update(player)
                },
                onSeekForward = { seekBy(10_000L) },
                onCycleSpeed = ::cycleSpeed,
                onUserInteraction = ::keepControlsAlive,
            )
        }
        if (contentListVisible) {
            PlayerContentListOverlay(
                queue = queue,
                onSelectItem = ::switchTo,
                onDismiss = {
                    backPressGuard.markOverlayBackHandled(SystemClock.uptimeMillis())
                    applyInputResult(reducePlayerInput(inputState, PlayerInputAction.BackPressed))
                },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        if (connectionTimeoutVisible) {
            PlayerConnectionTimeoutDialog(
                onRetry = ::retryCurrentContent,
                onDismiss = { connectionTimeoutDismissed = true },
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
