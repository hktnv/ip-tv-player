package com.hktnv.iptvbox.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.accentText
import com.hktnv.iptvbox.core.designsystem.focusBorder
import kotlinx.coroutines.delay

@Composable
internal fun PlayerTimeline(
    positionMs: Long,
    durationMs: Long,
    canSeek: Boolean,
    onSeekTo: (Long) -> Unit,
    timelineExitFocusRequester: FocusRequester,
    onUserInteraction: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var previewPositionMs by remember { mutableStateOf<Long?>(null) }
    var previewRevision by remember { mutableIntStateOf(0) }
    val shownPositionMs = previewPositionMs ?: positionMs
    val progress = if (canSeek && durationMs > 0L) {
        (shownPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    LaunchedEffect(previewRevision) {
        val target = previewPositionMs ?: return@LaunchedEffect
        delay(TIMELINE_SEEK_COMMIT_DEBOUNCE_MS)
        if (previewPositionMs == target) {
            onSeekTo(target)
            previewPositionMs = null
        }
    }
    fun updatePreview(deltaMs: Long) {
        previewPositionMs = calculateSeekTarget(shownPositionMs, durationMs, deltaMs)
        previewRevision++
        onUserInteraction()
    }
    fun commitPreview(): Boolean {
        val target = previewPositionMs ?: return false
        onSeekTo(target)
        previewPositionMs = null
        previewRevision++
        return true
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (canSeek) formatPlayerTime(shownPositionMs) else stringResource(R.string.player_live_label),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(58.dp),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if ((focused || previewPositionMs != null) && canSeek) {
                SeekTooltip(
                    text = formatPlayerTime(shownPositionMs),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(bottom = 24.dp),
                )
            }
            Slider(
                value = progress,
                onValueChange = { value ->
                    if (!canSeek || durationMs <= 0L) return@Slider
                    previewPositionMs = (durationMs * value).toLong().coerceIn(0L, durationMs)
                    onUserInteraction()
                },
                onValueChangeFinished = {
                    previewPositionMs?.let(onSeekTo)
                    previewPositionMs = null
                },
                enabled = canSeek,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onSurface,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                    disabledThumbColor = MaterialTheme.colorScheme.accentText,
                    disabledActiveTrackColor = MaterialTheme.colorScheme.accentText,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .focusProperties {
                        up = timelineExitFocusRequester
                        down = timelineExitFocusRequester
                    }
                    .focusable(enabled = canSeek)
                    .onPreviewKeyEvent { event ->
                        if (!canSeek) return@onPreviewKeyEvent false
                        if (event.key.isTimelineConfirmKey()) {
                            if (previewPositionMs == null) return@onPreviewKeyEvent false
                            if (event.type == KeyEventType.KeyDown) {
                                onUserInteraction()
                                commitPreview()
                            }
                            return@onPreviewKeyEvent true
                        }
                        val action = resolvePlayerTimelineKeyAction(event.key.toTimelineRemoteKey(), canSeek)
                        if (action == PlayerTimelineKeyAction.None) return@onPreviewKeyEvent false
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        when (action) {
                            PlayerTimelineKeyAction.PreviewBack -> {
                                updatePreview(-10_000L)
                                true
                            }
                            PlayerTimelineKeyAction.PreviewForward -> {
                                updatePreview(10_000L)
                                true
                            }
                            PlayerTimelineKeyAction.ExitTimeline -> {
                                onUserInteraction()
                                runCatching { timelineExitFocusRequester.requestFocus() }
                                true
                            }
                            PlayerTimelineKeyAction.None -> false
                        }
                    },
            )
        }
        Text(
            text = if (canSeek) formatPlayerTime(durationMs) else stringResource(R.string.player_stream_label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(70.dp),
        )
    }
}

private const val TIMELINE_SEEK_COMMIT_DEBOUNCE_MS = 650L

private fun Key.toTimelineRemoteKey(): PlayerTimelineRemoteKey {
    return when (this) {
        Key.DirectionLeft -> PlayerTimelineRemoteKey.Left
        Key.DirectionRight -> PlayerTimelineRemoteKey.Right
        Key.DirectionUp -> PlayerTimelineRemoteKey.Up
        Key.DirectionDown -> PlayerTimelineRemoteKey.Down
        else -> PlayerTimelineRemoteKey.Other
    }
}

private fun Key.isTimelineConfirmKey(): Boolean {
    return this == Key.DirectionCenter || this == Key.Enter || this == Key.NumPadEnter
}

@Composable
private fun SeekTooltip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.focusBorder.copy(alpha = 0.55f)),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
