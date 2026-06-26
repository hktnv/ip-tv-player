package com.evomrdm.iptvbox

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evomrdm.iptvbox.core.designsystem.IptvColors

@Composable
internal fun SideNavigation(
    selected: AppScreen,
    selectedTab: CatalogTab,
    hasPlaylist: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenTab: (CatalogTab) -> Unit,
    onOpenPlaylistEntry: () -> Unit,
) {
    val entries = playlistNavEntries(hasPlaylist)
    Column(
        modifier = Modifier
            .width(if (expanded) 204.dp else 78.dp)
            .fillMaxHeight()
            .animateContentSize()
            .focusGroup()
            .onFocusChanged { state ->
                if (state.hasFocus) onExpandedChange(true)
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onExpandedChange(false)
                    false
                } else {
                    false
                }
            }
            .background(Color(0xFF0A0F16), RoundedCornerShape(18.dp))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (expanded) "IP TV" else "IP",
                color = IptvColors.TextPrimary,
                fontSize = if (expanded) 18.sp else 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = if (expanded) TextAlign.Start else TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (expanded) {
                Text(
                    text = screenLabel(selected, selectedTab),
                    color = IptvColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            entries.forEach { entry ->
                val isSelected = when {
                    entry.tab != null -> selected == AppScreen.CATALOG && selectedTab == entry.tab
                    entry.screen != null -> selected == entry.screen
                    else -> false
                }
                NavigationButton(
                    label = entry.label,
                    icon = entry.icon,
                    selected = isSelected,
                    enabled = entry.enabled,
                    expanded = expanded,
                    onClick = {
                        entry.tab?.let(onOpenTab)
                        entry.screen?.let(onNavigate)
                    },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NavigationButton(
                label = "Liste seçimi",
                icon = Icons.AutoMirrored.Filled.List,
                selected = selected == AppScreen.PLAYLISTS,
                enabled = true,
                expanded = expanded,
                compact = true,
                onClick = onOpenPlaylistEntry,
            )
            NavigationButton(
                label = "Ayarlar",
                icon = Icons.Filled.Settings,
                selected = selected == AppScreen.SETTINGS,
                enabled = true,
                expanded = expanded,
                compact = true,
                onClick = { onNavigate(AppScreen.SETTINGS) },
            )
        }
    }
}

@Composable
internal fun BottomNavigation(
    selected: AppScreen,
    selectedTab: CatalogTab,
    hasPlaylist: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onOpenTab: (CatalogTab) -> Unit,
) {
    val entries = playlistNavEntries(hasPlaylist)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xF20A0F16),
        tonalElevation = 6.dp,
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(entries) { entry ->
                val isSelected = when {
                    entry.tab != null -> selected == AppScreen.CATALOG && selectedTab == entry.tab
                    entry.screen != null -> selected == entry.screen
                    else -> false
                }
                BottomNavItem(
                    label = entry.label,
                    icon = entry.icon,
                    selected = isSelected,
                    enabled = entry.enabled,
                    onClick = {
                        entry.tab?.let(onOpenTab)
                        entry.screen?.let(onNavigate)
                    },
                    modifier = Modifier.width(92.dp),
                )
            }
        }
    }
}

@Composable
internal fun BottomNavItem(
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
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onClick),
        color = when {
            focused -> Color(0xFF182636)
            selected -> Color(0xFF10221F)
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
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.height(17.dp))
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (!enabled) IptvColors.TextSecondary.copy(alpha = 0.36f) else Color.Unspecified,
            )
        }
    }
}

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
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 38.dp else 44.dp)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onClick),
        color = when {
            focused -> Color(0xFF1E3345)
            selected -> Color(0xFF122721)
            else -> Color.Transparent
        },
        contentColor = when {
            selected -> IptvColors.Accent
            enabled -> IptvColors.TextPrimary
            else -> IptvColors.TextSecondary.copy(alpha = 0.46f)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> TvFocusBorder
                selected -> IptvColors.Accent
                expanded -> Color(0xFF263241)
                else -> Color.Transparent
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (expanded) 10.dp else 0.dp),
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

internal data class NavEntry(
    val label: String,
    val screen: AppScreen? = null,
    val tab: CatalogTab? = null,
    val enabled: Boolean,
    val icon: ImageVector,
)

internal fun playlistNavEntries(hasPlaylist: Boolean): List<NavEntry> {
    return listOf(
        NavEntry("Anasayfa", screen = AppScreen.HOME, enabled = hasPlaylist, icon = Icons.Filled.Home),
        NavEntry("Arama", screen = AppScreen.SEARCH, enabled = hasPlaylist, icon = Icons.Filled.Search),
        NavEntry("Canlı TV", tab = CatalogTab.LIVE, enabled = hasPlaylist, icon = Icons.AutoMirrored.Filled.List),
        NavEntry("Diziler", tab = CatalogTab.SERIES, enabled = hasPlaylist, icon = Icons.AutoMirrored.Filled.List),
        NavEntry("Filmler", tab = CatalogTab.MOVIES, enabled = hasPlaylist, icon = Icons.AutoMirrored.Filled.List),
        NavEntry("Favoriler", screen = AppScreen.FAVORITES, enabled = hasPlaylist, icon = Icons.Filled.Home),
        NavEntry("Son İzlenen", screen = AppScreen.RECENT, enabled = hasPlaylist, icon = Icons.AutoMirrored.Filled.List),
    )
}

private fun screenLabel(screen: AppScreen, selectedTab: CatalogTab): String {
    return when (screen) {
        AppScreen.HOME -> "Anasayfa"
        AppScreen.PLAYLISTS -> "Liste seçimi"
        AppScreen.CATALOG -> selectedTab.label
        AppScreen.SEARCH -> "Arama"
        AppScreen.FAVORITES -> "Favoriler"
        AppScreen.RECENT -> "Son izlenen"
        AppScreen.SETTINGS -> "Ayarlar"
        AppScreen.PLAYER -> "Oynatıcı"
    }
}

private val TvFocusBorder = Color(0xFFB9D8FF)
