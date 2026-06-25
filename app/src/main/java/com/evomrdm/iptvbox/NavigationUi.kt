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
import androidx.compose.foundation.focusGroup
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
internal fun SideNavigation(
    selected: AppScreen,
    hasPlaylist: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onAddPlaylist: () -> Unit,
) {
    val entries = navEntries(hasPlaylist)
    Column(
        modifier = Modifier
            .width(176.dp)
            .fillMaxHeight()
            .focusGroup()
            .background(Color(0xFF0C1118), RoundedCornerShape(14.dp))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "IP TV",
                color = IptvColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = screenLabel(selected),
                color = IptvColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            entries.forEach { entry ->
                NavigationButton(
                    label = entry.label,
                    icon = entry.icon,
                    selected = selected == entry.screen,
                    enabled = entry.enabled,
                    onClick = { onNavigate(entry.screen) },
                )
            }
        }
        Text(
            text = if (hasPlaylist) "OK: seç · Geri: dön" else "Listeler ekranından başlayın",
            color = IptvColors.TextSecondary.copy(alpha = 0.82f),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun BottomNavigation(
    selected: AppScreen,
    hasPlaylist: Boolean,
    onNavigate: (AppScreen) -> Unit,
) {
    val entries = listOf(
        NavEntry("Ana", AppScreen.HOME, true, AppScreen.HOME.navIcon()),
        NavEntry("Listeler", AppScreen.PLAYLISTS, true, AppScreen.PLAYLISTS.navIcon()),
        NavEntry("Ara", AppScreen.SEARCH, hasPlaylist, AppScreen.SEARCH.navIcon()),
        NavEntry("Ayarlar", AppScreen.SETTINGS, true, AppScreen.SETTINGS.navIcon()),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = Color(0xF20A0F16),
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entries.forEach { entry ->
                BottomNavItem(
                    label = entry.label,
                    icon = entry.screen.bottomIcon(),
                    selected = selected == entry.screen,
                    enabled = entry.enabled,
                    onClick = { onNavigate(entry.screen) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onClick),
        color = when {
            focused -> Color(0xFF182636)
            selected -> Color(0xFF10221F)
            else -> Color.Transparent
        },
        contentColor = if (selected) IptvColors.Accent else IptvColors.TextPrimary,
        shape = RoundedCornerShape(12.dp),
        border = if (focused || selected) {
            BorderStroke(1.dp, if (focused) TvFocusBorder else IptvColors.Accent)
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.height(17.dp),
                tint = if (!enabled) IptvColors.TextSecondary.copy(alpha = 0.36f) else Color.Unspecified,
            )
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (!enabled) IptvColors.TextSecondary.copy(alpha = 0.36f) else Color.Unspecified,
            )
        }
    }
}

@Composable
internal fun NavigationButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 34.dp else 40.dp)
            .onFocusChanged { focused = it.isFocused },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = when {
                focused -> Color(0xFF1E3345)
                selected -> Color(0xFF15221F)
                else -> Color.Transparent
            },
            contentColor = when {
                focused -> IptvColors.TextPrimary
                selected -> IptvColors.Accent
                else -> IptvColors.TextPrimary
            },
            disabledContentColor = IptvColors.TextSecondary.copy(alpha = 0.72f),
        ),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> TvFocusBorder
                selected -> IptvColors.Accent
                else -> Color(0xFF2A3542)
            },
        ),
        contentPadding = PaddingValues(horizontal = if (compact) 4.dp else 10.dp, vertical = if (compact) 4.dp else 7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.height(16.dp))
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = if (compact) 12.sp else 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}


internal data class NavEntry(
    val label: String,
    val screen: AppScreen,
    val enabled: Boolean,
    val icon: ImageVector,
)

internal fun navEntries(hasPlaylist: Boolean): List<NavEntry> {
    return listOf(
        NavEntry("Ana", AppScreen.HOME, true, AppScreen.HOME.navIcon()),
        NavEntry("Listeler", AppScreen.PLAYLISTS, true, AppScreen.PLAYLISTS.navIcon()),
        NavEntry("Katalog", AppScreen.CATALOG, hasPlaylist, AppScreen.CATALOG.navIcon()),
        NavEntry("Arama", AppScreen.SEARCH, hasPlaylist, AppScreen.SEARCH.navIcon()),
        NavEntry("Favoriler", AppScreen.FAVORITES, hasPlaylist, AppScreen.FAVORITES.navIcon()),
        NavEntry("Son İzlenen", AppScreen.RECENT, hasPlaylist, AppScreen.RECENT.navIcon()),
        NavEntry("Ayarlar", AppScreen.SETTINGS, true, AppScreen.SETTINGS.navIcon()),
    )
}

private fun screenLabel(screen: AppScreen): String {
    return when (screen) {
        AppScreen.HOME -> "Ana sayfa"
        AppScreen.PLAYLISTS -> "Oynatma listeleri"
        AppScreen.CATALOG -> "Katalog"
        AppScreen.SEARCH -> "Arama"
        AppScreen.FAVORITES -> "Favoriler"
        AppScreen.RECENT -> "Son izlenenler"
        AppScreen.SETTINGS -> "Ayarlar"
        AppScreen.PLAYER -> "Oynatıcı"
    }
}

internal fun AppScreen.bottomIcon(): ImageVector {
    return navIcon()
}

private fun AppScreen.navIcon(): ImageVector {
    return when (this) {
        AppScreen.HOME -> Icons.Filled.Home
        AppScreen.PLAYLISTS -> Icons.AutoMirrored.Filled.List
        AppScreen.CATALOG -> Icons.AutoMirrored.Filled.List
        AppScreen.SEARCH -> Icons.Filled.Search
        AppScreen.FAVORITES -> Icons.Filled.Home
        AppScreen.RECENT -> Icons.AutoMirrored.Filled.List
        AppScreen.SETTINGS -> Icons.Filled.Settings
        else -> Icons.Filled.Home
    }
}

private val TvFocusBorder = Color(0xFFB9D8FF)
