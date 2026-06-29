package com.hktnv.iptvbox.player

import android.content.Context
import android.view.KeyEvent
import android.view.View
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

    var exitDialogState by remember(item.id) {
        mutableStateOf(PlayerExitDialogState.Hidden)
    }
    var controlsVisible by remember(item.id) {
        mutableStateOf(false)
    }
    var contentListVisible by remember(item.id) {
        mutableStateOf(false)
    }
    val controllerVisibilityListener = PlayerView.ControllerVisibilityListener { visibility: Int ->
        controlsVisible = visibility == View.VISIBLE
    }
    fun applyExitAction(action: PlayerExitAction) {
        val result = reducePlayerExitDialog(exitDialogState, action)
        exitDialogState = result.state
        if (result.exitRequested) {
            onBack()
        }
    }

    fun switchTo(itemToPlay: CatalogItem) {
        contentListVisible = false
        controlsVisible = true
        onSelectItem(itemToPlay)
    }

    fun handleRemoteCommand(command: PlayerRemoteCommand, playerView: PlayerView): Boolean {
        if (contentListVisible || exitDialogState == PlayerExitDialogState.Visible) return false
        return when (command) {
            PlayerRemoteCommand.TogglePlayPause -> {
                if (player.isPlaying) player.pause() else player.play()
                controlsVisible = true
                playerView.showController()
                true
            }
            PlayerRemoteCommand.NextItem -> {
                queue.next?.let(::switchTo)
                controlsVisible = true
                playerView.showController()
                true
            }
            PlayerRemoteCommand.PreviousItem -> {
                queue.previous?.let(::switchTo)
                controlsVisible = true
                playerView.showController()
                true
            }
            PlayerRemoteCommand.OpenContentList -> {
                contentListVisible = true
                controlsVisible = true
                playerView.showController()
                true
            }
            PlayerRemoteCommand.None -> false
        }
    }

    BackHandler(enabled = contentListVisible) {
        contentListVisible = false
    }
    BackHandler(enabled = !contentListVisible && exitDialogState == PlayerExitDialogState.Hidden) {
        applyExitAction(PlayerExitAction.BackPressed)
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
                        !contentListVisible && exitDialogState == PlayerExitDialogState.Hidden
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
                        !contentListVisible && exitDialogState == PlayerExitDialogState.Hidden
                    }
                    onRemoteKeyUp = { keyCode ->
                        handleRemoteCommand(
                            command = playerRemoteCommandForKeyCode(keyCode),
                            playerView = view,
                        )
                    }
                }
                if (!contentListVisible && exitDialogState == PlayerExitDialogState.Hidden) {
                    view.requestFocus()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (shouldShowPlayerContentInfo(controlsVisible, exitDialogState) && !contentListVisible) {
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
                onDismiss = { contentListVisible = false },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        if (exitDialogState == PlayerExitDialogState.Visible) {
            PlayerExitConfirmationDialog(
                onExit = { applyExitAction(PlayerExitAction.ExitSelected) },
                onContinue = { applyExitAction(PlayerExitAction.ContinueSelected) },
            )
        }
    }
}

private class TvRemotePlayerView(context: Context) : PlayerView(context) {
    var shouldInterceptRemoteKeys: () -> Boolean = { true }
    var onRemoteKeyUp: (keyCode: Int) -> Boolean = { false }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val command = playerRemoteCommandForKeyCode(event.keyCode)
        if (command != PlayerRemoteCommand.None && shouldInterceptRemoteKeys()) {
            if (event.action == KeyEvent.ACTION_DOWN) return true
            if (event.action == KeyEvent.ACTION_UP) return onRemoteKeyUp(event.keyCode)
        }
        return super.dispatchKeyEvent(event)
    }
}
