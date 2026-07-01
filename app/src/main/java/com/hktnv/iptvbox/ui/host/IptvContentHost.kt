package com.hktnv.iptvbox.ui.host
import androidx.compose.material3.MaterialTheme
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
import com.hktnv.iptvbox.core.model.CatalogItem
import kotlinx.coroutines.delay
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.AppPerformanceMode
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.LocalPerformanceMode
import com.hktnv.iptvbox.model.PlaylistImportProgress
import com.hktnv.iptvbox.navigation.NavigationDrawerEvent
import com.hktnv.iptvbox.navigation.NavigationDrawerFocusExpansion
import com.hktnv.iptvbox.navigation.PlaylistContentScaffold
import com.hktnv.iptvbox.navigation.shouldHandleSeriesBack
import com.hktnv.iptvbox.navigation.shouldReturnToCatalogCategories
import com.hktnv.iptvbox.player.PlayerScreen
import com.hktnv.iptvbox.player.PlayerUiMode
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
    playerContextItems: List<CatalogItem>,
    playlists: List<LoadedPlaylist>,
    catalogSnapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    playerUiMode: PlayerUiMode,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    showCatalogCategoryLanding: Boolean,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    favoriteIds: List<String>,
    recentIds: List<String>,
    favoriteItems: List<CatalogItem>,
    recentItems: List<CatalogItem>,
    playlistDetailId: String?,
    diagnostics: PerformanceDiagnostics,
    banner: String?,
    playlistImportProgress: PlaylistImportProgress?,
    catalogSyncStatuses: Map<String, CatalogSyncStatus>,
    sideMenuExpanded: Boolean,
    drawerFocusExpansion: NavigationDrawerFocusExpansion,
    contentFocusRequest: Int,
    contentInitialFocusRequester: FocusRequester,
    catalogRepository: AppCatalogRepository,
    telemetry: AppPerformanceTelemetry,
    searchDraft: String,
    submittedSearch: String,
    onPlayerBack: () -> Unit,
    onSeriesBack: () -> Unit,
    onShowCatalogCategories: () -> Unit,
    onRecoveryContinue: () -> Unit,
    onRecoveryReload: () -> Unit,
    onRecoveryRemove: () -> Unit,
    onOpenLastPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onOpenPlaylistDetails: (String) -> Unit,
    onClosePlaylistDetails: () -> Unit,
    onUsePlaylist: (String) -> Unit,
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
    onDeletePlaylist: (LoadedPlaylist) -> Unit,
    onAutoUpdateHoursChange: (LoadedPlaylist, Int) -> Unit,
    onTabSelected: (CatalogTab) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectPlayerItem: (CatalogItem) -> Unit,
    onOpenPlaylistEntry: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onDrawerEvent: (NavigationDrawerEvent) -> Unit,
    onPlayerUiModeChange: (PlayerUiMode) -> Unit,
    onRequestExitConfirmation: () -> Unit,
    onDismissBanner: () -> Unit,
) {
    CompositionLocalProvider(LocalPerformanceMode provides performanceMode) {
        BackHandler(
            shouldHandleSeriesBack(
                screen = screen,
                selectedTab = selectedTab,
                showCategoryLanding = showCatalogCategoryLanding,
                selectedSeriesTitle = selectedSeriesTitle,
                selectedSeasonNumber = selectedSeasonNumber,
            ),
        ) {
            onSeriesBack()
        }
        BackHandler(
            shouldReturnToCatalogCategories(
                screen = screen,
                showCategoryLanding = showCatalogCategoryLanding,
                selectedSeriesTitle = selectedSeriesTitle,
                selectedSeasonNumber = selectedSeasonNumber,
            ),
        ) {
            onShowCatalogCategories()
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val wide = maxWidth >= 900.dp
            val contentPadding = if (wide) 30.dp else 18.dp
            val focusManager = LocalFocusManager.current
            BackHandler(
                enabled = wide &&
                    !showPlaylistEntry &&
                    screen != AppScreen.PLAYER &&
                    !shouldHandleSeriesBack(
                        screen = screen,
                        selectedTab = selectedTab,
                        showCategoryLanding = showCatalogCategoryLanding,
                        selectedSeriesTitle = selectedSeriesTitle,
                        selectedSeasonNumber = selectedSeasonNumber,
                    ) &&
                    !shouldReturnToCatalogCategories(
                        screen = screen,
                        showCategoryLanding = showCatalogCategoryLanding,
                        selectedSeriesTitle = selectedSeriesTitle,
                        selectedSeasonNumber = selectedSeasonNumber,
                    ),
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
                    playbackItems = playerContextItems,
                    playerUiMode = playerUiMode,
                    onSelectItem = onSelectPlayerItem,
                    onBack = onPlayerBack,
                )
                showPlaylistEntry -> PlaylistEntryScreen(
                    playlists = playlists,
                    selectedPlaylistId = selectedPlaylist?.id,
                    detailPlaylistId = playlistDetailId,
                    contentPadding = contentPadding,
                    onOpenLastPlaylist = onOpenLastPlaylist,
                    onAddPlaylist = onAddPlaylist,
                    onOpenPlaylistDetails = onOpenPlaylistDetails,
                    onClosePlaylistDetails = onClosePlaylistDetails,
                    onUsePlaylist = onUsePlaylist,
                    onReloadPlaylist = onReloadPlaylist,
                    onRenamePlaylist = onRenamePlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onAutoUpdateHoursChange = onAutoUpdateHoursChange,
                    onOpenSettings = onOpenSettingsFromEntry,
                    progress = playlistImportProgress,
                    syncStatuses = catalogSyncStatuses,
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
                    showCatalogCategoryLanding = showCatalogCategoryLanding,
                    selectedSeriesTitle = selectedSeriesTitle,
                    selectedSeasonNumber = selectedSeasonNumber,
                    favoriteIds = favoriteIds,
                    recentIds = recentIds,
                    favoriteItems = favoriteItems,
                    recentItems = recentItems,
                    playlistDetailId = playlistDetailId,
                    diagnostics = diagnostics,
                    playerUiMode = playerUiMode,
                    banner = banner,
                    playlistImportProgress = playlistImportProgress,
                    catalogSyncStatuses = catalogSyncStatuses,
                    sideMenuExpanded = sideMenuExpanded,
                    drawerFocusExpansion = drawerFocusExpansion,
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
                    onOpenPlaylistDetails = onOpenPlaylistDetails,
                    onClosePlaylistDetails = onClosePlaylistDetails,
                    onUsePlaylist = onUsePlaylist,
                    onReloadPlaylist = onReloadPlaylist,
                    onRenamePlaylist = onRenamePlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onAutoUpdateHoursChange = onAutoUpdateHoursChange,
                    onTabSelected = onTabSelected,
                    onCategorySelected = onCategorySelected,
                    onSeriesSelected = onSeriesSelected,
                    onSeasonSelected = onSeasonSelected,
                    onShowItemOptions = onShowItemOptions,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onOpenPlaylistEntry = onOpenPlaylistEntry,
                        onNavigate = onNavigate,
                        onDrawerEvent = onDrawerEvent,
                        onPlayerUiModeChange = onPlayerUiModeChange,
                        onRequestExitConfirmation = onRequestExitConfirmation,
                        onDismissBanner = onDismissBanner,
                    )
            }
        }
    }
}
