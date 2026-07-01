package com.hktnv.iptvbox.ui.playlist
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
import androidx.compose.ui.zIndex
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
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.ui.common.PremiumPanel
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.stats

@Composable
internal fun PlaylistSummary(
    playlist: LoadedPlaylist,
    favoriteCount: Int,
    recentCount: Int,
    onOpenCatalog: () -> Unit,
    onOpenCatalogTab: (CatalogTab) -> Unit,
) {
    val stats = remember(playlist.id, playlist.items) { playlist.stats() }
    PremiumPanel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${playlist.type.label()} · $favoriteCount favori · $recentCount son izlenen",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = onOpenCatalog,
                    modifier = Modifier
                        .height(40.dp)
                        .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                ) {
                    Text("Katalog")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip(
                    label = "Canlı TV",
                    value = stats.live,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenCatalogTab(CatalogTab.LIVE) },
                )
                SummaryChip(
                    label = "Filmler",
                    value = stats.movies,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenCatalogTab(CatalogTab.MOVIES) },
                )
                SummaryChip(
                    label = "Diziler",
                    value = stats.series,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenCatalogTab(CatalogTab.SERIES) },
                )
            }
        }
    }
}

@Composable
internal fun SummaryChip(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .height(48.dp)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.02f, liftPx = -4f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
            Text(
                value.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PlaylistRow(
    playlist: LoadedPlaylist,
    selected: Boolean,
    onClick: () -> Unit,
    onReload: (() -> Unit)?,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val stats = remember(playlist.id, playlist.items) { playlist.stats() }
    val totalCount = remember(stats, playlist.items.size) {
        (stats.live + stats.movies + stats.series).takeIf { it > 0 } ?: playlist.items.size
    }
    var focused by remember { mutableStateOf(false) }
    PremiumPanel(
        modifier = Modifier
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.012f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        borderColor = when {
            focused -> TvFocusBorder
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> TvRestingBorder
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$totalCount içerik · ${stats.live} canlı · ${stats.movies} film · ${stats.series} dizi",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(playlist.type.label(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                if (playlist.xtreamApiSupported) {
                    Text(
                        text = "Xtream API Destekli",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
                if (onRename != null) {
                    OutlinedButton(
                        onClick = onRename,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Adı düzenle", fontSize = 12.sp)
                    }
                }
                if (onReload != null) {
                    OutlinedButton(
                        onClick = onReload,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Yenile", fontSize = 12.sp)
                    }
                }
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Sil", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
