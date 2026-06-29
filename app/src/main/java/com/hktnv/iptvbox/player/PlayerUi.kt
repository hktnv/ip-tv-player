package com.hktnv.iptvbox.player

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.player.MediaPlayerFactory

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
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    var inputState by remember { mutableStateOf(PlayerInputState.Watching) }
    val controlsVisible = inputState == PlayerInputState.ControlsVisible
    val contentListVisible = inputState == PlayerInputState.ContentListVisible
    val exitConfirmVisible = inputState == PlayerInputState.ExitConfirmVisible
    val controllerVisibilityListener = PlayerView.ControllerVisibilityListener { visibility: Int ->
        val action = if (visibility == View.VISIBLE) {
            PlayerInputAction.ControllerShown
        } else {
            PlayerInputAction.ControllerHidden
        }
        inputState = reducePlayerInput(inputState, action).state
    }

    fun switchTo(itemToPlay: CatalogItem) {
        inputState = PlayerInputState.ControlsVisible
        onSelectItem(itemToPlay)
    }

    fun applyInputResult(result: PlayerInputResult, playerView: PlayerView? = null): Boolean {
        inputState = result.state
        if (result.togglePlayback) {
            if (player.isPlaying) player.pause() else player.play()
        }
        if (result.selectNextItem) queue.next?.let(::switchTo)
        if (result.selectPreviousItem) queue.previous?.let(::switchTo)
        if (result.showControls) playerView?.showController()
        if (result.exitRequested) onBack()
        return result.consumeInput
    }

    fun handleRemoteCommand(command: PlayerRemoteCommand, playerView: PlayerView): Boolean {
        val action = command.toInputAction() ?: return false
        return applyInputResult(reducePlayerInput(inputState, action), playerView)
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
            .background(MaterialTheme.colorScheme.background),
    ) {
        AndroidView(
            factory = { ctx ->
                TvRemotePlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    keepScreenOn = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setControllerVisibilityListener(controllerVisibilityListener)
                    shouldInterceptRemoteKeys = {
                        inputState != PlayerInputState.ContentListVisible &&
                            inputState != PlayerInputState.ExitConfirmVisible
                    }
                    onRemoteKeyUp = { keyCode ->
                        handleRemoteCommand(
                            command = playerRemoteCommandForKeyCode(keyCode),
                            playerView = this,
                        )
                    }
                    requestFocus()
                }
            },
            update = { view ->
                view.player = player
                view.setControllerVisibilityListener(controllerVisibilityListener)
                view.apply {
                    shouldInterceptRemoteKeys = {
                        inputState != PlayerInputState.ContentListVisible &&
                            inputState != PlayerInputState.ExitConfirmVisible
                    }
                    onRemoteKeyUp = { keyCode ->
                        handleRemoteCommand(
                            command = playerRemoteCommandForKeyCode(keyCode),
                            playerView = view,
                        )
                    }
                }
                if (!contentListVisible && !exitConfirmVisible) {
                    view.requestFocus()
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
    var shouldInterceptRemoteKeys: () -> Boolean = { true }
    var onRemoteKeyUp: (keyCode: Int) -> Boolean = { false }

    init {
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val command = playerRemoteCommandForKeyCode(event.keyCode)
        if (command != PlayerRemoteCommand.None && shouldInterceptRemoteKeys()) {
            if (event.action == KeyEvent.ACTION_DOWN) return true
            if (event.action == KeyEvent.ACTION_UP) return onRemoteKeyUp(event.keyCode)
        }
        return super.dispatchKeyEvent(event)
    }
}
