package com.hktnv.iptvbox.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.model.CatalogItem

@Composable
internal fun PlayerControlsOverlay(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    canSeek: Boolean,
    favorite: Boolean,
    currentItem: CatalogItem,
    relatedContextItems: List<CatalogItem>,
    onSeekBack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelectRelatedItem: (CatalogItem) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playFocusRequester = remember { FocusRequester() }
    val relatedFocusRequester = remember { FocusRequester() }
    var relatedExpanded by remember(currentItem.id) { mutableStateOf(false) }
    var selectedRelatedOptionId by remember(currentItem.id) { mutableStateOf<String?>(null) }
    val relatedModel = remember(currentItem.id, relatedContextItems, selectedRelatedOptionId) {
        buildPlayerRelatedContentModel(
            currentItem = currentItem,
            contextItems = relatedContextItems,
            selectedOptionId = selectedRelatedOptionId,
        )
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { playFocusRequester.requestFocus() }
    }
    LaunchedEffect(relatedExpanded, relatedModel) {
        if (relatedExpanded && relatedModel.hasContent) {
            withFrameNanos { }
            runCatching { relatedFocusRequester.requestFocus() }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp, vertical = 30.dp)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) onUserInteraction()
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown &&
                    !relatedExpanded &&
                    relatedModel.hasContent
                ) {
                    relatedExpanded = true
                    true
                } else {
                    false
                }
            },
    ) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            PlayerTimeline(
                positionMs = positionMs,
                durationMs = durationMs,
                canSeek = canSeek,
                onSeekTo = {
                    onUserInteraction()
                    onSeekTo(it)
                },
                timelineExitFocusRequester = playFocusRequester,
                onUserInteraction = onUserInteraction,
            )
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                PlayerIconControl(
                    icon = Icons.Filled.Replay10,
                    contentDescription = stringResource(R.string.player_seek_back_10),
                    enabled = canSeek,
                    onClick = {
                        onUserInteraction()
                        onSeekBack()
                    },
                )
                PlayerIconControl(
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.player_pause else R.string.player_resume,
                    ),
                    modifier = Modifier.focusRequester(playFocusRequester),
                    emphasized = true,
                    onClick = {
                        onUserInteraction()
                        onTogglePlayback()
                    },
                )
                PlayerIconControl(
                    icon = Icons.Filled.Forward10,
                    contentDescription = stringResource(R.string.player_seek_forward_10),
                    enabled = canSeek,
                    onClick = {
                        onUserInteraction()
                        onSeekForward()
                    },
                )
                PlayerSpeedControl(
                    speed = speed,
                    onClick = {
                        onUserInteraction()
                        onCycleSpeed()
                    },
                )
                PlayerIconControl(
                    icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (favorite) R.string.player_favorite_remove else R.string.player_favorite_add,
                    ),
                    emphasized = favorite,
                    onClick = {
                        onUserInteraction()
                        onToggleFavorite()
                    },
                )
                PlayerIconControl(
                    icon = Icons.Filled.Subtitles,
                    contentDescription = stringResource(R.string.player_subtitles),
                    enabled = false,
                    onClick = onUserInteraction,
                )
                PlayerIconControl(
                    icon = Icons.Filled.Audiotrack,
                    contentDescription = stringResource(R.string.player_audio),
                    enabled = false,
                    onClick = onUserInteraction,
                )
                PlayerIconControl(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.player_settings),
                    enabled = false,
                    onClick = onUserInteraction,
                )
            }
            PlayerRelatedContentRail(
                model = relatedModel,
                expanded = relatedExpanded,
                initialFocusRequester = relatedFocusRequester,
                onExpand = {
                    onUserInteraction()
                    relatedExpanded = true
                },
                onCollapse = {
                    onUserInteraction()
                    relatedExpanded = false
                    runCatching { playFocusRequester.requestFocus() }
                },
                onOptionSelected = {
                    onUserInteraction()
                    selectedRelatedOptionId = it
                },
                onSelectItem = {
                    onUserInteraction()
                    relatedExpanded = false
                    onSelectRelatedItem(it)
                },
            )
        }
    }
}
