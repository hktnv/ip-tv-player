package com.hktnv.iptvbox.player

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val contentInfo = remember(item.id) { item.toPlayerContentInfo() }
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

    BackHandler {
        applyExitAction(PlayerExitAction.BackPressed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    keepScreenOn = true
                    setControllerVisibilityListener(controllerVisibilityListener)
                }
            },
            update = { view ->
                view.player = player
                view.setControllerVisibilityListener(controllerVisibilityListener)
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (shouldShowPlayerContentInfo(controlsVisible, exitDialogState)) {
            PlayerInfoOverlay(
                info = contentInfo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 28.dp, top = 28.dp),
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
