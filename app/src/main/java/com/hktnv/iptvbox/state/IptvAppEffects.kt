package com.hktnv.iptvbox.state
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.data.catalog.CatalogStore
import com.hktnv.iptvbox.data.catalog.MetadataCleanupScheduler
import com.hktnv.iptvbox.model.AppPerformanceMode
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.HomePreviewLimit
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.ui.media.requiresPlaylist
import com.hktnv.iptvbox.update.AppUpdateCheckResult
import com.hktnv.iptvbox.update.AppUpdateInfo
import com.hktnv.iptvbox.update.AppUpdateService

@Composable
internal fun RestoreAppStateEffect(
    stateStore: AppStateStore,
    telemetry: AppPerformanceTelemetry,
    onSuccess: (PersistedAppState, Boolean) -> Unit,
    onFailure: (Throwable) -> Unit,
) {
    LaunchedEffect(Unit) {
        val restoreStartedAt = SystemClock.elapsedRealtime()
        runCatching { withContext(Dispatchers.IO) { stateStore.loadFast() } }
            .onSuccess { restoredState ->
                telemetry.recordDuration("cold_start_restore_state_ms", restoreStartedAt)
                onSuccess(restoredState, stateStore.lastLoadHadCatalogProblem)
            }
            .onFailure {
                telemetry.recordDuration("cold_start_restore_state_failed_ms", restoreStartedAt)
                onFailure(it)
            }
    }
}

@Composable
internal fun ScheduledMetadataCleanupEffect(
    restoredApplied: Boolean,
    cleanupScheduler: MetadataCleanupScheduler,
) {
    LaunchedEffect(restoredApplied, cleanupScheduler) {
        if (!restoredApplied) return@LaunchedEffect
        cleanupScheduler.runAfterStartupDelay()
    }
}

@Composable
internal fun StartupUpdateEffect(
    contextPackageName: String,
    restoredApplied: Boolean,
    screen: AppScreen,
    firstDrawRecorded: Boolean,
    updateCheckStarted: Boolean,
    telemetry: AppPerformanceTelemetry,
    updateService: AppUpdateService,
    currentVersionCode: Int,
    onFirstDrawRecorded: () -> Unit,
    onUpdateCheckStarted: () -> Unit,
    onUpdateAvailable: (AppUpdateInfo) -> Unit,
) {
    LaunchedEffect(restoredApplied, screen) {
        if (restoredApplied && !firstDrawRecorded) {
            withFrameNanos { }
            telemetry.record("home_first_draw_ms", telemetry.sinceAppStartMs())
            onFirstDrawRecorded()
        }
    }

    LaunchedEffect(firstDrawRecorded) {
        if (!firstDrawRecorded || updateCheckStarted) return@LaunchedEffect
        onUpdateCheckStarted()
        when (
            val result = updateService.checkForUpdate(
                packageName = contextPackageName,
                currentVersionCode = currentVersionCode,
            )
        ) {
            is AppUpdateCheckResult.Available -> onUpdateAvailable(result.update)
            is AppUpdateCheckResult.Failed -> Unit
            AppUpdateCheckResult.NoUpdate -> Unit
        }
    }
}

@Composable
internal fun SelectedPlaylistRepairEffect(
    restoredApplied: Boolean,
    selectedPlaylist: LoadedPlaylist?,
    selectedPlaylistId: String?,
    screen: AppScreen,
    onSelectPlaylist: (String) -> Unit,
    onMissingPlaylist: () -> Unit,
) {
    LaunchedEffect(restoredApplied, selectedPlaylist?.id) {
        if (restoredApplied && selectedPlaylistId == null && selectedPlaylist != null) {
            onSelectPlaylist(selectedPlaylist.id)
        }
    }
    LaunchedEffect(restoredApplied, selectedPlaylist?.id, screen) {
        if (restoredApplied && selectedPlaylist == null && screen.requiresPlaylist()) {
            onMissingPlaylist()
        }
    }
}

