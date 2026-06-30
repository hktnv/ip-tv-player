package com.hktnv.iptvbox.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.accentSubtle
import com.hktnv.iptvbox.core.designsystem.accentText
import com.hktnv.iptvbox.core.designsystem.focusBorder
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
    onSeekBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward: () -> Unit,
    onCycleSpeed: () -> Unit,
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
            )
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                PlayerIconControl(
                    icon = Icons.Filled.Replay10,
                    contentDescription = "10 saniye geri",
                    enabled = canSeek,
                    onClick = {
                        onUserInteraction()
                        onSeekBack()
                    },
                )
                PlayerIconControl(
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Duraklat" else "Devam Et",
                    modifier = Modifier.focusRequester(playFocusRequester),
                    emphasized = true,
                    onClick = {
                        onUserInteraction()
                        onTogglePlayback()
                    },
                )
                PlayerIconControl(
                    icon = Icons.Filled.Forward10,
                    contentDescription = "10 saniye ileri",
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
                    icon = Icons.Filled.Subtitles,
                    contentDescription = "Altyazı",
                    enabled = false,
                    onClick = onUserInteraction,
                )
                PlayerIconControl(
                    icon = Icons.Filled.Audiotrack,
                    contentDescription = "Ses",
                    enabled = false,
                    onClick = onUserInteraction,
                )
                PlayerIconControl(
                    icon = Icons.Filled.Settings,
                    contentDescription = "Ayarlar",
                    enabled = false,
                    onClick = onUserInteraction,
                )
            }
        }
    }
}

@Composable
private fun PlayerTimeline(
    positionMs: Long,
    durationMs: Long,
    canSeek: Boolean,
) {
    val progress = if (canSeek && durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = if (canSeek) formatPlayerTime(positionMs) else "Canlı",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(58.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(3.dp),
            color = if (canSeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.accentText,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            drawStopIndicator = {},
        )
        Text(
            text = if (canSeek) formatPlayerTime(durationMs) else "Yayın",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(70.dp),
        )
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
    val borderColor = when {
        focused -> MaterialTheme.colorScheme.focusBorder
        else -> TvRestingBorder
    }
    Surface(
        modifier = modifier
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = enabled)
            .tvClickable(enabled = enabled, onClick = onClick),
        color = surfaceColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
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
                text = formatPlayerSpeed(speed),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private fun formatPlayerSpeed(speed: Float): String {
    return if (speed == 1f) "1x" else "${speed}x"
}

private fun formatPlayerTime(valueMs: Long): String {
    if (valueMs <= 0L) return "00:00"
    val totalSeconds = valueMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
