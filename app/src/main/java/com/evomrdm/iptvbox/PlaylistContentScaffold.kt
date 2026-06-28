package com.evomrdm.iptvbox

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.evomrdm.iptvbox.core.model.CatalogItem

@Composable
internal fun PlaylistContentScaffold(
    wide: Boolean,
    contentPadding: Dp,
    screen: AppScreen,
    selectedPlaylist: LoadedPlaylist?,
    playlists: List<LoadedPlaylist>,
    catalogSnapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    favoriteIds: List<String>,
    recentIds: List<String>,
    favoriteItems: List<CatalogItem>,
    recentItems: List<CatalogItem>,
    diagnostics: PerformanceDiagnostics,
    banner: String?,
    sideMenuExpanded: Boolean,
    contentInitialFocusRequester: FocusRequester,
    catalogRepository: AppCatalogRepository,
    telemetry: AppPerformanceTelemetry,
    searchDraft: String,
    submittedSearch: String,
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
    onTabSelected: (CatalogTab) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenPlaylistEntry: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onDrawerEvent: (NavigationDrawerEvent) -> Unit,
    onDismissBanner: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(
                start = if (wide) 36.dp else 0.dp,
                end = if (wide) 36.dp else 0.dp,
                top = if (wide) 22.dp else 0.dp,
                bottom = if (wide) 22.dp else 0.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (wide) 96.dp else 0.dp)
                .fillMaxHeight()
                .focusGroup()
                .statusBarsPadding(),
        ) {
            Box(Modifier.weight(1f)) {
                PlaylistScreenContent(
                    screen = screen,
                    selectedPlaylist = selectedPlaylist,
                    playlists = playlists,
                    catalogSnapshot = catalogSnapshot,
                    catalogIndexLoading = catalogIndexLoading,
                    selectedTab = selectedTab,
                    selectedCategory = selectedCategory,
                    selectedSeriesTitle = selectedSeriesTitle,
                    selectedSeasonNumber = selectedSeasonNumber,
                    favoriteIds = favoriteIds,
                    recentIds = recentIds,
                    favoriteItems = favoriteItems,
                    recentItems = recentItems,
                    diagnostics = diagnostics,
                    contentInitialFocusRequester = contentInitialFocusRequester,
                    catalogRepository = catalogRepository,
                    telemetry = telemetry,
                    searchDraft = searchDraft,
                    submittedSearch = submittedSearch,
                    contentPadding = contentPadding,
                    onAddPlaylist = onAddPlaylist,
                    onOpenCatalog = onOpenCatalog,
                    onOpenCatalogTab = onOpenCatalogTab,
                    onOpenLatest = onOpenLatest,
                    onOpenFavorites = onOpenFavorites,
                    onOpenRecent = onOpenRecent,
                    onOpenSeries = onOpenSeries,
                    onOpenItem = onOpenItem,
                    onSelectPlaylist = onSelectPlaylist,
                    onReloadPlaylist = onReloadPlaylist,
                    onRenamePlaylist = onRenamePlaylist,
                    onTabSelected = onTabSelected,
                    onCategorySelected = onCategorySelected,
                    onSeriesSelected = onSeriesSelected,
                    onSeasonSelected = onSeasonSelected,
                    onToggleFavorite = onToggleFavorite,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onOpenPlaylistEntry = onOpenPlaylistEntry,
                    onRequestSideMenu = { onDrawerEvent(NavigationDrawerEvent.OpenByUserNavigation) },
                )
            }
            if (!wide) {
                BottomNavigation(
                    selected = screen,
                    selectedTab = selectedTab,
                    hasPlaylist = selectedPlaylist != null,
                    stats = selectedPlaylist?.stats(),
                    onNavigate = onNavigate,
                    onOpenTab = onOpenCatalogTab,
                )
            }
        }
        if (wide) {
            SideNavigation(
                selected = screen,
                selectedTab = selectedTab,
                hasPlaylist = selectedPlaylist != null,
                stats = selectedPlaylist?.stats(),
                expanded = sideMenuExpanded,
                onDrawerEvent = onDrawerEvent,
                onNavigate = onNavigate,
                onOpenTab = onOpenCatalogTab,
            )
        }
        banner?.let {
            FloatingStatusToast(
                text = it,
                onDismiss = onDismissBanner,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = if (wide) 28.dp else 14.dp, start = 14.dp),
            )
        }
    }
}

@Composable
private fun PlaylistScreenContent(
    screen: AppScreen,
    selectedPlaylist: LoadedPlaylist?,
    playlists: List<LoadedPlaylist>,
    catalogSnapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    favoriteIds: List<String>,
    recentIds: List<String>,
    favoriteItems: List<CatalogItem>,
    recentItems: List<CatalogItem>,
    diagnostics: PerformanceDiagnostics,
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
    onTabSelected: (CatalogTab) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenPlaylistEntry: () -> Unit,
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
            onAddPlaylist = onAddPlaylist,
            onOpenCatalog = onOpenCatalog,
            onOpenCatalogTab = onOpenCatalogTab,
            onOpenLatest = onOpenLatest,
            onOpenFavorites = onOpenFavorites,
            onOpenRecent = onOpenRecent,
            onOpenSeries = onOpenSeries,
            onOpenItem = onOpenItem,
            onSelectPlaylist = onSelectPlaylist,
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
            contentPadding = contentPadding,
        )
        AppScreen.CATALOG -> CatalogScreen(
            playlist = selectedPlaylist,
            snapshot = catalogSnapshot,
            catalogIndexLoading = catalogIndexLoading,
            selectedTab = selectedTab,
            selectedCategory = selectedCategory,
            selectedSeriesTitle = selectedSeriesTitle,
            selectedSeasonNumber = selectedSeasonNumber,
            favoriteIds = favoriteIds,
            onTabSelected = onTabSelected,
            onCategorySelected = onCategorySelected,
            onSeriesSelected = onSeriesSelected,
            onSeasonSelected = onSeasonSelected,
            onOpenItem = onOpenItem,
            onToggleFavorite = onToggleFavorite,
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
            favoriteIds = favoriteIds,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onOpenItem = onOpenItem,
            onToggleFavorite = onToggleFavorite,
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
            onToggleFavorite = onToggleFavorite,
            onAddPlaylist = onAddPlaylist,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
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
            onToggleFavorite = onToggleFavorite,
            onAddPlaylist = onAddPlaylist,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
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
            onToggleFavorite = onToggleFavorite,
            onAddPlaylist = onAddPlaylist,
            contentPadding = contentPadding,
            initialFocusRequester = contentInitialFocusRequester,
        )
        AppScreen.SETTINGS -> SettingsScreen(
            playlist = selectedPlaylist,
            diagnostics = diagnostics,
            onReload = { selectedPlaylist?.let(onReloadPlaylist) },
            onAddPlaylist = onAddPlaylist,
            onOpenPlaylistEntry = onOpenPlaylistEntry,
            contentPadding = contentPadding,
        )
        AppScreen.PLAYER -> Unit
    }
}
