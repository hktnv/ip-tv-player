package com.hktnv.iptvbox.player

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (exitDialogState == PlayerExitDialogState.Visible) {
            PlayerExitConfirmationDialog(
                onExit = { applyExitAction(PlayerExitAction.ExitSelected) },
                onContinue = { applyExitAction(PlayerExitAction.ContinueSelected) },
            )
        }
    }
}
