package com.hktnv.iptvbox.player

import android.view.KeyEvent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.media3.common.Player
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import kotlinx.coroutines.delay

@Composable
internal fun PlayerScreen(
    item: CatalogItem,
    headers: Map<String, String>,
    playbackItems: List<CatalogItem>,
    discoveryItems: List<CatalogItem>,
    playerUiMode: PlayerUiMode,
    isFavorite: Boolean,
    onSelectItem: (CatalogItem) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    PlayerOrientationLock()
    val player = rememberIptvPlayerSession(context, headers, item)
    val diagnosticsContext = remember(item.id, item.streamUrl, playerUiMode) {
        if (isPlayerDiagnosticEnabled) item.toPlayerDiagnosticContext(playerUiMode) else null
    }
    val queue = remember(playbackItems, item.id) {
        buildPlayerPlaybackQueue(playbackItems, item)
    }
    val relatedContextItems = remember(discoveryItems, queue.items) {
        val uniqueItems = LinkedHashMap<String, CatalogItem>()
        discoveryItems.forEach { uniqueItems[it.id] = it }
        queue.items.forEach { uniqueItems[it.id] = it }
        uniqueItems.values.toList()
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
        item.toPlayerContentInfo(previousItem = queue.previous, nextItem = queue.next)
    }
    val liveContent = remember(item.id) {
        item.kind == ContentKind.LIVE_CHANNEL || item.kind == ContentKind.RADIO
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
        onDispose { player.removeListener(listener) }
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
    var seekLoadingVisible by remember(player, item.id) { mutableStateOf(false) }
    var seekLoadingRevision by remember(player, item.id) { mutableIntStateOf(0) }
    val controlsVisible = inputState == PlayerInputState.ControlsVisible
    val exitConfirmVisible = inputState == PlayerInputState.ExitConfirmVisible
    val connectionTimeoutVisible = connectionTimeoutUi.showTimeoutDialog
    val presentedAsPlaying = shouldPresentAsPlaying(player.playWhenReady, manuallyPaused)
    val autoHideOsd = shouldAutoHidePlayerOsd(
        controlsVisible = controlsVisible,
        playWhenReady = player.playWhenReady,
        manuallyPaused = manuallyPaused,
    )
    val osdVisible = controlsVisible && !exitConfirmVisible && !connectionTimeoutVisible
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
    LaunchedEffect(inputState, controlsRevision, item.id, autoHideOsd) {
        if (autoHideOsd) {
            delay(3_500L)
            if (
                shouldAutoHidePlayerOsd(
                    controlsVisible = inputState == PlayerInputState.ControlsVisible,
                    playWhenReady = player.playWhenReady,
                    manuallyPaused = manuallyPaused,
                )
            ) {
                inputState = PlayerInputState.Watching
            }
        }
    }
    LaunchedEffect(zappingInfoVisible, item.id) {
        if (!zappingInfoVisible) return@LaunchedEffect
        delay(1_900L)
        zappingInfoVisible = false
    }
    LaunchedEffect(seekLoadingRevision, item.id) {
        if (seekLoadingRevision == 0) return@LaunchedEffect
        delay(650L)
        if (playbackSnapshot.playbackState != Player.STATE_BUFFERING) {
            seekLoadingVisible = false
        }
    }
    LaunchedEffect(playbackSnapshot.playbackState, item.id) {
        if (
            seekLoadingVisible &&
            (playbackSnapshot.playbackState == Player.STATE_READY ||
                playbackSnapshot.playbackState == Player.STATE_ENDED)
        ) {
            delay(250L)
            seekLoadingVisible = false
        }
    }

    fun markSeekLoading() {
        seekLoadingVisible = true
        seekLoadingRevision++
    }
    fun switchTo(itemToPlay: CatalogItem, revealControls: Boolean = true) {
        manuallyPaused = false
        seekLoadingVisible = false
        zappingInfoVisible = !revealControls
        inputState = if (revealControls) PlayerInputState.ControlsVisible else PlayerInputState.Watching
        if (revealControls) controlsRevision++
        onSelectItem(itemToPlay)
    }
    fun seekBy(deltaMs: Long) {
        val target = calculateSeekTarget(player.currentPosition, playbackSnapshot.durationMs, deltaMs)
        diagnostics?.logSeekRequest(targetMs = target, canSeek = playbackSnapshot.canSeek, source = "remote")
        if (!playbackSnapshot.canSeek) return
        markSeekLoading()
        player.seekTo(target)
        playbackSnapshot.update(player)
    }
    fun seekTo(targetMs: Long) {
        val target = targetMs.coerceIn(0L, playbackSnapshot.durationMs)
        diagnostics?.logSeekRequest(targetMs = target, canSeek = playbackSnapshot.canSeek, source = "timeline")
        if (!playbackSnapshot.canSeek) return
        markSeekLoading()
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
        return applyInputResult(reducePlayerInput(inputState, action))
    }

    fun shouldHandleRemoteKey(keyCode: Int): Boolean {
        val action = playerRemoteCommandForKeyCode(keyCode).toInputAction() ?: return false
        return reducePlayerInput(inputState, action).consumeInput
    }
    fun handleBackPressed() {
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
                enabled = !controlsVisible &&
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
            exitConfirmVisible = exitConfirmVisible || connectionTimeoutVisible,
            playerFocusRequester = playerFocusRequester,
            onOverlayKeyEvent = ::handleExitDialogKeyEvent,
            shouldHandleKeyCode = ::shouldHandleRemoteKey,
            onRemoteCommand = ::handleRemoteCommand,
            modifier = Modifier.fillMaxSize(),
        )
        if (!exitConfirmVisible && !connectionTimeoutVisible) {
            PlayerVideoTouchLayer(
                inputState = inputState,
                onInputStateChange = { nextState -> inputState = nextState },
                onControlsShown = {
                    controlsRevision++
                },
            )
        }
        PlayerOverlayHost(
            osdVisible = osdVisible,
            loadingVisible = shouldShowPlayerLoadingIndicator(
                playbackState = playbackSnapshot.playbackState,
                manuallyPaused = manuallyPaused,
                connectionLoading = connectionTimeoutUi.showLoading,
                seekLoading = seekLoadingVisible,
            ),
            loadingMessage = connectionTimeoutUi.message,
            controlsVisible = controlsVisible,
            exitConfirmVisible = exitConfirmVisible,
            zappingInfoActive = zappingInfoActive,
            connectionTimeoutVisible = connectionTimeoutVisible,
            contentInfo = contentInfo,
            currentItem = item,
            relatedContextItems = relatedContextItems,
            exitChoice = exitChoice,
            isPlaying = presentedAsPlaying,
            positionMs = playbackSnapshot.currentPositionMs,
            durationMs = playbackSnapshot.durationMs,
            speed = playbackSnapshot.playbackSpeed,
            canSeek = playbackSnapshot.canSeek,
            liveContent = liveContent,
            favorite = isFavorite,
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
            onControlsInteraction = ::keepControlsAlive,
            onToggleFavorite = { onToggleFavorite(item) },
            onSelectRelatedItem = ::switchTo,
            onConnectionRetry = ::retryCurrentContent,
            onConnectionDismiss = { connectionTimeoutDismissed = true },
            onExitChoiceChange = { exitChoice = it },
            onExit = {
                applyInputResult(reducePlayerInput(inputState, PlayerInputAction.ExitSelected))
            },
            onContinue = {
                applyInputResult(reducePlayerInput(inputState, PlayerInputAction.ContinueSelected))
            },
        )
    }
}
