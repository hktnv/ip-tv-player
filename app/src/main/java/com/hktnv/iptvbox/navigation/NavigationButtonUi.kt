package com.hktnv.iptvbox.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
@Composable
internal fun NavigationButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    interactive: Boolean = true,
    visualFocused: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val showFocus = focused || visualFocused
    LaunchedEffect(interactive) {
        if (!interactive) focused = false
    }
    Surface(
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .focusProperties { canFocus = interactive }
            .fillMaxWidth()
            .height(if (compact) 38.dp else 44.dp)
            .zIndex(if (showFocus) 1f else 0f)
            .tvFocusLift(focused = showFocus, scale = 1.035f, liftPx = -4f)
            .onFocusChanged {
                focused = interactive && it.isFocused
                if (interactive && it.isFocused) onFocused?.invoke()
            }
            .then(if (interactive) Modifier.tvClickable(enabled = enabled, onClick = onClick) else Modifier),
        color = when {
            showFocus -> TvFocusPanel
            selected -> IptvColors.Accent.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        contentColor = when {
            selected -> IptvColors.Accent
            enabled -> IptvColors.TextPrimary
            else -> IptvColors.TextSecondary.copy(alpha = 0.46f)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (showFocus) 2.dp else 1.dp,
            when {
                showFocus -> TvFocusBorder
                expanded -> TvRestingBorder.copy(alpha = if (selected) 0.55f else 1f)
                else -> Color.Transparent
            },
        ),
        shadowElevation = tvFocusElevation(focused = showFocus, resting = 0.dp, focusedElevation = 10.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (selected) {
                NavigationActiveIndicator(
                    compact = compact,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (expanded) 12.dp else 0.dp),
                horizontalArrangement = if (expanded) Arrangement.spacedBy(9.dp) else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.height(17.dp))
                if (expanded) {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = if (compact) 11.sp else 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
