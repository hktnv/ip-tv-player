package com.hktnv.iptvbox.ui.common
import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val TvFocusBorder = Color(0xFFE7F3FF)
internal val TvFocusPanel = Color(0xFF17283A)
internal val TvSelectedPanel = Color(0xFF10251F)
internal val TvRestingBorder = Color(0xFF263240)

@Composable
internal fun Modifier.tvFocusLift(
    focused: Boolean,
    scale: Float = 1.025f,
    liftPx: Float = -5f,
): Modifier {
    val animatedScale by animateFloatAsState(
        targetValue = if (focused) scale else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "tvFocusScale",
    )
    val animatedLift by animateFloatAsState(
        targetValue = if (focused) liftPx else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "tvFocusLift",
    )
    return graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
        translationY = animatedLift
    }
}

@Composable
internal fun tvFocusElevation(
    focused: Boolean,
    resting: Dp = 1.dp,
    focusedElevation: Dp = 16.dp,
): Dp {
    val elevation by animateDpAsState(
        targetValue = if (focused) focusedElevation else resting,
        animationSpec = tween(durationMillis = 140),
        label = "tvFocusElevation",
    )
    return elevation
}

internal fun Modifier.tvClickable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return clickable(enabled = false, onClick = onClick)
    return composed {
        var longPressJob by remember { mutableStateOf<Job?>(null) }
        val clickGuard = remember { TvLongPressClickGuard() }
        val scope = rememberCoroutineScope()
        fun dispatchLongClick() {
            clickGuard.markLongClick(SystemClock.uptimeMillis())
            onLongClick?.invoke()
        }
        fun dispatchClickIfAllowed() {
            if (clickGuard.consumeClick(SystemClock.uptimeMillis())) onClick()
        }
        val pressHandler = Modifier.onPreviewKeyEvent { event ->
            if (!event.key.isSelectKey()) return@onPreviewKeyEvent false
            when {
                event.type == KeyEventType.KeyDown &&
                    onLongClick != null -> {
                    if (event.isNativeLongSelectPress() && !clickGuard.longClickHandled) {
                        longPressJob?.cancel()
                        longPressJob = null
                        dispatchLongClick()
                        return@onPreviewKeyEvent true
                    }
                    if (longPressJob == null && !clickGuard.longClickHandled) {
                        longPressJob = scope.launch {
                            delay(360L)
                            dispatchLongClick()
                            longPressJob = null
                        }
                    }
                    true
                }
                event.type == KeyEventType.KeyUp -> {
                    longPressJob?.cancel()
                    longPressJob = null
                    dispatchClickIfAllowed()
                    true
                }
                else -> false
            }
        }
        then(pressHandler).tvPointerClickable(
            onClick = { dispatchClickIfAllowed() },
            onLongClick = onLongClick?.let { { dispatchLongClick() } },
        )
    }
}

internal class TvLongPressClickGuard(
    private val suppressWindowMs: Long = 900L,
) {
    var longClickHandled: Boolean = false
        private set
    private var suppressClickUntilMs: Long = 0L

    fun markLongClick(nowMs: Long) {
        longClickHandled = true
        suppressClickUntilMs = nowMs + suppressWindowMs
    }

    fun consumeClick(nowMs: Long): Boolean {
        if (longClickHandled || nowMs < suppressClickUntilMs) {
            longClickHandled = false
            return false
        }
        return true
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.tvPointerClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
): Modifier {
    return combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

private fun Key.isSelectKey(): Boolean {
    return this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter
}

private fun androidx.compose.ui.input.key.KeyEvent.isNativeLongSelectPress(): Boolean {
    return nativeKeyEvent.isLongPress || nativeKeyEvent.repeatCount > 0
}
