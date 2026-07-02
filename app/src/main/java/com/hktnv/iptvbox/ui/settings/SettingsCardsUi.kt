package com.hktnv.iptvbox.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel

@Composable
internal fun SettingsActionCard(
    title: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    caption: String? = null,
    trailingText: String? = null,
    focusRequester: FocusRequester,
    padding: Dp,
    leftFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var selectPressed by remember { mutableStateOf(false) }
    SettingsCardSurface(
        focused = focused,
        modifier = modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        leftFocusRequester.requestFocusSafely() || onRequestSideMenu.invokeIfPresent()
                    }
                    Key.DirectionRight -> {
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        rightFocusRequester.requestFocusSafely()
                    }
                    Key.DirectionUp -> {
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        upFocusRequester.requestFocusSafely()
                    }
                    Key.DirectionDown -> {
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        downFocusRequester.requestFocusSafely()
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        when (event.type) {
                            KeyEventType.KeyDown -> selectPressed = true
                            KeyEventType.KeyUp -> {
                                if (selectPressed) onClick()
                                selectPressed = false
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) selectPressed = false
            }
            .focusable()
            .clickable(onClick = onClick),
    ) {
        SettingsCardContent(
            title = title,
            body = body,
            caption = caption,
            icon = icon,
            padding = padding,
            trailingText = trailingText,
            showChevron = true,
        )
    }
}

@Composable
private fun SettingsCardSurface(
    focused: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val scale = if (focused) 1.012f else 1f
                scaleX = scale
                scaleY = scale
            },
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) TvFocusBorder else MaterialTheme.colorScheme.surfaceBorder,
        ),
        shadowElevation = 0.dp,
        content = content,
    )
}

@Composable
private fun SettingsCardContent(
    title: String,
    body: String,
    caption: String?,
    icon: ImageVector,
    padding: Dp,
    trailingText: String? = null,
    showChevron: Boolean,
) {
    Row(
        modifier = Modifier.padding(padding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = SettingsHeadingSp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = SettingsBodySp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            caption?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    fontSize = SettingsCaptionSp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingText != null || showChevron) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                trailingText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = SettingsCaptionSp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showChevron) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

internal val SettingsHeadingSp = 16.sp
internal val SettingsBodySp = 13.sp
internal val SettingsCaptionSp = 11.sp

private fun FocusRequester?.requestFocusSafely(): Boolean {
    return this != null && runCatching { requestFocus() }.getOrDefault(false)
}

private fun (() -> Unit)?.invokeIfPresent(): Boolean {
    this?.invoke()
    return this != null
}