@Composable
internal fun CatalogSnapshotEffect(
    selectedPlaylist: LoadedPlaylist?,
    showPlaylistEntry: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    currentSnapshotPlaylistId: String?,
    favoriteIds: List<String>,
    recentIds: List<String>,
    favoriteSignature: String,
    recentSignature: String,
    refreshToken: Int,
    performanceMode: AppPerformanceMode,
    catalogStore: CatalogStore,
    catalogRepository: AppCatalogRepository,
    telemetry: AppPerformanceTelemetry,
    onSnapshotChange: (CatalogSnapshot?) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onRecoveryNeeded: (String) -> Unit,
) {
    LaunchedEffect(
        selectedPlaylist?.id,
        selectedPlaylist?.cachedItemCount,
        selectedPlaylist?.cachedLiveCount,
        selectedPlaylist?.cachedMovieCount,
        selectedPlaylist?.cachedSeriesCount,
        selectedTab,
        selectedCategory,
        selectedSeriesTitle,
        selectedSeasonNumber,
        showPlaylistEntry,
        favoriteSignature,
        recentSignature,
        refreshToken,
    ) {
        val playlist = selectedPlaylist
        if (playlist == null || showPlaylistEntry) {
            val loadPlan = planCatalogSnapshotLoad(currentSnapshotPlaylistId, playlist?.id, showPlaylistEntry)
            if (loadPlan.clearSnapshot) onSnapshotChange(null)
            onLoadingChange(false)
            return@LaunchedEffect
        }

        val loadPlan = planCatalogSnapshotLoad(currentSnapshotPlaylistId, playlist.id, showPlaylistEntry)
        if (loadPlan.clearSnapshot) onSnapshotChange(null)
        onLoadingChange(loadPlan.showBlockingLoading)
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
            onRecoveryNeeded("Liste yeniden yüklenebilir.")
        } else {
            onSnapshotChange(snapshot)
            telemetry.recordDuration("catalog_screen_ready_ms", catalogStartedAt)
        }
        onLoadingChange(false)
    }
}

@Composable
internal fun PersistAppStateEffect(
    restoredApplied: Boolean,
    showRecovery: Boolean,
    playlistSignature: String,
    selectedPlaylist: LoadedPlaylist?,
    playlists: List<LoadedPlaylist>,
    screen: AppScreen,
    showPlaylistEntry: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    searchDraft: String,
    submittedSearch: String,
    favoriteSignature: String,
    recentSignature: String,
    favoriteIds: List<String>,
    recentIds: List<String>,
    stateStore: AppStateStore,
) {
    LaunchedEffect(
        restoredApplied,
        playlistSignature,
        selectedPlaylist?.id,
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
        if (!restoredApplied || showRecovery) return@LaunchedEffect
        stateStore.save(
            PersistedAppState(
                playlists = playlists,
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
                favoriteIds = favoriteIds,
                recentIds = recentIds,
            ),
        )
    }
}

@Composable
internal fun InitialContentFocusEffect(
    restoredApplied: Boolean,
    selectedPlaylist: LoadedPlaylist?,
    showPlaylistEntry: Boolean,
    screen: AppScreen,
    initialContentFocusApplied: Boolean,
    onApplied: () -> Unit,
    requestContentFocus: () -> Unit,
) {
    LaunchedEffect(restoredApplied, selectedPlaylist?.id, showPlaylistEntry, screen) {
        if (
            restoredApplied &&
            !initialContentFocusApplied &&
            selectedPlaylist != null &&
            !showPlaylistEntry &&
            screen != AppScreen.PLAYER
        ) {
            onApplied()
            delay(220L)
            requestContentFocus()
        }
    }
}

@Composable
internal fun NavigationTimingEffect(
    restoredApplied: Boolean,
    screen: AppScreen,
    pendingNavigationStartedAt: Long?,
    telemetry: AppPerformanceTelemetry,
    onRecorded: () -> Unit,
) {
    LaunchedEffect(screen) {
        val startedAt = pendingNavigationStartedAt
        if (restoredApplied && startedAt != null) {
            withFrameNanos { }
            telemetry.recordDuration("menu_transition_${screen.name.lowercase()}_ms", startedAt)
            onRecorded()
        }
    }
}

@Composable
internal fun AutoDismissBannerEffect(
    banner: String?,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(banner) {
        val visibleMessage = banner ?: return@LaunchedEffect
        delay(3600)
        if (banner == visibleMessage) {
            onDismiss()
        }
    }
}
