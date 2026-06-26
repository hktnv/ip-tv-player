package com.evomrdm.iptvbox

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import com.evomrdm.iptvbox.core.designsystem.IptvTheme
import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.core.model.ContentHint
import com.evomrdm.iptvbox.data.playlist.CreatePlaylistSourceRequest
import com.evomrdm.iptvbox.data.playlist.RemotePlaylistLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val telemetry = AppPerformanceTelemetry(this)
        telemetry.mark("cold_start_on_create")
        setContent {
            IptvTheme {
                IptvBoxApp(telemetry)
            }
        }
    }
}

@Composable
private fun IptvBoxApp(telemetry: AppPerformanceTelemetry) {
    val context = LocalContext.current.applicationContext
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
    var sideMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var returnScreen by rememberSaveable { mutableStateOf(AppScreen.CATALOG) }
    var selectedTab by rememberSaveable { mutableStateOf(CatalogTab.LIVE) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSeriesTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSeasonNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var searchDraft by rememberSaveable { mutableStateOf("") }
    var submittedSearch by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<LoadedPlaylist?>(null) }
    var banner by rememberSaveable { mutableStateOf<String?>(null) }
    var currentItem by remember { mutableStateOf<CatalogItem?>(null) }
    var currentHeaders by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var catalogSnapshot by remember { mutableStateOf<CatalogSnapshot?>(null) }
    var catalogIndexLoading by remember { mutableStateOf(false) }
    var firstDrawRecorded by remember { mutableStateOf(false) }
    var updateCheckStarted by rememberSaveable { mutableStateOf(false) }
    var updateState by remember { mutableStateOf<AppUpdateUiState>(AppUpdateUiState.Hidden) }
    var pendingNavigationStartedAt by remember { mutableStateOf<Long?>(null) }
    val favoriteIds = remember { mutableStateListOf<String>() }
    val recentIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val restoreStartedAt = SystemClock.elapsedRealtime()
        runCatching { withContext(Dispatchers.IO) { stateStore.loadFast() } }
            .onSuccess { restoredState ->
                telemetry.recordDuration("cold_start_restore_state_ms", restoreStartedAt)
                playlists.clear()
                playlists.addAll(restoredState.playlists)
                selectedPlaylistId = restoredState.selectedPlaylistId
                val restoredScreenValue = restoredScreen(
                    name = restoredState.selectedScreen,
                    hasPlaylist = restoredState.playlists.isNotEmpty(),
                )
                showPlaylistEntry = restoredState.playlists.isEmpty() ||
                    restoredScreenValue == AppScreen.PLAYLISTS
                screen = if (showPlaylistEntry) AppScreen.PLAYLISTS else restoredScreenValue
                selectedTab = restoredTab(restoredState.selectedTab)
                selectedCategory = restoredState.selectedCategory
                selectedSeriesTitle = restoredState.selectedSeriesTitle
                selectedSeasonNumber = restoredState.selectedSeasonNumber
                searchDraft = restoredState.searchDraft
                submittedSearch = restoredState.submittedSearch
                favoriteIds.clear()
                favoriteIds.addAll(restoredState.favoriteIds.distinct())
                recentIds.clear()
                recentIds.addAll(restoredState.recentIds.distinct())
                showRecovery = stateStore.lastLoadHadCatalogProblem
                restoredApplied = true
            }
            .onFailure {
                telemetry.recordDuration("cold_start_restore_state_failed_ms", restoreStartedAt)
                telemetry.recordError("Açılış verisi okunamadı", it)
                bootError = "Uygulama verileri okunamadı."
                showRecovery = true
                showPlaylistEntry = true
                restoredApplied = true
            }
            /*
                bootError = "Kayıtlı liste güvenli şekilde okunamadı."
                showRecovery = true
                catalogRestoreComplete = true
            */
    }

    LaunchedEffect(restoredApplied, screen) {
        if (restoredApplied && !firstDrawRecorded) {
            withFrameNanos { }
            telemetry.record("home_first_draw_ms", telemetry.sinceAppStartMs())
            firstDrawRecorded = true
        }
    }

    LaunchedEffect(firstDrawRecorded) {
        if (!firstDrawRecorded || updateCheckStarted) return@LaunchedEffect
        updateCheckStarted = true
        when (
            val result = updateService.checkForUpdate(
                packageName = context.packageName,
                currentVersionCode = installedVersionCode(context),
            )
        ) {
            is AppUpdateCheckResult.Available -> {
                updateState = AppUpdateUiState.Available(result.update)
            }
            is AppUpdateCheckResult.Failed -> Unit
            AppUpdateCheckResult.NoUpdate -> Unit
        }
    }

    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId } ?: playlists.firstOrNull()
    LaunchedEffect(restoredApplied, selectedPlaylist?.id) {
        if (restoredApplied && selectedPlaylistId == null && selectedPlaylist != null) {
            selectedPlaylistId = selectedPlaylist.id
        }
    }
    LaunchedEffect(restoredApplied, selectedPlaylist?.id, screen) {
        if (restoredApplied && selectedPlaylist == null && screen.requiresPlaylist()) {
            screen = AppScreen.HOME
            showPlaylistEntry = true
        }
    }

    LaunchedEffect(
        selectedPlaylist?.id,
        selectedPlaylist?.cachedItemCount,
        selectedPlaylist?.cachedLiveCount,
        selectedPlaylist?.cachedMovieCount,
        selectedPlaylist?.cachedSeriesCount,
        screen,
        selectedTab,
        selectedCategory,
        selectedSeriesTitle,
        selectedSeasonNumber,
        showPlaylistEntry,
        favoriteIds.joinToString("|"),
        recentIds.joinToString("|"),
    ) {
        val playlist = selectedPlaylist
        if (playlist == null || showPlaylistEntry) {
            catalogSnapshot = null
            catalogIndexLoading = false
        } else {
            catalogIndexLoading = true
            catalogSnapshot = null
            val catalogStartedAt = SystemClock.elapsedRealtime()
            val snapshot = withContext(Dispatchers.IO) {
                if (playlist.items.isEmpty() && !catalogStore.hasItems(playlist.id)) {
                    null
                } else {
                    catalogRepository.buildUiSnapshot(
                        playlist = playlist,
                        selectedTab = selectedTab,
                        selectedCategory = selectedCategory,
                        selectedSeriesTitle = selectedSeriesTitle,
                        selectedSeasonNumber = selectedSeasonNumber,
                        favoriteIds = favoriteIds,
                        recentIds = recentIds,
                        previewLimit = performanceMode.homePreviewLimit,
                        visibleLimit = when (selectedTab) {
                            CatalogTab.LIVE -> 600
                            CatalogTab.MOVIES -> 420
                            CatalogTab.SERIES -> 360
                        },
                    )
                }
            }
            if (snapshot == null) {
                bootError = "Liste yeniden yüklenebilir."
                showRecovery = true
            } else {
                catalogSnapshot = snapshot
                telemetry.recordDuration("catalog_screen_ready_ms", catalogStartedAt)
            }
            catalogIndexLoading = false
        }
    }

    val playlistSignature = catalogSignature(playlists)
    val favoriteSignature = favoriteIds.joinToString("|")
    val recentSignature = recentIds.joinToString("|")
    val favoriteItems = remember(catalogSnapshot, favoriteSignature) {
        catalogSnapshot?.itemsByIds(favoriteIds).orEmpty()
    }
    val recentItems = remember(catalogSnapshot, recentSignature) {
        catalogSnapshot?.itemsByIds(recentIds).orEmpty()
    }
    LaunchedEffect(
        restoredApplied,
        playlistSignature,
        selectedPlaylistId,
        screen,
        showRecovery,
        selectedTab,
        selectedCategory,
        selectedSeriesTitle,
        selectedSeasonNumber,
        showPlaylistEntry,
        searchDraft,
        submittedSearch,
        favoriteSignature,
        recentSignature,
    ) {
        if (restoredApplied && !showRecovery) {
            stateStore.save(
                PersistedAppState(
                    playlists = playlists.toList(),
                    selectedPlaylistId = selectedPlaylist?.id,
                    selectedScreen = if (showPlaylistEntry) {
                        AppScreen.PLAYLISTS.name
                    } else {
                        screen.takeUnless { it == AppScreen.PLAYER }?.name
                    },
                    selectedTab = selectedTab.name,
                    selectedCategory = selectedCategory,
                    selectedSeriesTitle = selectedSeriesTitle,
                    selectedSeasonNumber = selectedSeasonNumber,
                    searchDraft = searchDraft,
                    submittedSearch = submittedSearch,
                    favoriteIds = favoriteIds.toList(),
                    recentIds = recentIds.toList(),
                ),
            )
        }
    }

    fun openCatalogRoot(tab: CatalogTab = selectedPlaylist?.let(::firstAvailableTab) ?: CatalogTab.LIVE) {
        selectedTab = tab
        selectedCategory = null
        selectedSeriesTitle = null
        selectedSeasonNumber = null
        screen = AppScreen.CATALOG
        showPlaylistEntry = false
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
        sideMenuExpanded = false
    }

    fun openPlaylistEntry() {
        screen = AppScreen.PLAYLISTS
        showPlaylistEntry = true
        sideMenuExpanded = true
    }

    fun navigate(target: AppScreen) {
        if (!showPlaylistEntry && target == screen && target != AppScreen.CATALOG) return
        pendingNavigationStartedAt = SystemClock.elapsedRealtime()
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
            }
        }
    }

    LaunchedEffect(screen) {
        val startedAt = pendingNavigationStartedAt
        if (restoredApplied && startedAt != null) {
            withFrameNanos { }
            telemetry.recordDuration("menu_transition_${screen.name.lowercase()}_ms", startedAt)
            pendingNavigationStartedAt = null
        }
    }

    LaunchedEffect(banner) {
        val visibleMessage = banner ?: return@LaunchedEffect
        delay(3600)
        if (banner == visibleMessage) {
            banner = null
        }
    }

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
        banner = "${playlist.name} yenileniyor"
        scope.launch {
            val request = CreatePlaylistSourceRequest(
                type = playlist.type,
                name = playlist.name,
                endpoint = playlist.endpoint,
                headers = playlist.headers,
                contentHint = ContentHint.AUTO,
            )
            runCatching { loader.load(playlist.id, request) }
                .onSuccess { result ->
                    recordPlaylistLoadMetrics(telemetry, result.metrics)
                    telemetry.record("playlist_import_image_ms", 0L)
                    val index = playlists.indexOfFirst { it.id == playlist.id }
                    if (index >= 0) {
                        val normalizeStartedAt = SystemClock.elapsedRealtime()
                        val normalized = withContext(Dispatchers.Default) {
                            playlist.copy(
                                items = result.items,
                                epgUrls = result.epgUrls,
                                warnings = result.warnings.map(::simpleUserMessage).filter { it.isNotBlank() }.distinct().take(1),
                            ).normalizedForUi()
                        }
                        telemetry.recordDuration("playlist_import_normalize_ms", normalizeStartedAt)
                        val writeResult = withContext(Dispatchers.IO) {
                            catalogStore.replacePlaylistMeasured(normalized)
                        }
                        recordCatalogWriteTimings(telemetry, writeResult.timings)
                        playlists[index] = writeResult.playlist
                    }
                    telemetry.record("playlist_import_item_count", result.items.size.toLong())
                    banner = "${playlist.name} yenilendi: ${result.items.size} içerik"
                }
                .onFailure { throwable ->
                    telemetry.recordError("Liste yenileme hatası", throwable)
                    banner = simpleUserMessage(throwable.message.orEmpty()).ifBlank { "Liste yüklenemedi" }
                }
        }
    }

    fun startUpdateDownload(update: AppUpdateInfo) {
        scope.launch {
            updateState = AppUpdateUiState.Downloading(update, progress = null)
            runCatching {
                updateService.downloadVerifiedApk(
                    cacheDir = context.cacheDir,
                    release = update.release,
                    onProgress = { progress ->
                        updateState = AppUpdateUiState.Downloading(update, progress)
                    },
                )
            }.onSuccess { apkFile ->
                if (updateInstaller.canInstallPackages()) {
                    updateInstaller.openInstaller(apkFile)
                    updateState = AppUpdateUiState.Hidden
                } else {
                    updateState = AppUpdateUiState.PermissionRequired(update, apkFile)
                }
            }.onFailure { throwable ->
                telemetry.recordError("Güncelleme indirme hatası", throwable)
                updateState = AppUpdateUiState.Error(
                    message = simpleUserMessage(throwable.message.orEmpty()).ifBlank {
                        "Güncelleme indirilemedi."
                    },
                    required = false,
                )
            }
        }
    }

    fun renamePlaylist(playlist: LoadedPlaylist, requestedName: String) {
        val finalName = resolvedPlaylistName(
            requestedName = requestedName,
            type = playlist.type,
            endpoint = playlist.endpoint,
            existingNames = playlists.filterNot { it.id == playlist.id }.map { it.name },
        )
        scope.launch {
            val updated = withContext(Dispatchers.IO) {
                catalogStore.updatePlaylistName(playlist.id, finalName)
            } ?: playlist.copy(name = finalName)
            val index = playlists.indexOfFirst { it.id == playlist.id }
            if (index >= 0) playlists[index] = updated
            if (selectedPlaylistId == playlist.id) selectedPlaylistId = updated.id
            renamingPlaylist = null
            banner = "Oynatma listesi adı güncellendi"
        }
    }

    CompositionLocalProvider(LocalPerformanceMode provides performanceMode) {
        BackHandler(screen == AppScreen.PLAYER) {
            screen = returnScreen
            currentItem = null
        }
        BackHandler(
            screen == AppScreen.CATALOG &&
                selectedTab == CatalogTab.SERIES &&
                (selectedSeasonNumber != null || selectedSeriesTitle != null),
        ) {
            if (selectedSeasonNumber != null) {
                selectedSeasonNumber = null
            } else {
                selectedSeriesTitle = null
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(IptvColors.Night),
        ) {
            val wide = maxWidth >= 900.dp
            val contentPadding = if (wide) 30.dp else 18.dp

            if (!restoredApplied) {
                BootScreen(contentPadding = contentPadding)
            } else if (showRecovery) {
                RecoveryScreen(
                    message = bootError ?: "Önceki liste güvenli şekilde açılamadı.",
                    hasPlaylist = selectedPlaylist != null,
                    contentPadding = contentPadding,
                    onContinue = {
                        showRecovery = false
                        bootError = null
                    },
                    onReload = {
                        showRecovery = false
                        bootError = null
                        selectedPlaylist?.let(::reloadPlaylist) ?: run { showAddDialog = true }
                    },
                    onRemove = {
                        scope.launch {
                            withContext(Dispatchers.IO) { stateStore.clear() }
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
                    },
                )
            } else if (screen == AppScreen.PLAYER && currentItem != null) {
                PlayerScreen(
                    item = currentItem!!,
                    headers = currentHeaders,
                    onBack = {
                        screen = returnScreen
                        currentItem = null
                    },
                )
            } else if (showPlaylistEntry) {
                PlaylistEntryScreen(
                    playlists = playlists,
                    selectedPlaylistId = selectedPlaylist?.id,
                    contentPadding = contentPadding,
                    onOpenLastPlaylist = {
                        selectedPlaylist?.let { openPlaylistCatalog(it.id) } ?: run {
                            showAddDialog = true
                        }
                    },
                    onAddPlaylist = { showAddDialog = true },
                    onSelectPlaylist = ::openPlaylistCatalog,
                    onOpenSettings = {
                        screen = AppScreen.SETTINGS
                        showPlaylistEntry = false
                    },
                )
            } else {
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
                                onAddPlaylist = { showAddDialog = true },
                                onOpenCatalog = { openCatalogRoot() },
                                onOpenCatalogTab = { openCatalogRoot(it) },
                                onOpenFavorites = { navigate(AppScreen.FAVORITES) },
                                onOpenRecent = { navigate(AppScreen.RECENT) },
                                onOpenSeries = {
                                    selectedTab = CatalogTab.SERIES
                                    selectedCategory = null
                                    selectedSeriesTitle = it
                                    selectedSeasonNumber = null
                                    screen = AppScreen.CATALOG
                                },
                                onOpenItem = ::openItem,
                                onSelectPlaylist = {
                                    openPlaylistCatalog(it)
                                },
                                contentPadding = contentPadding,
                            )
                            AppScreen.PLAYLISTS -> PlaylistScreen(
                                playlists = playlists,
                                selectedPlaylistId = selectedPlaylist?.id,
                                onAddPlaylist = { showAddDialog = true },
                                onSelectPlaylist = {
                                    openPlaylistCatalog(it)
                                },
                                onReload = ::reloadPlaylist,
                                onRename = { renamingPlaylist = it },
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
                                onTabSelected = {
                                    selectedTab = it
                                    selectedCategory = null
                                    selectedSeriesTitle = null
                                    selectedSeasonNumber = null
                                },
                                onCategorySelected = {
                                    selectedCategory = it
                                    selectedSeriesTitle = null
                                    selectedSeasonNumber = null
                                },
                                onSeriesSelected = {
                                    selectedSeriesTitle = it
                                    selectedSeasonNumber = null
                                },
                                onSeasonSelected = { selectedSeasonNumber = it },
                                onOpenItem = ::openItem,
                                onToggleFavorite = { toggleFavorite(favoriteIds, it.id) },
                                onAddPlaylist = { showAddDialog = true },
                                contentPadding = contentPadding,
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
                                onQueryChange = { searchDraft = it },
                                onSearch = { submittedSearch = searchDraft.trim() },
                                onOpenItem = ::openItem,
                                onToggleFavorite = { toggleFavorite(favoriteIds, it.id) },
                                onAddPlaylist = { showAddDialog = true },
                                contentPadding = contentPadding,
                            )
                            AppScreen.FAVORITES -> SavedItemsScreen(
                                title = "Favoriler",
                                emptyText = "Favori içerik yok",
                                playlist = selectedPlaylist,
                                snapshot = catalogSnapshot,
                                catalogIndexLoading = catalogIndexLoading,
                                itemIds = favoriteIds,
                                favoriteIds = favoriteIds,
                                onOpenItem = ::openItem,
                                onToggleFavorite = { toggleFavorite(favoriteIds, it.id) },
                                onAddPlaylist = { showAddDialog = true },
                                contentPadding = contentPadding,
                            )
                            AppScreen.RECENT -> SavedItemsScreen(
                                title = "Son izlenenler",
                                emptyText = "Henüz izlenen içerik yok",
                                playlist = selectedPlaylist,
                                snapshot = catalogSnapshot,
                                catalogIndexLoading = catalogIndexLoading,
                                itemIds = recentIds,
                                favoriteIds = favoriteIds,
                                onOpenItem = ::openItem,
                                onToggleFavorite = { toggleFavorite(favoriteIds, it.id) },
                                onAddPlaylist = { showAddDialog = true },
                                contentPadding = contentPadding,
                            )
                            AppScreen.SETTINGS -> SettingsScreen(
                                playlist = selectedPlaylist,
                                diagnostics = diagnostics,
                                onReload = { selectedPlaylist?.let(::reloadPlaylist) },
                                onAddPlaylist = { showAddDialog = true },
                                onOpenPlaylistEntry = ::openPlaylistEntry,
                                contentPadding = contentPadding,
                            )
                            AppScreen.PLAYER -> Unit
                        }
                    }
                    if (!wide) {
                        BottomNavigation(
                            selected = screen,
                            selectedTab = selectedTab,
                            hasPlaylist = selectedPlaylist != null,
                            stats = selectedPlaylist?.stats(),
                            onNavigate = ::navigate,
                            onOpenTab = { openCatalogRoot(it) },
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
                        onExpandedChange = { sideMenuExpanded = it },
                        onNavigate = ::navigate,
                        onOpenTab = { openCatalogRoot(it) },
                    )
                }
                banner?.let {
                    FloatingStatusToast(
                        text = it,
                        onDismiss = { banner = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = if (wide) 28.dp else 14.dp, start = 14.dp),
                    )
                }
            }
        }
    }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            loader = loader,
            telemetry = telemetry,
            existingPlaylistNames = playlists.map { it.name },
            onDismiss = { showAddDialog = false },
            onLoaded = { draft, result ->
                banner = "Oynatma listesi kaydediliyor"
                scope.launch {
                    runCatching {
                        val normalizeStartedAt = SystemClock.elapsedRealtime()
                        val playlist = withContext(Dispatchers.Default) {
                            LoadedPlaylist(
                                id = draft.id,
                                name = draft.name,
                                type = draft.type,
                                endpoint = draft.endpoint,
                                headers = draft.headers,
                                items = result.items,
                                epgUrls = result.epgUrls,
                                warnings = result.warnings.map(::simpleUserMessage).filter { it.isNotBlank() }.distinct().take(1),
                            ).normalizedForUi()
                        }
                        val normalizeMs = SystemClock.elapsedRealtime() - normalizeStartedAt
                        val writeResult = withContext(Dispatchers.IO) {
                            catalogStore.replacePlaylistMeasured(playlist)
                        }
                        val uiStartedAt = SystemClock.elapsedRealtime()
                        val stored = writeResult.playlist
                        playlists.removeAll { it.id == stored.id }
                        playlists += stored
                        selectedPlaylistId = stored.id
                        selectedTab = firstAvailableTab(stored)
                        selectedCategory = null
                        selectedSeriesTitle = null
                        selectedSeasonNumber = null
                        submittedSearch = ""
                        screen = AppScreen.HOME
                        showPlaylistEntry = false
                        sideMenuExpanded = false
                        banner = "${playlist.name} yüklendi: ${playlist.items.size} içerik"
                        showAddDialog = false
                        withFrameNanos { }
                        val uiUpdateMs = SystemClock.elapsedRealtime() - uiStartedAt
                        finishPlaylistImportTelemetry(
                            telemetry = telemetry,
                            importStartedAtMs = draft.loadStartedAtMs,
                            firstResponseMs = draft.firstResponseMs,
                            metrics = result.metrics,
                            dbTimings = writeResult.timings,
                            normalizeMs = normalizeMs,
                            uiUpdateMs = uiUpdateMs,
                            itemCount = result.items.size,
                        )
                    }.onFailure { throwable ->
                        telemetry.recordError("Oynatma listesi kaydetme hatası", throwable)
                        banner = simpleUserMessage(throwable.message.orEmpty()).ifBlank { "Liste kaydedilemedi" }
                        showAddDialog = false
                    }
                    telemetry.endUiWatch(draft.uiWatchId)
                }
            },
        )
    }

    renamingPlaylist?.let { playlist ->
        RenamePlaylistDialog(
            playlist = playlist,
            onDismiss = { renamingPlaylist = null },
            onSave = { renamePlaylist(playlist, it) },
        )
    }

    val visibleUpdateState = updateState.takeUnless {
        it is AppUpdateUiState.Hidden ||
            screen == AppScreen.PLAYER ||
            showAddDialog ||
            renamingPlaylist != null ||
            showRecovery
    }
    if (visibleUpdateState != null) {
        AppUpdateDialog(
            state = visibleUpdateState,
            onDownload = {
                val update = when (val state = updateState) {
                    is AppUpdateUiState.Available -> state.update
                    is AppUpdateUiState.Downloading -> state.update
                    is AppUpdateUiState.PermissionRequired -> state.update
                    else -> null
                }
                if (update != null) startUpdateDownload(update)
            },
            onOpenPermission = { updateInstaller.openInstallPermissionSettings() },
            onOpenInstaller = {
                val state = updateState as? AppUpdateUiState.PermissionRequired ?: return@AppUpdateDialog
                if (updateInstaller.canInstallPackages()) {
                    updateInstaller.openInstaller(state.file)
                    updateState = AppUpdateUiState.Hidden
                } else {
                    updateInstaller.openInstallPermissionSettings()
                }
            },
            onDismiss = { updateState = AppUpdateUiState.Hidden },
        )
    }
}

private fun installedVersionCode(context: Context): Int {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode
    }
}

