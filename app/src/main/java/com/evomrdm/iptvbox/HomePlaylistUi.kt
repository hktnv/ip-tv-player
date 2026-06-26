package com.evomrdm.iptvbox

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.evomrdm.iptvbox.core.common.SearchNormalizer
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import com.evomrdm.iptvbox.core.designsystem.IptvTheme
import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.core.model.ContentHint
import com.evomrdm.iptvbox.core.model.ContentKind
import com.evomrdm.iptvbox.core.model.PlaylistSourceType
import com.evomrdm.iptvbox.core.player.MediaPlayerFactory
import com.evomrdm.iptvbox.core.security.SecretRedactor
import com.evomrdm.iptvbox.data.playlist.CreatePlaylistSourceRequest
import com.evomrdm.iptvbox.data.playlist.RemotePlaylistLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

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
    onAddPlaylist: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenCatalogTab: (CatalogTab) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenSeries: (String) -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    contentPadding: Dp,
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
                )
            }
            item(key = "favorites-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Favoriler",
                    items = favoriteItems.take(previewLimit),
                    emptyText = "Favori içerik yok",
                    onOpenAll = onOpenFavorites,
                    onOpenItem = onOpenItem,
                )
            }
            item(key = "latest-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Son Eklenenler",
                    items = latestPreview,
                    emptyText = "Son eklenen içerik yok",
                    onOpenAll = { onOpenCatalogTab(firstAvailableTab(playlist)) },
                    onOpenItem = onOpenItem,
                )
            }
            item(key = "live-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Canlı TV",
                    items = livePreview,
                    emptyText = "Canlı kanal bulunamadı",
                    onOpenAll = { onOpenCatalogTab(CatalogTab.LIVE) },
                    onOpenItem = onOpenItem,
                )
            }
            item(key = "series-row", contentType = "series-row") {
                HomeSeriesRow(
                    title = "Diziler",
                    groups = seriesPreview,
                    emptyText = "Dizi bulunamadı",
                    onOpenAll = { onOpenCatalogTab(CatalogTab.SERIES) },
                    onOpenSeries = onOpenSeries,
                )
            }
            item(key = "movie-row", contentType = "media-row") {
                HomeContentRow(
                    title = "Filmler",
                    items = moviePreview,
                    emptyText = "Film bulunamadı",
                    onOpenAll = { onOpenCatalogTab(CatalogTab.MOVIES) },
                    onOpenItem = onOpenItem,
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

@Composable
internal fun HomeContentRow(
    title: String,
    items: List<CatalogItem>,
    emptyText: String,
    onOpenAll: () -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(title)
            SeeAllButton(onClick = onOpenAll)
        }
        if (items.isEmpty()) {
            Text(emptyText, color = IptvColors.TextSecondary, fontSize = 13.sp)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(items, key = { it.id }, contentType = { it.kind.name }) { item ->
                    CompactContentCard(
                        item = item,
                        onClick = { onOpenItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeSeriesRow(
    title: String,
    groups: List<SeriesGroup>,
    emptyText: String,
    onOpenAll: () -> Unit,
    onOpenSeries: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(title)
            SeeAllButton(onClick = onOpenAll)
        }
        if (groups.isEmpty()) {
            Text(emptyText, color = IptvColors.TextSecondary, fontSize = 13.sp)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(groups, key = { it.id }, contentType = { "series-card" }) { group ->
                    SeriesGroupCard(
                        group = group,
                        onClick = { onOpenSeries(group.title) },
                        modifier = Modifier.width(132.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SeeAllButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .tvFocusLift(focused = focused, scale = 1.035f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = if (focused) TvFocusPanel else Color(0xFF101720),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 10.dp),
    ) {
        Text(
            text = "Tümü",
            color = if (focused) IptvColors.TextPrimary else IptvColors.Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1,
        )
    }
}


