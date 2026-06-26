package com.evomrdm.iptvbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evomrdm.iptvbox.core.designsystem.IptvColors

@Composable
internal fun PlaylistEntryScreen(
    playlists: List<LoadedPlaylist>,
    selectedPlaylistId: String?,
    contentPadding: Dp,
    onOpenLastPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showPlaylistList by rememberSaveable(playlists.size) { mutableStateOf(playlists.isNotEmpty()) }
    val lastPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId } ?: playlists.firstOrNull()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = contentPadding),
        contentPadding = PaddingValues(top = 20.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "entry-header", contentType = "header") {
            ScreenHeader(
                title = "Oynatma Listeleri",
                subtitle = "İzlemek istediğiniz listeyi seçin veya yeni liste ekleyin",
                actionLabel = null,
                onAction = null,
            )
        }
        item(key = "entry-actions", contentType = "entry-actions") {
            PlaylistEntryActions(
                hasPlaylist = lastPlaylist != null,
                lastPlaylistTitle = lastPlaylist?.name ?: "Henüz liste yok",
                lastPlaylistSubtitle = lastPlaylist?.catalogSummary() ?: "Önce bir oynatma listesi ekleyin",
                showPlaylistList = showPlaylistList,
                onOpenLastPlaylist = onOpenLastPlaylist,
                onAddPlaylist = onAddPlaylist,
                onTogglePlaylistList = { showPlaylistList = !showPlaylistList },
                onOpenSettings = onOpenSettings,
            )
        }
        if (playlists.isEmpty()) {
            item(key = "entry-empty", contentType = "empty") {
                EmptyState(
                    title = "Oynatma listesi yok",
                    body = "M3U, JSON veya Xtream listenizi ekleyerek izlemeye başlayın.",
                    actionLabel = "Oynatma Listesi Ekle",
                    onAction = onAddPlaylist,
                )
            }
        } else if (showPlaylistList) {
            item(key = "entry-list-title", contentType = "section-title") {
                SectionTitle("Kayıtlı oynatma listeleri")
            }
            items(playlists, key = { it.id }, contentType = { "playlist-row" }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    selected = playlist.id == lastPlaylist?.id,
                    onClick = { onSelectPlaylist(playlist.id) },
                    onReload = null,
                )
            }
        }
    }
}

@Composable
private fun PlaylistEntryActions(
    hasPlaylist: Boolean,
    lastPlaylistTitle: String,
    lastPlaylistSubtitle: String,
    showPlaylistList: Boolean,
    onOpenLastPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onTogglePlaylistList: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints {
        val actions = listOf(
            EntryAction(
                title = "Son Oynatma Listesi",
                subtitle = "$lastPlaylistTitle · $lastPlaylistSubtitle",
                icon = Icons.Filled.Home,
                enabled = hasPlaylist,
                selected = false,
                onClick = onOpenLastPlaylist,
            ),
            EntryAction(
                title = "Oynatma Listesi Ekle",
                subtitle = "Yeni M3U, JSON veya Xtream listesi",
                icon = Icons.AutoMirrored.Filled.List,
                enabled = true,
                selected = false,
                onClick = onAddPlaylist,
            ),
            EntryAction(
                title = "Oynatma Listeleri",
                subtitle = if (showPlaylistList) "Liste bölümü açık" else "Kayıtlı listeleri göster",
                icon = Icons.Filled.Search,
                enabled = true,
                selected = showPlaylistList,
                onClick = onTogglePlaylistList,
            ),
            EntryAction(
                title = "Ayarlar",
                subtitle = "Tanılama ve uygulama tercihleri",
                icon = Icons.Filled.Settings,
                enabled = true,
                selected = false,
                onClick = onOpenSettings,
            ),
        )
        when {
            maxWidth >= 880.dp -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    actions.forEach { action ->
                        EntryActionCard(
                            action = action,
                            modifier = Modifier
                                .weight(1f)
                                .height(166.dp),
                        )
                    }
                }
            }
            maxWidth >= 520.dp -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    actions.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowItems.forEach { action ->
                                EntryActionCard(
                                    action = action,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(150.dp),
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    actions.forEach { action ->
                        EntryActionCard(
                            action = action,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(118.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryActionCard(
    action: EntryAction,
    modifier: Modifier = Modifier,
) {
    var focused by androidx.compose.runtime.remember { mutableStateOf(false) }
    val borderColor = when {
        focused -> Color(0xFFB9D8FF)
        action.selected -> IptvColors.Accent
        else -> Color(0xFF263240)
    }
    Surface(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = action.enabled, onClick = action.onClick),
        color = when {
            focused -> Color(0xFF172538)
            action.selected -> Color(0xFF102720)
            else -> IptvColors.Panel
        },
        contentColor = if (action.enabled) IptvColors.TextPrimary else IptvColors.TextSecondary.copy(alpha = 0.54f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .defaultMinSize(minHeight = 112.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.height(24.dp),
                tint = if (focused || action.selected) IptvColors.Accent else Color.Unspecified,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = action.title,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = action.subtitle,
                    color = IptvColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class EntryAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val selected: Boolean,
    val onClick: () -> Unit,
)
