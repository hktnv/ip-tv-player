package com.hktnv.iptvbox.ui.playlist
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.designsystem.focusBorder
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.TvSelectedPanel
import com.hktnv.iptvbox.ui.media.label

@Composable
internal fun EmptyPrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, tween(150), label = "emptyActionScale")
    val elevation by animateDpAsState(if (focused) 18.dp else 6.dp, tween(150), label = "emptyActionElevation")
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
    Surface(
        modifier = modifier
            .height(62.dp)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (focused) -5f else 0f
            }
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(18.dp),
        border = if (focused) BorderStroke(2.dp, MaterialTheme.colorScheme.focusBorder) else null,
        shadowElevation = elevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun EntryActionCard(
    action: EntryAction,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "entryCardScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (focused) 22.dp else if (action.emphasis == EntryEmphasis.Primary) 8.dp else 2.dp,
        animationSpec = tween(durationMillis = 150),
        label = "entryCardElevation",
    )
    val borderColor = when {
        focused -> TvFocusBorder
        action.selected -> MaterialTheme.colorScheme.onPrimaryContainer
        action.emphasis == EntryEmphasis.Primary -> MaterialTheme.colorScheme.surfaceVariant
        else -> TvRestingBorder
    }
    Surface(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (focused) -8f else 0f
            }
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = action.onClick),
        color = when {
            focused -> TvFocusPanel
            action.selected -> TvSelectedPanel
            action.emphasis == EntryEmphasis.Primary -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(if (action.emphasis == EntryEmphasis.Primary) 22.dp else 16.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
        shadowElevation = elevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .defaultMinSize(minHeight = 104.dp)
                .padding(if (action.emphasis == EntryEmphasis.Primary) 22.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.height(if (action.emphasis == EntryEmphasis.Primary) 30.dp else 22.dp),
                tint = when {
                    focused -> TvFocusBorder
                    action.selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = action.title,
                    fontSize = if (action.emphasis == EntryEmphasis.Primary) 26.sp else 18.sp,
                    lineHeight = if (action.emphasis == EntryEmphasis.Primary) 31.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (action.emphasis == EntryEmphasis.Primary) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = action.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.94f),
                    fontSize = if (action.emphasis == EntryEmphasis.Primary) 15.sp else 13.sp,
                    lineHeight = if (action.emphasis == EntryEmphasis.Primary) 20.sp else 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal data class EntryAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val selected: Boolean,
    val emphasis: EntryEmphasis,
    val onClick: () -> Unit,
)

internal enum class EntryEmphasis {
    Primary,
    Secondary,
    Tertiary,
}
