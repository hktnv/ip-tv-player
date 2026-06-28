package com.hktnv.iptvbox.ui.common
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.ui.media.label

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
    onClick: () -> Unit,
): Modifier {
    val keyHandler = if (enabled) {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key.isSelectKey()) {
                onClick()
                true
            } else {
                false
            }
        }
    } else {
        Modifier
    }
    return this
        .then(keyHandler)
        .clickable(enabled = enabled, onClick = onClick)
}

private fun Key.isSelectKey(): Boolean {
    return this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter
}
