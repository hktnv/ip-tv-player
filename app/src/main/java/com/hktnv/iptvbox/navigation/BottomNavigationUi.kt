package com.hktnv.iptvbox.navigation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.PlaylistStats
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvSelectedPanel

@Composable
internal fun BottomNavigation(
    selected: AppScreen,
    selectedTab: CatalogTab,
    hasPlaylist: Boolean,
    stats: PlaylistStats?,
    onNavigate: (AppScreen) -> Unit,
    onOpenTab: (CatalogTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xF20A0F16),
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(54.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomNavEntries(hasPlaylist, stats).forEach { entry ->
                BottomNavItem(
                    label = entry.label,
                    icon = entry.icon,
                    selected = entrySelected(entry, selected, selectedTab),
                    enabled = entry.enabled,
                    onClick = {
                        entry.tab?.let(onOpenTab)
                        entry.screen?.let(onNavigate)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.03f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> TvSelectedPanel
            else -> Color.Transparent
        },
        contentColor = if (selected) IptvColors.Accent else IptvColors.TextPrimary,
        shape = RoundedCornerShape(14.dp),
        border = if (focused || selected) {
            BorderStroke(1.dp, if (focused) TvFocusBorder else IptvColors.Accent)
        } else {
            null
        },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (!enabled) IptvColors.TextSecondary.copy(alpha = 0.36f) else Color.Unspecified,
            )
        }
    }
}
