package com.evomrdm.iptvbox

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import kotlinx.coroutines.delay

@Composable
internal fun SideNavigation(
    selected: AppScreen,
    selectedTab: CatalogTab,
    hasPlaylist: Boolean,
    stats: PlaylistStats?,
    expanded: Boolean,
    onDrawerEvent: (NavigationDrawerEvent) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenTab: (CatalogTab) -> Unit,
) {
    val entries = playlistNavEntries(hasPlaylist, stats)
    val focusManager = LocalFocusManager.current
    val menuFocusRequester = remember { FocusRequester() }
    val settingsIndex = entries.size
    val selectedIndex = selectedMenuIndex(
        entries = entries,
        selected = selected,
        selectedTab = selectedTab,
        settingsIndex = settingsIndex,
    )
    var focusedIndex by remember { mutableStateOf(selectedIndex) }

    LaunchedEffect(expanded, selected, selectedTab, entries.map { it.focusKey() }) {
        focusedIndex = selectedIndex
        if (expanded) {
            withFrameNanos { }
            delay(80L)
            runCatching { menuFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .width(if (expanded) 204.dp else 78.dp)
            .fillMaxHeight()
            .zIndex(2f)
            .animateContentSize()
            .focusRequester(menuFocusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                if (!expanded) {
                    return@onPreviewKeyEvent when (event.key) {
                        Key.DirectionLeft, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onDrawerEvent(NavigationDrawerEvent.OpenByUserNavigation)
                            true
                        }
                        else -> false
                    }
                }
                when (event.key) {
                    Key.DirectionUp -> {
                        focusedIndex = previousEnabledMenuIndex(focusedIndex, entries)
                        true
                    }
                    Key.DirectionDown -> {
                        focusedIndex = nextEnabledMenuIndex(focusedIndex, entries)
                        true
                    }
                    Key.DirectionRight -> {
                        onDrawerEvent(NavigationDrawerEvent.CollapseForContentFocus)
                        focusManager.moveFocus(FocusDirection.Right)
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        activateMenuItem(
                            focusedIndex = focusedIndex,
                            settingsIndex = settingsIndex,
                            entries = entries,
                            onDrawerEvent = onDrawerEvent,
                            onNavigate = onNavigate,
                            onOpenTab = onOpenTab,
                        )
                    }
                    else -> false
                }
            }
            .onFocusChanged { state ->
                if (state.hasFocus) {
                    focusedIndex = selectedIndex
                    if (!expanded) {
                        onDrawerEvent(NavigationDrawerEvent.DrawerFocused)
                    }
                }
            }
            .focusable()
            .focusGroup()
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
            entries.forEachIndexed { index, entry ->
                NavigationButton(
                    label = entry.label,
                    icon = entry.icon,
                    selected = entrySelected(entry, selected, selectedTab),
                    visualFocused = expanded && focusedIndex == index,
                    enabled = entry.enabled,
                    expanded = expanded,
                    interactive = expanded,
                    onFocused = { focusedIndex = index },
                    onClick = {
                        onDrawerEvent(NavigationDrawerEvent.CollapseForNavigation)
                        entry.tab?.let(onOpenTab)
                        entry.screen?.let(onNavigate)
                    },
                )
            }
        }

        NavigationButton(
            label = "Ayarlar",
            icon = Icons.Filled.Settings,
            selected = selected == AppScreen.SETTINGS,
            visualFocused = expanded && focusedIndex == settingsIndex,
            enabled = true,
            expanded = expanded,
            interactive = expanded,
            compact = true,
            onFocused = { focusedIndex = settingsIndex },
            onClick = {
                onDrawerEvent(NavigationDrawerEvent.CollapseForNavigation)
                onNavigate(AppScreen.SETTINGS)
            },
        )
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
            selected -> TvSelectedPanel
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
                selected -> IptvColors.Accent
                expanded -> TvRestingBorder
                else -> Color.Transparent
            },
        ),
        shadowElevation = tvFocusElevation(focused = showFocus, resting = 0.dp, focusedElevation = 10.dp),
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

internal fun playlistNavEntries(hasPlaylist: Boolean, stats: PlaylistStats?): List<NavEntry> {
    return listOf(
        NavEntry("Anasayfa", screen = AppScreen.HOME, enabled = hasPlaylist, icon = Icons.Filled.Home),
        NavEntry("Arama", screen = AppScreen.SEARCH, enabled = hasPlaylist, icon = Icons.Filled.Search),
        NavEntry(menuLabel("Canlı TV", stats?.live), tab = CatalogTab.LIVE, enabled = hasPlaylist, icon = Icons.Filled.LiveTv),
        NavEntry(menuLabel("Diziler", stats?.series), tab = CatalogTab.SERIES, enabled = hasPlaylist, icon = Icons.Filled.VideoLibrary),
        NavEntry(menuLabel("Filmler", stats?.movies), tab = CatalogTab.MOVIES, enabled = hasPlaylist, icon = Icons.Filled.Movie),
        NavEntry("Favoriler", screen = AppScreen.FAVORITES, enabled = hasPlaylist, icon = Icons.Filled.Favorite),
    )
}

