package com.hktnv.iptvbox.navigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.PlaylistStats
import com.hktnv.iptvbox.ui.media.count
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.stats

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
        NavEntry("Arama", screen = AppScreen.SEARCH, enabled = hasPlaylist, icon = Icons.Filled.Search),
        NavEntry(menuLabel("Canlı TV", stats?.live), tab = CatalogTab.LIVE, enabled = hasPlaylist, icon = Icons.Filled.LiveTv),
        NavEntry(menuLabel("Filmler", stats?.movies), tab = CatalogTab.MOVIES, enabled = hasPlaylist, icon = Icons.Filled.Movie),
        NavEntry(menuLabel("Diziler", stats?.series), tab = CatalogTab.SERIES, enabled = hasPlaylist, icon = Icons.Filled.VideoLibrary),
        NavEntry("Ayarlar", screen = AppScreen.SETTINGS, enabled = true, icon = Icons.Filled.Settings),
    )
}

internal fun entrySelected(entry: NavEntry, selected: AppScreen, selectedTab: CatalogTab): Boolean {
    return when {
        entry.tab != null -> selected == AppScreen.CATALOG && selectedTab == entry.tab
        entry.screen != null -> selected == entry.screen
        else -> false
    }
}

internal fun screenLabel(screen: AppScreen, selectedTab: CatalogTab): String {
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

private fun menuLabel(label: String, count: Int?): String {
    return if (count == null) label else "$label ($count)"
}
