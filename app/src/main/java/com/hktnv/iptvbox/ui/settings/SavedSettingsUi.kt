package com.hktnv.iptvbox.ui.settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.core.model.CatalogItem
import kotlinx.coroutines.launch
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.ScreenBottomPadding
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
    onToggleFavorite: (CatalogItem) -> Unit,
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
            LoadingPanel("Katalog hazırlanıyor", Modifier.padding(top = 18.dp))
        } else if (items.isEmpty()) {
            EmptyState(
                title = emptyText,
                body = "Katalogdaki içerikleri seçtikçe bu ekran otomatik dolar.",
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onToggleFavorite = onToggleFavorite,
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
    onToggleFavorite: (CatalogItem) -> Unit,
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
            LoadingPanel("Katalog hazırlanıyor", Modifier.padding(top = 18.dp))
        } else if (items.isEmpty()) {
            EmptyState(
                title = "Son eklenen içerik yok",
                body = "Oynatma listesi hazır olduğunda yeni içerikler burada görünür.",
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onToggleFavorite = onToggleFavorite,
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
    onReload: () -> Unit,
    onAddPlaylist: () -> Unit,
    onOpenPlaylistEntry: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val startFocusRequester = initialFocusRequester ?: remember { FocusRequester() }
    val privacyFocusRequester = remember { FocusRequester() }
    val diagnosticsFocusRequester = remember { FocusRequester() }
    val playlistFocusRequester = remember { FocusRequester() }
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
                    title = "Başlangıç",
                    body = "Son oynatma listesi, favoriler ve son izlenenler bu cihazda hatırlanır.",
                    actionLabel = "Liste seçimine dön",
                    onAction = onOpenPlaylistEntry,
                )
            }
        }
        item {
            SettingsFocusPanel(
                focusRequester = privacyFocusRequester,
                previousFocusRequester = startFocusRequester,
                nextFocusRequester = diagnosticsFocusRequester,
                onFocused = { scrollTo(1) },
                onRequestSideMenu = onRequestSideMenu,
            ) {
                InfoPanelContent(
                    title = "Gizlilik",
                    body = "Oynatma listesi bilgileri yalnızca bu cihazda saklanır.",
                )
            }
        }
        item {
            SettingsFocusPanel(
                focusRequester = diagnosticsFocusRequester,
                previousFocusRequester = privacyFocusRequester,
                nextFocusRequester = playlistFocusRequester,
                onFocused = { scrollTo(2) },
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
                onFocused = { scrollTo(3) },
                onRequestSideMenu = onRequestSideMenu,
            ) {
                if (playlist == null) {
                    InfoPanelContent(
                        title = "Oynatma listesi yok",
                        body = "Liste eklediğinizde yenileme ve katalog bilgileri burada görünür.",
                        actionLabel = "Liste Ekle",
                        onAction = onAddPlaylist,
                    )
                } else {
                    InfoPanelContent(
                        title = playlist.name,
                        body = playlist.catalogSummary(),
                        actionLabel = "Yenile",
                        onAction = onReload,
                    )
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = IptvColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(body, color = IptvColors.TextSecondary, fontSize = 14.sp, lineHeight = 19.sp)
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction, shape = RoundedCornerShape(8.dp)) {
                Text(actionLabel)
            }
        }
    }
}
