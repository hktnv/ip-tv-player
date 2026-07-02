package com.hktnv.iptvbox.navigation
import androidx.compose.material3.MaterialTheme
import android.os.SystemClock
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import kotlinx.coroutines.delay
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.PlaylistStats
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.stats

@Composable
internal fun SideNavigation(
    selected: AppScreen,
    selectedTab: CatalogTab,
    hasPlaylist: Boolean,
    stats: PlaylistStats?,
    expanded: Boolean,
    focusExpansion: NavigationDrawerFocusExpansion,
    onDrawerEvent: (NavigationDrawerEvent) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenTab: (CatalogTab) -> Unit,
    lastCollapsedMenuIntentAt: () -> Long,
    onRequestExitConfirmation: () -> Unit,
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
                    Key.Back -> {
                        onRequestExitConfirmation()
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
                        val focusFollowsUserLeftIntent = shouldExpandCollapsedDrawerOnFocus(
                            nowMs = SystemClock.uptimeMillis(),
                            lastUserLeftIntentMs = lastCollapsedMenuIntentAt(),
                            focusExpansion = focusExpansion,
                        )
                        onDrawerEvent(
                            if (focusFollowsUserLeftIntent) {
                                NavigationDrawerEvent.OpenByUserNavigation
                            } else {
                                NavigationDrawerEvent.DrawerFocused
                            },
                        )
                    }
                }
            }
            .focusable(enabled = drawerContainerCanFocus(expanded))
            .focusGroup()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(18.dp))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (expanded) "IP TV" else "IP",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (expanded) 18.sp else 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = if (expanded) TextAlign.Start else TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (expanded) {
                Text(
                    text = screenLabel(selected, selectedTab),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun NavEntry.focusKey(): String = tab?.name ?: screen?.name ?: label

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
