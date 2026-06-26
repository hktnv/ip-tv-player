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
import androidx.compose.ui.zIndex
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
internal fun CatalogScreen(
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    selectedTab: CatalogTab,
    selectedCategory: String?,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
    favoriteIds: List<String>,
    onTabSelected: (CatalogTab) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    contentPadding: Dp,
) {
    if (playlist == null) {
        EmptyCatalog(onAddPlaylist, contentPadding)
        return
    }

    val categories = remember(snapshot, selectedTab) { snapshot?.categories(selectedTab).orEmpty() }
    val visibleItems = remember(snapshot, selectedTab, selectedCategory) {
        snapshot?.visibleItems(selectedTab, selectedCategory).orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        CategoryStrip(
            categories = categories,
            selected = selectedCategory,
            onSelected = onCategorySelected,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (playlist.warnings.isNotEmpty()) {
            WarningText(playlist.warnings.first())
        }
        if (snapshot == null || catalogIndexLoading) {
            LoadingPanel(
                text = "Katalog hazırlanıyor",
                modifier = Modifier.padding(top = 18.dp),
            )
        } else if (selectedTab == CatalogTab.SERIES) {
            SeriesCatalogContent(
                snapshot = snapshot,
                selectedCategory = selectedCategory,
                selectedSeriesTitle = selectedSeriesTitle,
                selectedSeasonNumber = selectedSeasonNumber,
                favoriteIds = favoriteIds,
                onSeriesSelected = onSeriesSelected,
                onSeasonSelected = onSeasonSelected,
                onOpenItem = onOpenItem,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f),
            )
        } else if (visibleItems.isEmpty()) {
            EmptyState(
                title = selectedTab.emptyLabel,
                body = "Başka kategori seç veya farklı bir oynatma listesi yükle.",
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = visibleItems,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f),
            )
        }
    }
}


@Composable
internal fun ContentTabs(
    stats: PlaylistStats,
    selectedTab: CatalogTab,
    onSelected: (CatalogTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CatalogTab.entries.forEach { tab ->
            MainSectionButton(
                label = tab.label,
                count = stats.count(tab),
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun MainSectionButton(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .height(50.dp)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.02f, liftPx = -4f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> TvSelectedPanel
            else -> Color(0xFF101720)
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            when {
                focused -> TvFocusBorder
                selected -> IptvColors.Accent
                else -> TvRestingBorder
            },
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = if (selected) IptvColors.Accent else IptvColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = count.toString(),
                color = if (selected) IptvColors.TextPrimary else IptvColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun CategoryStrip(
    categories: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowState = rememberLazyListState()
    LaunchedEffect(categories, selected) {
        val index = if (selected == null) 0 else categories.indexOf(selected).takeIf { it >= 0 }?.plus(1) ?: 0
        rowState.scrollToItem(index)
    }
    LazyRow(
        modifier = modifier.padding(bottom = 6.dp),
        state = rowState,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        item {
            CategoryButton(
                label = "Tüm kategoriler",
                selected = selected == null,
                onClick = { onSelected(null) },
            )
        }
        items(categories, key = { it }) { category ->
            CategoryButton(
                label = category,
                selected = selected == category,
                onClick = { onSelected(category) },
            )
        }
    }
}

@Composable
internal fun CategoryButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .height(34.dp)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> TvSelectedPanel
            else -> Color(0xFF101720)
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            when {
                focused -> TvFocusBorder
                selected -> IptvColors.Accent
                else -> TvRestingBorder
            },
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 10.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) IptvColors.TextPrimary else IptvColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
