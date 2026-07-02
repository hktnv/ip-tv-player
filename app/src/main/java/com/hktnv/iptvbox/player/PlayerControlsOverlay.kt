package com.hktnv.iptvbox.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.accentSubtle
import com.hktnv.iptvbox.core.designsystem.accentText
import com.hktnv.iptvbox.core.designsystem.focusBorder
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift

@Composable
internal fun PlayerControlsOverlay(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    canSeek: Boolean,
    favorite: Boolean,
    relatedItems: List<CatalogItem>,
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
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { playFocusRequester.requestFocus() }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp, vertical = 30.dp)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) onUserInteraction()
                false
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
                items = relatedItems,
                onSelectItem = {
                    onUserInteraction()
                    onSelectRelatedItem(it)
                },
            )
        }
    }
}

@Composable
private fun PlayerIconControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val surfaceColor = when {
        emphasized && enabled -> MaterialTheme.colorScheme.accentSubtle.copy(alpha = 0.92f)
        focused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.42f else 0.18f)
    }
    Surface(
        modifier = modifier
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = enabled)
            .tvClickable(enabled = enabled, onClick = onClick),
        color = surfaceColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.focusBorder else TvRestingBorder,
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                emphasized -> MaterialTheme.colorScheme.accentText
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier
                .size(50.dp)
                .padding(13.dp),
        )
    }
}

@Composable
private fun PlayerSpeedControl(
    speed: Float,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .tvClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (focused) 0.78f else 0.42f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(23.dp),
            )
            Text(
                text = if (speed == 1f) "1x" else "${speed}x",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
