package com.hktnv.iptvbox.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hktnv.iptvbox.core.model.CatalogItem

@Composable
internal fun BoxScope.PlayerOverlayHost(
    osdVisible: Boolean,
    loadingVisible: Boolean,
    loadingMessage: String?,
    controlsVisible: Boolean,
    contentListVisible: Boolean,
    exitConfirmVisible: Boolean,
    zappingInfoActive: Boolean,
    connectionTimeoutVisible: Boolean,
    contentInfo: PlayerContentInfo,
    queue: PlayerPlaybackQueue,
    exitChoice: PlayerExitChoice,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    canSeek: Boolean,
    onSeekBack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward: () -> Unit,
    onCycleSpeed: () -> Unit,
    onControlsInteraction: () -> Unit,
    onSelectContentListItem: (CatalogItem) -> Unit,
    onDismissContentList: () -> Unit,
    onConnectionRetry: () -> Unit,
    onConnectionDismiss: () -> Unit,
    onExitChoiceChange: (PlayerExitChoice) -> Unit,
    onExit: () -> Unit,
    onContinue: () -> Unit,
) {
    AnimatedVisibility(
        visible = osdVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        PlayerOsdScrims()
    }
    AnimatedVisibility(
        visible = loadingVisible && !connectionTimeoutVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center),
    ) {
        PlayerConnectionLoadingOverlay(message = loadingMessage)
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
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            speed = speed,
            canSeek = canSeek,
            onSeekBack = onSeekBack,
            onSeekTo = onSeekTo,
            onTogglePlayback = onTogglePlayback,
            onSeekForward = onSeekForward,
            onCycleSpeed = onCycleSpeed,
            onUserInteraction = onControlsInteraction,
        )
    }
    if (contentListVisible) {
        PlayerContentListOverlay(
            queue = queue,
            onSelectItem = onSelectContentListItem,
            onDismiss = onDismissContentList,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
    if (connectionTimeoutVisible) {
        PlayerConnectionTimeoutDialog(
            onRetry = onConnectionRetry,
            onDismiss = onConnectionDismiss,
        )
    }
    if (exitConfirmVisible) {
        PlayerExitConfirmationDialog(
            selectedChoice = exitChoice,
            onChoiceChange = onExitChoiceChange,
            onExit = onExit,
            onContinue = onContinue,
        )
    }
}
