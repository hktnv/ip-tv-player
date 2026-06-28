package com.hktnv.iptvbox.ui.host
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.core.model.CatalogItem
import kotlinx.coroutines.delay
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.AppPerformanceMode
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.LocalPerformanceMode
import com.hktnv.iptvbox.navigation.NavigationDrawerEvent
import com.hktnv.iptvbox.navigation.PlaylistContentScaffold
import com.hktnv.iptvbox.player.PlayerScreen
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.telemetry.PerformanceDiagnostics
import com.hktnv.iptvbox.ui.common.BootScreen
import com.hktnv.iptvbox.ui.common.RecoveryScreen
import com.hktnv.iptvbox.ui.playlist.PlaylistEntryScreen

@Composable
internal fun IptvContentHost(
    performanceMode: AppPerformanceMode,
    restoredApplied: Boolean,
    showRecovery: Boolean,
    bootError: String?,
    selectedPlaylist: LoadedPlaylist?,
    screen: AppScreen,
    showPlaylistEntry: Boolean,
    currentItem: CatalogItem?,
    currentHeaders: Map<String, String>,
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
    contentFocusRequest: Int,
    contentInitialFocusRequester: FocusRequester,
    catalogRepository: AppCatalogRepository,
    telemetry: AppPerformanceTelemetry,
    searchDraft: String,
    submittedSearch: String,
    onPlayerBack: () -> Unit,
    onSeriesBack: () -> Unit,
    onRecoveryContinue: () -> Unit,
    onRecoveryReload: () -> Unit,
    onRecoveryRemove: () -> Unit,
    onOpenLastPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onOpenSettingsFromEntry: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenCatalogTab: (CatalogTab) -> Unit,
    onOpenLatest: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenSeries: (String) -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
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
    onRequestExitConfirmation: () -> Unit,
    onDismissBanner: () -> Unit,
) {
    CompositionLocalProvider(LocalPerformanceMode provides performanceMode) {
        BackHandler(screen == AppScreen.PLAYER) {
            onPlayerBack()
        }
        BackHandler(
            screen == AppScreen.CATALOG &&
                selectedTab == CatalogTab.SERIES &&
                (selectedSeasonNumber != null || selectedSeriesTitle != null),
        ) {
            onSeriesBack()
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(IptvColors.Night),
        ) {
            val wide = maxWidth >= 900.dp
            val contentPadding = if (wide) 30.dp else 18.dp
            val focusManager = LocalFocusManager.current
            BackHandler(
                enabled = wide &&
                    !showPlaylistEntry &&
                    screen != AppScreen.PLAYER &&
                    !(screen == AppScreen.CATALOG &&
                        selectedTab == CatalogTab.SERIES &&
                        (selectedSeasonNumber != null || selectedSeriesTitle != null)),
            ) {
                if (sideMenuExpanded) {
                    onRequestExitConfirmation()
                } else {
                    onDrawerEvent(NavigationDrawerEvent.OpenByUserNavigation)
                }
            }

            LaunchedEffect(contentFocusRequest) {
                if (contentFocusRequest > 0 && wide && !showPlaylistEntry && screen != AppScreen.PLAYER) {
                    withFrameNanos { }
                    onDrawerEvent(NavigationDrawerEvent.CollapseForContentFocus)
                    focusManager.clearFocus(force = true)
                    delay(120L)
                    runCatching { contentInitialFocusRequester.requestFocus() }
                    delay(40L)
                    onDrawerEvent(NavigationDrawerEvent.ContentFocusRestored)
                }
            }

            when {
                !restoredApplied -> BootScreen(contentPadding = contentPadding)
                showRecovery -> RecoveryScreen(
                    message = bootError ?: "Önceki liste güvenli şekilde açılamadı.",
                    hasPlaylist = selectedPlaylist != null,
                    contentPadding = contentPadding,
                    onContinue = onRecoveryContinue,
                    onReload = onRecoveryReload,
                    onRemove = onRecoveryRemove,
                )
                screen == AppScreen.PLAYER && currentItem != null -> PlayerScreen(
                    item = currentItem,
                    headers = currentHeaders,
                    onBack = onPlayerBack,
                )
                showPlaylistEntry -> PlaylistEntryScreen(
                    playlists = playlists,
                    selectedPlaylistId = selectedPlaylist?.id,
                    contentPadding = contentPadding,
                    onOpenLastPlaylist = onOpenLastPlaylist,
                    onAddPlaylist = onAddPlaylist,
                    onSelectPlaylist = onSelectPlaylist,
                    onOpenSettings = onOpenSettingsFromEntry,
                )
                else -> PlaylistContentScaffold(
                    wide = wide,
                    contentPadding = contentPadding,
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
                    banner = banner,
                    sideMenuExpanded = sideMenuExpanded,
                    contentInitialFocusRequester = contentInitialFocusRequester,
                    catalogRepository = catalogRepository,
                    telemetry = telemetry,
                    searchDraft = searchDraft,
                    submittedSearch = submittedSearch,
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
                        onNavigate = onNavigate,
                        onDrawerEvent = onDrawerEvent,
                        onRequestExitConfirmation = onRequestExitConfirmation,
                        onDismissBanner = onDismissBanner,
                    )
            }
        }
    }
}
