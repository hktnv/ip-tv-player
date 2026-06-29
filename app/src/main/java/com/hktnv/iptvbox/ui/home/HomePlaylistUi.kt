package com.hktnv.iptvbox.ui.home
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.filled.Home as HomeIcon
import androidx.compose.material.icons.filled.Search as SearchIcon
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.designsystem.IptvTheme
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentHint
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.core.player.MediaPlayerFactory
import com.hktnv.iptvbox.core.security.SecretRedactor
import com.hktnv.iptvbox.data.playlist.CreatePlaylistSourceRequest
import com.hktnv.iptvbox.data.playlist.RemotePlaylistLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.HomePreviewLimit
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.LocalPerformanceMode
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.common.LoadingPanel
import com.hktnv.iptvbox.ui.common.ScreenHeader
import com.hktnv.iptvbox.ui.media.seriesPreview
import com.hktnv.iptvbox.ui.playlist.PlaylistRow

@Composable
internal fun HomeScreen(
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    playlists: List<LoadedPlaylist>,
    favoriteCount: Int,
    recentCount: Int,
    favoriteItems: List<CatalogItem>,
    recentItems: List<CatalogItem>,
    favoriteIds: List<String>,
    onAddPlaylist: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenCatalogTab: (CatalogTab) -> Unit,
    onOpenLatest: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenSeries: (String) -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onShowItemOptions: (CatalogItem) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onRequestSideMenu: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
) {
    val performanceMode = LocalPerformanceMode.current
    val previewLimit = performanceMode.homePreviewLimit
    val livePreview = remember(snapshot, previewLimit) {
        snapshot?.items(CatalogTab.LIVE).orEmpty().take(previewLimit)
    }
    val moviePreview = remember(snapshot, previewLimit) {
        snapshot?.items(CatalogTab.MOVIES).orEmpty().take(previewLimit)
    }
    val seriesPreview = remember(snapshot, previewLimit) {
        snapshot?.seriesGroupsAll.orEmpty().take(previewLimit)
    }
    val latestPreview = remember(snapshot, previewLimit) {
        snapshot?.allItems
            .orEmpty()
            .toList()
            .takeLast(previewLimit)
            .asReversed()
    }
    val fallbackRecentRailFocus = remember { FocusRequester() }
    val recentRailFocus = initialFocusRequester ?: fallbackRecentRailFocus
    val favoritesRailFocus = remember { FocusRequester() }
    val latestRailFocus = remember { FocusRequester() }
    val liveRailFocus = remember { FocusRequester() }
    val seriesRailFocus = remember { FocusRequester() }
    val moviesRailFocus = remember { FocusRequester() }
    val showFavoritesRail = favoriteItems.isNotEmpty()
    val afterRecentFocus = if (showFavoritesRail) favoritesRailFocus else latestRailFocus
    val favoriteIdSet = remember(favoriteIds.joinToString("|")) { favoriteIds.toSet() }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
        contentPadding = PaddingValues(top = 16.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (playlist == null) {
            item(key = "home-empty", contentType = "empty") {
                EmptyState(
                    title = "Oynatma listenizi ekleyin",
                    body = "Canlı TV, filmler ve diziler için M3U, JSON veya Xtream listenizi bağlayın.",
                    actionLabel = "Liste Ekle",
                    onAction = onAddPlaylist,
                )
            }
        } else {
            if (catalogIndexLoading || snapshot == null) {
                item(key = "catalog-loading", contentType = "loading") {
                    LoadingPanel("Katalog hazırlanıyor")
                }
            }
            item(key = "recent-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Son İzlenenler",
                    items = recentItems.take(previewLimit),
                    emptyText = "Henüz izlenen içerik yok",
                    onOpenAll = onOpenRecent,
                    onOpenItem = onOpenItem,
                    onShowItemOptions = onShowItemOptions,
                    headerFocusRequester = recentRailFocus,
                    nextRailFocusRequester = afterRecentFocus,
                    onRequestSideMenu = onRequestSideMenu,
                    favoriteIds = favoriteIdSet,
                    cardRatio = 0.78f,
                )
            }
            if (showFavoritesRail) {
                item(key = "favorites-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Favoriler",
                    items = favoriteItems.take(previewLimit),
                    emptyText = "Favori içerik yok",
                    onOpenAll = onOpenFavorites,
                    onOpenItem = onOpenItem,
                    onShowItemOptions = onShowItemOptions,
                    headerFocusRequester = favoritesRailFocus,
                    nextRailFocusRequester = latestRailFocus,
                    onRequestSideMenu = onRequestSideMenu,
                    favoriteIds = favoriteIdSet,
                )
                }
            }
            item(key = "latest-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Son Eklenenler",
                    items = latestPreview,
                    emptyText = "Son eklenen içerik yok",
                    onOpenAll = onOpenLatest,
                    onOpenItem = onOpenItem,
                    onShowItemOptions = onShowItemOptions,
                    headerFocusRequester = latestRailFocus,
                    nextRailFocusRequester = moviesRailFocus,
                    onRequestSideMenu = onRequestSideMenu,
                    favoriteIds = favoriteIdSet,
                )
            }
            item(key = "movie-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Filmler",
                    items = moviePreview,
                    emptyText = "Film bulunamadı",
                    onOpenAll = { onOpenCatalogTab(CatalogTab.MOVIES) },
                    onOpenItem = onOpenItem,
                    onShowItemOptions = onShowItemOptions,
                    headerFocusRequester = moviesRailFocus,
                    nextRailFocusRequester = liveRailFocus,
                    onRequestSideMenu = onRequestSideMenu,
                    favoriteIds = favoriteIdSet,
                )
            }
            item(key = "live-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Canlı TV",
                    items = livePreview,
                    emptyText = "Canlı kanal bulunamadı",
                    onOpenAll = { onOpenCatalogTab(CatalogTab.LIVE) },
                    onOpenItem = onOpenItem,
                    onShowItemOptions = onShowItemOptions,
                    headerFocusRequester = liveRailFocus,
                    nextRailFocusRequester = seriesRailFocus,
                    onRequestSideMenu = onRequestSideMenu,
                    favoriteIds = favoriteIdSet,
                )
            }
            item(key = "series-row", contentType = "series-row") {
                HomeSeriesRow(
                    title = "Diziler",
                    groups = seriesPreview,
                    emptyText = "Dizi bulunamadı",
                    onOpenAll = { onOpenCatalogTab(CatalogTab.SERIES) },
                    onOpenSeries = onOpenSeries,
                    headerFocusRequester = seriesRailFocus,
                    nextRailFocusRequester = null,
                    onRequestSideMenu = onRequestSideMenu,
                )
            }
        }
    }
}
@Composable
internal fun PlaylistScreen(
    playlists: List<LoadedPlaylist>,
    selectedPlaylistId: String?,
    onAddPlaylist: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onReload: (LoadedPlaylist) -> Unit,
    onRename: (LoadedPlaylist) -> Unit,
    contentPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
        contentPadding = PaddingValues(top = 16.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = "Oynatma Listeleri",
                subtitle = "${playlists.size} liste",
                actionLabel = if (playlists.isEmpty()) null else "Liste Ekle",
                onAction = if (playlists.isEmpty()) null else onAddPlaylist,
            )
        }
        if (playlists.isEmpty()) {
            item {
                EmptyState(
                    title = "Oynatma listesi yok",
                    body = "Bir liste ekleyin; hazır olduğunda katalog otomatik açılır.",
                    actionLabel = "Liste Ekle",
                    onAction = onAddPlaylist,
                )
            }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    selected = playlist.id == selectedPlaylistId,
                    onClick = { onSelectPlaylist(playlist.id) },
                    onReload = { onReload(playlist) },
                    onRename = { onRename(playlist) },
                )
            }
        }
    }
}
