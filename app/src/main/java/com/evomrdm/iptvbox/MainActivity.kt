package com.evomrdm.iptvbox

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.data.playlist.RemotePlaylistLoader
@Composable
internal fun IptvBoxApp(telemetry: AppPerformanceTelemetry) {
    val localContext = LocalContext.current; val context = localContext.applicationContext; val activity = localContext as? android.app.Activity
    val loader = remember { RemotePlaylistLoader() }
    val catalogStore = remember(context) { CatalogStore(context) }
    val catalogRepository = remember(catalogStore) { AppCatalogRepository(catalogStore) }
    val scope = rememberCoroutineScope()
    val stateStore = remember(context, catalogStore) { AppStateStore(context, catalogStore) }
    val updateService = remember { AppUpdateService() }
    val updateInstaller = remember(context) { AppUpdateInstaller(context) }
    val performanceMode = remember(context) { AppPerformanceMode.from(context) }
    val diagnostics by telemetry.diagnostics.collectAsState()
    val playlists = remember { mutableStateListOf<LoadedPlaylist>() }
    var restoredApplied by remember { mutableStateOf(false) }
    var showRecovery by rememberSaveable { mutableStateOf(false) }
    var bootError by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var showPlaylistEntry by rememberSaveable { mutableStateOf(true) }
    var drawerState by rememberSaveable { mutableStateOf(NavigationDrawerState.Collapsed) }; var drawerFocusExpansion by rememberSaveable { mutableStateOf(NavigationDrawerFocusExpansion.Enabled) }
    var returnScreen by rememberSaveable { mutableStateOf(AppScreen.CATALOG) }
    var selectedTab by rememberSaveable { mutableStateOf(CatalogTab.LIVE) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }; var selectedSeriesTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSeasonNumber by rememberSaveable { mutableStateOf<Int?>(null) }; var searchDraft by rememberSaveable { mutableStateOf("") }
    var submittedSearch by rememberSaveable { mutableStateOf("") }; var showAddDialog by rememberSaveable { mutableStateOf(false) }; var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<LoadedPlaylist?>(null) }
    var banner by rememberSaveable { mutableStateOf<String?>(null) }
    var currentItem by remember { mutableStateOf<CatalogItem?>(null) }; var currentHeaders by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var catalogSnapshot by remember { mutableStateOf<CatalogSnapshot?>(null) }; var catalogIndexLoading by remember { mutableStateOf(false) }
    var firstDrawRecorded by remember { mutableStateOf(false) }; var updateCheckStarted by rememberSaveable { mutableStateOf(false) }
    var updateState by remember { mutableStateOf<AppUpdateUiState>(AppUpdateUiState.Hidden) }
    var pendingNavigationStartedAt by remember { mutableStateOf<Long?>(null) }
    var contentFocusRequest by remember { mutableStateOf(0) }
    var initialContentFocusApplied by rememberSaveable { mutableStateOf(false) }
    val favoriteIds = remember { mutableStateListOf<String>() }; val recentIds = remember { mutableStateListOf<String>() }
    val contentInitialFocusRequester = remember { FocusRequester() }
    val drawerModel = NavigationDrawerModel(drawerState, drawerFocusExpansion)
    RestoreAppStateEffect(
        stateStore = stateStore, telemetry = telemetry,
        onSuccess = { restoredState, hadCatalogProblem ->
            playlists.clear()
            playlists.addAll(restoredState.playlists)
            selectedPlaylistId = restoredState.selectedPlaylistId
            val restoredScreenValue = restoredScreen(restoredState.selectedScreen, restoredState.playlists.isNotEmpty())
            showPlaylistEntry = restoredState.playlists.isEmpty() ||
                restoredScreenValue == AppScreen.PLAYLISTS
            screen = if (showPlaylistEntry) AppScreen.PLAYLISTS else restoredScreenValue
            selectedTab = restoredTab(restoredState.selectedTab); selectedCategory = restoredState.selectedCategory
            selectedSeriesTitle = restoredState.selectedSeriesTitle; selectedSeasonNumber = restoredState.selectedSeasonNumber
            searchDraft = restoredState.searchDraft; submittedSearch = restoredState.submittedSearch
            favoriteIds.clear()
            favoriteIds.addAll(restoredState.favoriteIds.distinct())
            recentIds.clear()
            recentIds.addAll(restoredState.recentIds.distinct())
            showRecovery = hadCatalogProblem
            restoredApplied = true
        },
        onFailure = {
            telemetry.recordError("Açılış verisi okunamadı", it)
            bootError = "Uygulama verileri okunamadı."
            showRecovery = true
            showPlaylistEntry = true
            restoredApplied = true
        },
    )
    StartupUpdateEffect(
        contextPackageName = context.packageName, restoredApplied = restoredApplied, screen = screen,
        firstDrawRecorded = firstDrawRecorded, updateCheckStarted = updateCheckStarted,
        telemetry = telemetry, updateService = updateService,
        currentVersionCode = installedVersionCode(context),
        onFirstDrawRecorded = { firstDrawRecorded = true },
        onUpdateCheckStarted = { updateCheckStarted = true },
        onUpdateAvailable = { updateState = AppUpdateUiState.Available(it) },
    )
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId } ?: playlists.firstOrNull()
    SelectedPlaylistRepairEffect(
        restoredApplied = restoredApplied, selectedPlaylist = selectedPlaylist, selectedPlaylistId = selectedPlaylistId,
        screen = screen, onSelectPlaylist = { selectedPlaylistId = it },
        onMissingPlaylist = {
            screen = AppScreen.HOME
            showPlaylistEntry = true
        },
    )
    val playlistSignature = catalogSignature(playlists)
    val favoriteSignature = favoriteIds.joinToString("|")
    val recentSignature = recentIds.joinToString("|")
    CatalogSnapshotEffect(
        selectedPlaylist = selectedPlaylist, showPlaylistEntry = showPlaylistEntry, screen = screen,
        selectedTab = selectedTab, selectedCategory = selectedCategory, selectedSeriesTitle = selectedSeriesTitle,
        selectedSeasonNumber = selectedSeasonNumber, favoriteIds = favoriteIds, recentIds = recentIds,
        favoriteSignature = favoriteSignature, recentSignature = recentSignature, performanceMode = performanceMode,
        catalogStore = catalogStore, catalogRepository = catalogRepository, telemetry = telemetry,
        onSnapshotChange = { catalogSnapshot = it },
        onLoadingChange = { catalogIndexLoading = it },
        onRecoveryNeeded = {
            bootError = it
            showRecovery = true
        },
    )
    val favoriteItems = remember(catalogSnapshot, favoriteSignature) {
        catalogSnapshot?.itemsByIds(favoriteIds).orEmpty()
    }
    val recentItems = remember(catalogSnapshot, recentSignature) {
        catalogSnapshot?.itemsByIds(recentIds).orEmpty()
    }
    PersistAppStateEffect(
        restoredApplied = restoredApplied, showRecovery = showRecovery, playlistSignature = playlistSignature,
        selectedPlaylist = selectedPlaylist, playlists = playlists.toList(), screen = screen,
        showPlaylistEntry = showPlaylistEntry, selectedTab = selectedTab, selectedCategory = selectedCategory,
        selectedSeriesTitle = selectedSeriesTitle, selectedSeasonNumber = selectedSeasonNumber,
        searchDraft = searchDraft, submittedSearch = submittedSearch, favoriteSignature = favoriteSignature,
        recentSignature = recentSignature, favoriteIds = favoriteIds.toList(), recentIds = recentIds.toList(),
        stateStore = stateStore,
    )
    fun applyDrawerEvent(event: NavigationDrawerEvent) {
        val next = drawerModel.reduce(event).model; drawerState = next.state; drawerFocusExpansion = next.focusExpansion
    }
    fun requestContentFocus() {
        applyDrawerEvent(NavigationDrawerEvent.CollapseForContentFocus)
        contentFocusRequest += 1
    }
    fun openCatalogRoot(tab: CatalogTab = selectedPlaylist?.let(::firstAvailableTab) ?: CatalogTab.LIVE) {
        selectedTab = tab
        selectedCategory = null
        selectedSeriesTitle = null
        selectedSeasonNumber = null
        screen = AppScreen.CATALOG
        showPlaylistEntry = false
        requestContentFocus()
    }
    fun openHomeRailScreen(target: AppScreen) {
        pendingNavigationStartedAt = SystemClock.elapsedRealtime()
        screen = target
        showPlaylistEntry = false
        selectedSeriesTitle = null
        selectedSeasonNumber = null
        requestContentFocus()
    }
    fun openPlaylistCatalog(playlistId: String) {
        val playlist = playlists.firstOrNull { it.id == playlistId }
        selectedPlaylistId = playlistId
        selectedTab = playlist?.let(::firstAvailableTab) ?: CatalogTab.LIVE
        selectedCategory = null
        selectedSeriesTitle = null
        selectedSeasonNumber = null
        submittedSearch = ""
        searchDraft = ""
        screen = AppScreen.HOME
        showPlaylistEntry = false
        requestContentFocus()
    }
    fun openPlaylistEntry() {
        screen = AppScreen.PLAYLISTS
        showPlaylistEntry = true
        applyDrawerEvent(NavigationDrawerEvent.CollapseForContentFocus)
    }
    fun navigate(target: AppScreen) {
        pendingNavigationStartedAt = SystemClock.elapsedRealtime()
        if (!showPlaylistEntry && target == screen && target != AppScreen.CATALOG) {
            requestContentFocus()
            return
        }
        when (target) {
            AppScreen.CATALOG -> openCatalogRoot()
            AppScreen.PLAYLISTS -> openPlaylistEntry()
            else -> {
                screen = target
                showPlaylistEntry = false
                if (target != AppScreen.PLAYER) {
                    selectedSeriesTitle = null
                    selectedSeasonNumber = null
                }
                requestContentFocus()
            }
        }
    }
    InitialContentFocusEffect(
        restoredApplied = restoredApplied,
        selectedPlaylist = selectedPlaylist,
        showPlaylistEntry = showPlaylistEntry,
        screen = screen,
        initialContentFocusApplied = initialContentFocusApplied,
        onApplied = { initialContentFocusApplied = true },
        requestContentFocus = ::requestContentFocus,
    )
    NavigationTimingEffect(
        restoredApplied = restoredApplied,
        screen = screen,
        pendingNavigationStartedAt = pendingNavigationStartedAt,
        telemetry = telemetry,
        onRecorded = { pendingNavigationStartedAt = null },
    )
    AutoDismissBannerEffect(
        banner = banner,
        onDismiss = { banner = null },
    )

    fun openItem(item: CatalogItem) {
        recentIds.remove(item.id)
        recentIds.add(0, item.id)
        while (recentIds.size > 60) recentIds.removeLast()
        currentItem = item
        currentHeaders = selectedPlaylist?.headers.orEmpty()
        returnScreen = screen.takeIf { it != AppScreen.PLAYER } ?: AppScreen.CATALOG
        screen = AppScreen.PLAYER
    }
    fun reloadPlaylist(playlist: LoadedPlaylist) {
        scope.reloadPlaylistAction(
            playlist = playlist,
            loader = loader,
            telemetry = telemetry,
            catalogStore = catalogStore,
            onBanner = { banner = it },
            onStored = { stored ->
                val index = playlists.indexOfFirst { it.id == stored.id }
                if (index >= 0) playlists[index] = stored
            },
        )
    }
    fun startUpdateDownload(update: AppUpdateInfo) {
        scope.startUpdateDownloadAction(
            context = context,
            update = update,
            updateService = updateService,
            updateInstaller = updateInstaller,
            telemetry = telemetry,
            onUpdateState = { updateState = it },
        )
    }
    fun renamePlaylist(playlist: LoadedPlaylist, requestedName: String) {
        scope.renamePlaylistAction(
            playlist = playlist,
            requestedName = requestedName,
            playlists = playlists,
            catalogStore = catalogStore,
            onUpdated = { updated ->
            val index = playlists.indexOfFirst { it.id == playlist.id }
            if (index >= 0) playlists[index] = updated
            if (selectedPlaylistId == playlist.id) selectedPlaylistId = updated.id
            renamingPlaylist = null
            banner = "Oynatma listesi adı güncellendi"
            },
        )
    }
    fun clearBrokenState() {
        scope.clearBrokenStateAction(stateStore) {
            playlists.clear()
            favoriteIds.clear()
            recentIds.clear()
            selectedPlaylistId = null
            selectedCategory = null
            selectedSeriesTitle = null
            selectedSeasonNumber = null
            submittedSearch = ""
            searchDraft = ""
            screen = AppScreen.PLAYLISTS
            showPlaylistEntry = true
            showRecovery = false
            bootError = null
            banner = "Sorunlu liste kaldırıldı"
        }
    }
    fun openSettingsFromEntry() {
        screen = AppScreen.SETTINGS
        showPlaylistEntry = false
        requestContentFocus()
    }

    fun openSeriesCatalog(title: String) {
        selectedTab = CatalogTab.SERIES
        selectedCategory = null
        selectedSeriesTitle = title
        selectedSeasonNumber = null
        screen = AppScreen.CATALOG
        showPlaylistEntry = false
        requestContentFocus()
    }

    fun resetCatalogNavigation(tab: CatalogTab? = null, category: String? = selectedCategory) {
        tab?.let { selectedTab = it }
        selectedCategory = category
        selectedSeriesTitle = null
        selectedSeasonNumber = null
    }

    IptvContentHost(
        performanceMode = performanceMode, restoredApplied = restoredApplied, showRecovery = showRecovery,
        bootError = bootError, selectedPlaylist = selectedPlaylist, screen = screen,
        showPlaylistEntry = showPlaylistEntry, currentItem = currentItem, currentHeaders = currentHeaders,
        playlists = playlists, catalogSnapshot = catalogSnapshot, catalogIndexLoading = catalogIndexLoading,
        selectedTab = selectedTab, selectedCategory = selectedCategory, selectedSeriesTitle = selectedSeriesTitle,
        selectedSeasonNumber = selectedSeasonNumber, favoriteIds = favoriteIds, recentIds = recentIds,
        favoriteItems = favoriteItems, recentItems = recentItems, diagnostics = diagnostics, banner = banner,
        sideMenuExpanded = drawerModel.state.expanded, contentFocusRequest = contentFocusRequest,
        contentInitialFocusRequester = contentInitialFocusRequester, catalogRepository = catalogRepository,
        telemetry = telemetry, searchDraft = searchDraft, submittedSearch = submittedSearch,
        onPlayerBack = { screen = returnScreen; currentItem = null },
        onSeriesBack = {
            if (selectedSeasonNumber != null) selectedSeasonNumber = null else selectedSeriesTitle = null
        },
        onRecoveryContinue = { showRecovery = false; bootError = null },
        onRecoveryReload = {
            showRecovery = false; bootError = null
            selectedPlaylist?.let(::reloadPlaylist) ?: run { showAddDialog = true }
        },
        onRecoveryRemove = ::clearBrokenState, onOpenLastPlaylist = {
            selectedPlaylist?.let { openPlaylistCatalog(it.id) } ?: run { showAddDialog = true }
        },
        onAddPlaylist = { showAddDialog = true }, onSelectPlaylist = ::openPlaylistCatalog,
        onOpenSettingsFromEntry = ::openSettingsFromEntry, onOpenCatalog = { openCatalogRoot() },
        onOpenCatalogTab = { openCatalogRoot(it) }, onOpenLatest = { openHomeRailScreen(AppScreen.LATEST) },
        onOpenFavorites = { openHomeRailScreen(AppScreen.FAVORITES) }, onOpenRecent = { openHomeRailScreen(AppScreen.RECENT) },
        onOpenSeries = ::openSeriesCatalog, onOpenItem = ::openItem, onReloadPlaylist = ::reloadPlaylist,
        onRenamePlaylist = { renamingPlaylist = it }, onTabSelected = { resetCatalogNavigation(tab = it, category = null) },
        onCategorySelected = { resetCatalogNavigation(category = it) },
        onSeriesSelected = { selectedSeriesTitle = it; selectedSeasonNumber = null },
        onSeasonSelected = { selectedSeasonNumber = it }, onToggleFavorite = { toggleFavorite(favoriteIds, it.id) },
        onQueryChange = { searchDraft = it }, onSearch = { submittedSearch = searchDraft.trim() },
        onOpenPlaylistEntry = ::openPlaylistEntry, onNavigate = ::navigate, onDrawerEvent = ::applyDrawerEvent,
        onRequestExitConfirmation = { showExitDialog = true }, onDismissBanner = { banner = null },
    )

    IptvDialogHost(
        showAddDialog = showAddDialog, showExitDialog = showExitDialog, loader = loader, telemetry = telemetry,
        existingPlaylistNames = playlists.map { it.name }, renamingPlaylist = renamingPlaylist,
        updateState = updateState, screen = screen, showRecovery = showRecovery,
        onDismissAdd = { showAddDialog = false }, onDismissExit = { showExitDialog = false },
        onConfirmExit = { showExitDialog = false; activity?.finish() },
        onPlaylistLoaded = { draft, result ->
            scope.saveLoadedPlaylistAction(
                draft = draft, result = result, telemetry = telemetry, catalogStore = catalogStore,
                onSaving = { banner = "Oynatma listesi kaydediliyor" },
                onStored = { stored, itemCount, playlistName ->
                    playlists.removeAll { it.id == stored.id }; playlists += stored; selectedPlaylistId = stored.id
                    selectedTab = firstAvailableTab(stored); selectedCategory = null; selectedSeriesTitle = null
                    selectedSeasonNumber = null; submittedSearch = ""; screen = AppScreen.HOME; showPlaylistEntry = false
                    requestContentFocus(); banner = "$playlistName yüklendi: $itemCount içerik"; showAddDialog = false
                },
                onFailure = { message -> banner = message; showAddDialog = false },
            )
        },
        onDismissRename = { renamingPlaylist = null }, onRenamePlaylist = ::renamePlaylist,
        onDownloadUpdate = {
            val update = when (val state = updateState) {
                is AppUpdateUiState.Available -> state.update; is AppUpdateUiState.Downloading -> state.update
                is AppUpdateUiState.PermissionRequired -> state.update; else -> null
            }
            if (update != null) startUpdateDownload(update)
        },
        onOpenPermission = { updateInstaller.openInstallPermissionSettings() }, onOpenInstaller = {
            val state = updateState as? AppUpdateUiState.PermissionRequired
            if (state != null && updateInstaller.canInstallPackages()) {
                updateInstaller.openInstaller(state.file); updateState = AppUpdateUiState.Hidden
            } else if (state != null) updateInstaller.openInstallPermissionSettings()
        }, onDismissUpdate = { updateState = AppUpdateUiState.Hidden },
    )
}