internal fun bottomNavEntries(hasPlaylist: Boolean, stats: PlaylistStats?): List<NavEntry> {
    return listOf(
        NavEntry("Anasayfa", screen = AppScreen.HOME, enabled = hasPlaylist, icon = Icons.Filled.Home),
        NavEntry(menuLabel("Canlı TV", stats?.live), tab = CatalogTab.LIVE, enabled = hasPlaylist, icon = Icons.Filled.LiveTv),
        NavEntry(menuLabel("Filmler", stats?.movies), tab = CatalogTab.MOVIES, enabled = hasPlaylist, icon = Icons.Filled.Movie),
        NavEntry(menuLabel("Diziler", stats?.series), tab = CatalogTab.SERIES, enabled = hasPlaylist, icon = Icons.Filled.VideoLibrary),
        NavEntry("Ayarlar", screen = AppScreen.SETTINGS, enabled = true, icon = Icons.Filled.Settings),
    )
}

private fun NavEntry.focusKey(): String = tab?.name ?: screen?.name ?: label

internal fun entrySelected(entry: NavEntry, selected: AppScreen, selectedTab: CatalogTab): Boolean {
    return when {
        entry.tab != null -> selected == AppScreen.CATALOG && selectedTab == entry.tab
        entry.screen != null -> selected == entry.screen
        else -> false
    }
}

private fun selectedMenuIndex(
    entries: List<NavEntry>,
    selected: AppScreen,
    selectedTab: CatalogTab,
    settingsIndex: Int,
): Int {
    if (selected == AppScreen.SETTINGS) return settingsIndex
    val selectedEntry = entries.indexOfFirst { entrySelected(it, selected, selectedTab) }
    if (selectedEntry >= 0) return selectedEntry
    return entries.indexOfFirst { it.enabled }.takeIf { it >= 0 } ?: settingsIndex
}

private fun activateMenuItem(
    focusedIndex: Int,
    settingsIndex: Int,
    entries: List<NavEntry>,
    onDrawerEvent: (NavigationDrawerEvent) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenTab: (CatalogTab) -> Unit,
): Boolean {
    if (focusedIndex == settingsIndex) {
        onDrawerEvent(NavigationDrawerEvent.CollapseForNavigation)
        onNavigate(AppScreen.SETTINGS)
        return true
    }
    val entry = entries.getOrNull(focusedIndex) ?: return false
    if (!entry.enabled) return false
    onDrawerEvent(NavigationDrawerEvent.CollapseForNavigation)
    entry.tab?.let(onOpenTab)
    entry.screen?.let(onNavigate)
    return true
}

private fun nextEnabledMenuIndex(currentIndex: Int, entries: List<NavEntry>): Int {
    val indexes = enabledMenuIndexes(entries)
    val currentPosition = indexes.indexOf(currentIndex).takeIf { it >= 0 } ?: 0
    return indexes[(currentPosition + 1) % indexes.size]
}

private fun previousEnabledMenuIndex(currentIndex: Int, entries: List<NavEntry>): Int {
    val indexes = enabledMenuIndexes(entries)
    val currentPosition = indexes.indexOf(currentIndex).takeIf { it >= 0 } ?: 0
    return indexes[(currentPosition - 1 + indexes.size) % indexes.size]
}

private fun enabledMenuIndexes(entries: List<NavEntry>): List<Int> {
    val entryIndexes = entries.mapIndexedNotNull { index, entry -> index.takeIf { entry.enabled } }
    return entryIndexes + entries.size
}

private fun menuLabel(label: String, count: Int?): String {
    return if (count == null) label else "$label ($count)"
}

private fun screenLabel(screen: AppScreen, selectedTab: CatalogTab): String {
    return when (screen) {
        AppScreen.HOME -> "Anasayfa"
        AppScreen.PLAYLISTS -> "Liste seçimi"
        AppScreen.CATALOG -> selectedTab.label
        AppScreen.SEARCH -> "Arama"
        AppScreen.LATEST -> "Son Eklenenler"
        AppScreen.FAVORITES -> "Favoriler"
        AppScreen.RECENT -> "Son izlenen"
        AppScreen.SETTINGS -> "Ayarlar"
        AppScreen.PLAYER -> "Oynatıcı"
    }
}
