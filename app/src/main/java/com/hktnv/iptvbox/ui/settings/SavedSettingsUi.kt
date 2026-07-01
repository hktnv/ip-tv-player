package com.hktnv.iptvbox.ui.settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.model.CatalogItem
import kotlinx.coroutines.launch
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.PlaylistImportProgress
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.player.PlayerUiMode
import com.hktnv.iptvbox.state.contentProgressLabel
import com.hktnv.iptvbox.telemetry.PerformanceDiagnostics
import com.hktnv.iptvbox.ui.common.EmptyCatalog
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.common.LoadingPanel
import com.hktnv.iptvbox.ui.media.catalogSummary
import com.hktnv.iptvbox.ui.media.ContentGrid
import com.hktnv.iptvbox.ui.media.itemsByIds

@Composable
internal fun SavedItemsScreen(
    title: String,
    emptyText: String,
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    itemIds: List<String>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    if (playlist == null) {
        EmptyCatalog(onAddPlaylist, contentPadding)
        return
    }
    val idSignature = itemIds.joinToString("|")
    val items = remember(snapshot, idSignature) { snapshot?.itemsByIds(itemIds).orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        if (snapshot == null || catalogIndexLoading) {
            LoadingPanel(stringResource(R.string.catalog_preparing), Modifier.padding(top = 18.dp))
        } else if (items.isEmpty()) {
            EmptyState(
                title = emptyText,
                body = stringResource(R.string.empty_saved_body),
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                modifier = Modifier.weight(1f),
                requestInitialFocus = initialFocusRequester != null,
                initialFocusRequester = initialFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
            )
        }
    }
}

@Composable
internal fun LatestItemsScreen(
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    if (playlist == null) {
        EmptyCatalog(onAddPlaylist, contentPadding)
        return
    }
    val items = remember(snapshot) {
        snapshot?.allItems
            .orEmpty()
            .toList()
            .asReversed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        if (snapshot == null || catalogIndexLoading) {
            LoadingPanel(stringResource(R.string.catalog_preparing), Modifier.padding(top = 18.dp))
        } else if (items.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_latest_title),
                body = stringResource(R.string.empty_latest_body),
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onShowItemOptions = onShowItemOptions,
                modifier = Modifier.weight(1f),
                requestInitialFocus = true,
                initialFocusRequester = initialFocusRequester,
                onRequestSideMenu = onRequestSideMenu,
            )
        }
    }
}

@Composable
internal fun SettingsScreen(
    playlist: LoadedPlaylist?,
    diagnostics: PerformanceDiagnostics,
    playerUiMode: PlayerUiMode,
    onPlayerUiModeChange: (PlayerUiMode) -> Unit,
    onReload: () -> Unit,
    onAddPlaylist: () -> Unit,
    onOpenPlaylistEntry: () -> Unit,
    refreshProgress: PlaylistImportProgress?,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val startFocusRequester = initialFocusRequester ?: remember { FocusRequester() }
    val privacyFocusRequester = remember { FocusRequester() }
    val playerUiFocusRequester = remember { FocusRequester() }
    val diagnosticsFocusRequester = remember { FocusRequester() }
    val playlistFocusRequester = remember { FocusRequester() }
    val playlistRefreshProgress = playlist?.let { current ->
        refreshProgress?.takeIf { it.playlistId == current.id }
    }
    fun scrollTo(index: Int) {
        scope.launch { listState.animateScrollToItem(index) }
    }
    LaunchedEffect(initialFocusRequester) {
        if (initialFocusRequester != null) {
            withFrameNanos { }
            runCatching { startFocusRequester.requestFocus() }
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
        contentPadding = PaddingValues(top = 16.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SettingsFocusPanel(
                focusRequester = startFocusRequester,
                previousFocusRequester = null,
                nextFocusRequester = privacyFocusRequester,
                onFocused = { scrollTo(0) },
                onRequestSideMenu = onRequestSideMenu,
            ) {
                InfoPanelContent(
                    title = stringResource(R.string.settings_start_title),
                    body = stringResource(R.string.settings_start_body),
                    actionLabel = stringResource(R.string.action_return_playlist_entry),
                    onAction = onOpenPlaylistEntry,
                )
            }
        }
        item {
            SettingsFocusPanel(
                focusRequester = privacyFocusRequester,
                previousFocusRequester = startFocusRequester,
                nextFocusRequester = playerUiFocusRequester,
                onFocused = { scrollTo(1) },
                onRequestSideMenu = onRequestSideMenu,
            ) {
                InfoPanelContent(
                    title = stringResource(R.string.settings_privacy_title),
                    body = stringResource(R.string.settings_privacy_body),
                )
            }
        }
        item {
            SettingsFocusPanel(
                focusRequester = playerUiFocusRequester,
                previousFocusRequester = privacyFocusRequester,
                nextFocusRequester = diagnosticsFocusRequester,
                onFocused = { scrollTo(2) },
                onConfirm = { onPlayerUiModeChange(playerUiMode.next()) },
                onRequestSideMenu = onRequestSideMenu,
            ) {
                PlayerUiModePanelContent(
                    selectedMode = playerUiMode,
                    onModeSelected = onPlayerUiModeChange,
                )
            }
        }
        item {
            SettingsFocusPanel(
                focusRequester = diagnosticsFocusRequester,
                previousFocusRequester = playerUiFocusRequester,
                nextFocusRequester = playlistFocusRequester,
                onFocused = { scrollTo(3) },
                onRequestSideMenu = onRequestSideMenu,
            ) {
                DiagnosticsPanelContent(diagnostics = diagnostics, playlist = playlist)
            }
        }
        item {
            SettingsFocusPanel(
                focusRequester = playlistFocusRequester,
                previousFocusRequester = diagnosticsFocusRequester,
                nextFocusRequester = null,
                onFocused = { scrollTo(4) },
                onRequestSideMenu = onRequestSideMenu,
            ) {
                if (playlist == null) {
                    InfoPanelContent(
                        title = stringResource(R.string.playlist_settings_title),
                        body = stringResource(R.string.playlist_settings_body),
                        actionLabel = stringResource(R.string.action_add_playlist),
                        onAction = onAddPlaylist,
                    )
                } else {
                    InfoPanelContent(
                        title = playlist.name,
                        body = playlist.catalogSummary(),
                        actionLabel = if (playlistRefreshProgress?.active == true) {
                            stringResource(R.string.action_refreshing)
                        } else {
                            stringResource(R.string.action_refresh)
                        },
                        onAction = onReload,
                        actionEnabled = playlistRefreshProgress?.active != true,
                    ) {
                        playlistRefreshProgress?.let { SettingsPlaylistProgress(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPanelContent(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 19.sp)
        extraContent()
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction, enabled = actionEnabled, shape = RoundedCornerShape(8.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SettingsPlaylistProgress(progress: PlaylistImportProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = progress.error ?: progress.message,
            color = if (progress.error == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (progress.error == null) {
            LinearProgressIndicator(
                progress = {
                    val total = progress.totalItems ?: 0
                    if (total > 0) {
                        (progress.processedItems.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = progress.error ?: contentProgressLabel(progress.processedItems, progress.totalItems),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
    }
}
