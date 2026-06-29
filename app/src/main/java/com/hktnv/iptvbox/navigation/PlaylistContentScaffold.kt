package com.hktnv.iptvbox.navigation
import android.os.SystemClock
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.telemetry.PerformanceDiagnostics
import com.hktnv.iptvbox.ui.catalog.CatalogScreen
import com.hktnv.iptvbox.ui.common.FloatingStatusToast
import com.hktnv.iptvbox.ui.home.HomeScreen
import com.hktnv.iptvbox.ui.home.PlaylistScreen
import com.hktnv.iptvbox.ui.media.stats
import com.hktnv.iptvbox.ui.search.SearchScreen
import com.hktnv.iptvbox.ui.settings.LatestItemsScreen
import com.hktnv.iptvbox.ui.settings.SavedItemsScreen
import com.hktnv.iptvbox.ui.settings.SettingsScreen

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
    showCatalogCategoryLanding: Boolean,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    favoriteIds: List<String>,
    recentIds: List<String>,
    favoriteItems: List<CatalogItem>,
    recentItems: List<CatalogItem>,
    diagnostics: PerformanceDiagnostics,
    banner: String?,
    sideMenuExpanded: Boolean,
    drawerFocusExpansion: NavigationDrawerFocusExpansion,
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
    onShowItemOptions: (CatalogItem) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenPlaylistEntry: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onDrawerEvent: (NavigationDrawerEvent) -> Unit,
    onRequestExitConfirmation: () -> Unit,
    onDismissBanner: () -> Unit,
) {
    var lastCollapsedMenuIntentAt by remember { mutableLongStateOf(0L) }
    fun handleDrawerEvent(event: NavigationDrawerEvent) {
        lastCollapsedMenuIntentAt = consumeUserLeftIntentAfterDrawerEvent(
            lastUserLeftIntentMs = lastCollapsedMenuIntentAt,
            event = event,
        )
        onDrawerEvent(event)
    }
    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (wide && !sideMenuExpanded &&
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft
                ) {
                    lastCollapsedMenuIntentAt = SystemClock.uptimeMillis()
                }
                false
            }
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
                    showCatalogCategoryLanding = showCatalogCategoryLanding,
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
                    onShowItemOptions = onShowItemOptions,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onOpenPlaylistEntry = onOpenPlaylistEntry,
                    onRequestSideMenu = { handleDrawerEvent(NavigationDrawerEvent.OpenByUserNavigation) },
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
                focusExpansion = drawerFocusExpansion,
                onDrawerEvent = ::handleDrawerEvent,
                onNavigate = onNavigate,
                onOpenTab = onOpenCatalogTab,
                lastCollapsedMenuIntentAt = lastCollapsedMenuIntentAt,
                onRequestExitConfirmation = onRequestExitConfirmation,
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
