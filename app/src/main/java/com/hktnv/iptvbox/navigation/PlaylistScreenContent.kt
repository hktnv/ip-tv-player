package com.hktnv.iptvbox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.player.PlayerUiMode
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.telemetry.PerformanceDiagnostics
import com.hktnv.iptvbox.ui.catalog.CatalogScreen
import com.hktnv.iptvbox.ui.home.HomeScreen
import com.hktnv.iptvbox.ui.home.PlaylistScreen
import com.hktnv.iptvbox.ui.media.stats
import com.hktnv.iptvbox.ui.search.SearchScreen
import com.hktnv.iptvbox.ui.settings.LatestItemsScreen
import com.hktnv.iptvbox.ui.settings.SavedItemsScreen
import com.hktnv.iptvbox.ui.settings.SettingsScreen
@Composable
internal fun PlaylistScreenContent(
    screen: AppScreen,
    selectedPlaylist: LoadedPlaylist?,
    playlists: List<LoadedPlaylist>,
    catalogSnapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    showCatalogCategoryLanding: Boolean,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    favoriteIds: List<String>,
    recentIds: List<String>,
    favoriteItems: List<CatalogItem>,
    recentItems: List<CatalogItem>,
    diagnostics: PerformanceDiagnostics,
    playerUiMode: PlayerUiMode,
    contentInitialFocusRequester: FocusRequester,
    catalogRepository: AppCatalogRepository,
    telemetry: AppPerformanceTelemetry,
    searchDraft: String,
    submittedSearch: String,
    contentPadding: Dp,
    onAddPlaylist: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenCatalogTab: (CatalogTab) -> Unit,
    onOpenLatest: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenSeries: (String) -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onReloadPlaylist: (LoadedPlaylist) -> Unit,
    onRenamePlaylist: (LoadedPlaylist) -> Unit,
    onDeletePlaylist: (LoadedPlaylist) -> Unit,
    onTabSelected: (CatalogTab) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenPlaylistEntry: () -> Unit,
    onPlayerUiModeChange: (PlayerUiMode) -> Unit,
    onRequestSideMenu: () -> Unit,
) {
    when (screen) {
        AppScreen.HOME -> HomeScreen(
            playlist = selectedPlaylist,
            snapshot = catalogSnapshot,
            catalogIndexLoading = catalogIndexLoading,
            playlists = playlists,
            favoriteCount = favoriteIds.size,
            recentCount = recentIds.size,
            favoriteItems = favoriteItems,
            recentItems = recentItems,
            favoriteIds = favoriteIds,
            onAddPlaylist = onAddPlaylist,
            onOpenCatalog = onOpenCatalog,
            onOpenCatalogTab = onOpenCatalogTab,
            onOpenLatest = onOpenLatest,
            onOpenFavorites = onOpenFavorites,
            onOpenRecent = onOpenRecent,
            onOpenSeries = onOpenSeries,
            onOpenItem = onOpenItem,
            onShowItemOptions = onShowItemOptions,
            onSelectPlaylist = onSelectPlaylist,
            onRequestSideMenu = onRequestSideMenu,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
        )
        AppScreen.PLAYLISTS -> PlaylistScreen(
            playlists = playlists,
            selectedPlaylistId = selectedPlaylist?.id,
            onAddPlaylist = onAddPlaylist,
            onSelectPlaylist = onSelectPlaylist,
            onReload = onReloadPlaylist,
            onRename = onRenamePlaylist,
            onDelete = onDeletePlaylist,
            contentPadding = contentPadding,
        )
        AppScreen.CATALOG -> CatalogScreen(
            playlist = selectedPlaylist,
            snapshot = catalogSnapshot,
            catalogIndexLoading = catalogIndexLoading,
            selectedTab = selectedTab,
            selectedCategory = selectedCategory,
            showCategoryLanding = showCatalogCategoryLanding,
            selectedSeriesTitle = selectedSeriesTitle,
            selectedSeasonNumber = selectedSeasonNumber,
            favoriteIds = favoriteIds,
            onTabSelected = onTabSelected,
            onCategorySelected = onCategorySelected,
            onSeriesSelected = onSeriesSelected,
            onSeasonSelected = onSeasonSelected,
            onOpenItem = onOpenItem,
            onShowItemOptions = onShowItemOptions,
            onAddPlaylist = onAddPlaylist,
            onRequestSideMenu = onRequestSideMenu,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
        )
        AppScreen.SEARCH -> SearchScreen(
            playlist = selectedPlaylist,
            snapshot = catalogSnapshot,
            catalogIndexLoading = catalogIndexLoading,
            catalogRepository = catalogRepository,
            telemetry = telemetry,
            query = searchDraft,
            submittedQuery = submittedSearch,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onOpenItem = onOpenItem,
            onOpenSeries = onOpenSeries,
            favoriteIds = favoriteIds,
            onShowItemOptions = onShowItemOptions,
            onAddPlaylist = onAddPlaylist,
            onRequestSideMenu = onRequestSideMenu,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
        )
        AppScreen.LATEST -> LatestItemsScreen(
            playlist = selectedPlaylist,
            snapshot = catalogSnapshot,
            catalogIndexLoading = catalogIndexLoading,
            favoriteIds = favoriteIds,
            onOpenItem = onOpenItem,
            onShowItemOptions = onShowItemOptions,
            onAddPlaylist = onAddPlaylist,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        AppScreen.FAVORITES -> SavedItemsScreen(
            title = "Favoriler",
            emptyText = "Favori içerik yok",
            playlist = selectedPlaylist,
            snapshot = catalogSnapshot,
            catalogIndexLoading = catalogIndexLoading,
            itemIds = favoriteIds,
            favoriteIds = favoriteIds,
            onOpenItem = onOpenItem,
            onShowItemOptions = onShowItemOptions,
            onAddPlaylist = onAddPlaylist,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        AppScreen.RECENT -> SavedItemsScreen(
            title = "Son izlenenler",
            emptyText = "Henüz izlenen içerik yok",
            playlist = selectedPlaylist,
            snapshot = catalogSnapshot,
            catalogIndexLoading = catalogIndexLoading,
            itemIds = recentIds,
            favoriteIds = favoriteIds,
            onOpenItem = onOpenItem,
            onShowItemOptions = onShowItemOptions,
            onAddPlaylist = onAddPlaylist,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        AppScreen.SETTINGS -> SettingsScreen(
            playlist = selectedPlaylist,
            diagnostics = diagnostics,
            playerUiMode = playerUiMode,
            onPlayerUiModeChange = onPlayerUiModeChange,
            onReload = { selectedPlaylist?.let(onReloadPlaylist) },
            onAddPlaylist = onAddPlaylist,
            onOpenPlaylistEntry = onOpenPlaylistEntry,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        )
        AppScreen.PLAYER -> Unit
    }
}
