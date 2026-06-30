package com.hktnv.iptvbox.ui.settings
import androidx.compose.material3.MaterialTheme
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.common.TvFocusPanel

@Composable
internal fun SettingsFocusPanel(
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester?,
    nextFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    onConfirm: (() -> Unit)? = null,
    onRequestSideMenu: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.012f, liftPx = -3f)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> previousFocusRequester.requestFocusSafely()
                    Key.DirectionDown -> nextFocusRequester.requestFocusSafely()
                    Key.DirectionLeft -> {
                        onRequestSideMenu?.invoke()
                        onRequestSideMenu != null
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        onConfirm?.invoke()
                        onConfirm != null
                    }
                    else -> false
                }
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .focusable(),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) TvFocusBorder else MaterialTheme.colorScheme.surfaceBorder,
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 10.dp),
    ) {
        Box(Modifier.padding(18.dp)) {
            content()
        }
    }
}

private fun FocusRequester?.requestFocusSafely(): Boolean {
    return this != null && runCatching { requestFocus() }.getOrDefault(false)
}
