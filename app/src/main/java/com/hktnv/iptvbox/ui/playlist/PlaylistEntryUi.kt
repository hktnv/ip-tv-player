package com.hktnv.iptvbox.ui.playlist
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.ui.catalog.tint
import com.hktnv.iptvbox.ui.common.ScreenHeader
import com.hktnv.iptvbox.ui.common.SectionTitle
import com.hktnv.iptvbox.ui.media.catalogSummary

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
    if (playlists.isEmpty()) {
        EmptyPlaylistEntryScene(
            contentPadding = contentPadding,
            onAddPlaylist = onAddPlaylist,
        )
        return
    }

    var showPlaylistList by rememberSaveable(playlists.size) { mutableStateOf(true) }
    val lastPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId } ?: playlists.first()
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
                subtitle = "Devam edin, listelerinizi yönetin veya yeni liste ekleyin",
                actionLabel = null,
                onAction = null,
            )
        }
        item(key = "entry-actions", contentType = "entry-actions") {
            PlaylistEntryActions(
                lastPlaylistTitle = lastPlaylist.name,
                lastPlaylistSubtitle = lastPlaylist.catalogSummary(),
                showPlaylistList = showPlaylistList,
                onOpenLastPlaylist = onOpenLastPlaylist,
                onAddPlaylist = onAddPlaylist,
                onTogglePlaylistList = { showPlaylistList = !showPlaylistList },
                onOpenSettings = onOpenSettings,
            )
        }
        if (showPlaylistList) {
            item(key = "entry-list-title", contentType = "section-title") {
                SectionTitle("Listelerim")
            }
            items(playlists, key = { it.id }, contentType = { "playlist-row" }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    selected = playlist.id == lastPlaylist.id,
                    onClick = { onSelectPlaylist(playlist.id) },
                    onReload = null,
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaylistEntryScene(
    contentPadding: Dp,
    onAddPlaylist: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        val panelModifier = if (maxWidth >= 768.dp) {
            Modifier.width(720.dp)
        } else {
            Modifier.fillMaxWidth()
        }
        Surface(
            color = Color(0xFF101923),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFF283747)),
            shadowElevation = 18.dp,
            modifier = panelModifier,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    color = Color(0xFF162432),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF31495F)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = IptvColors.TextPrimary,
                        modifier = Modifier
                            .padding(20.dp)
                            .size(48.dp),
                    )
                }
                Text(
                    text = "İlk listenizi ekleyin",
                    color = IptvColors.TextPrimary,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "M3U, JSON veya Xtream listenizi bağlayın; uygulama içerikleri düzenli bir kataloğa dönüştürsün.",
                    color = IptvColors.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                EmptyPrimaryAction(
                    text = "İlk Listeyi Ekle",
                    onClick = onAddPlaylist,
                    modifier = Modifier.widthIn(min = 260.dp, max = 420.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaylistEntryActions(
    lastPlaylistTitle: String,
    lastPlaylistSubtitle: String,
    showPlaylistList: Boolean,
    onOpenLastPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onTogglePlaylistList: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints {
        val continueAction = EntryAction(
            title = "Devam Et",
            subtitle = "$lastPlaylistTitle · $lastPlaylistSubtitle",
            icon = Icons.Filled.Home,
            selected = false,
            emphasis = EntryEmphasis.Primary,
            onClick = onOpenLastPlaylist,
        )
        val listAction = EntryAction(
            title = "Listelerim",
            subtitle = if (showPlaylistList) "Kayıtlı listeler açık" else "Kayıtlı listeleri göster",
            icon = Icons.Filled.Search,
            selected = showPlaylistList,
            emphasis = EntryEmphasis.Secondary,
            onClick = onTogglePlaylistList,
        )
        val addAction = EntryAction(
            title = "+ Ekle",
            subtitle = "Yeni liste bağla",
            icon = Icons.AutoMirrored.Filled.List,
            selected = false,
            emphasis = EntryEmphasis.Secondary,
            onClick = onAddPlaylist,
        )
        val settingsAction = EntryAction(
            title = "Ayarlar",
            subtitle = "Tanılama ve tercihler",
            icon = Icons.Filled.Settings,
            selected = false,
            emphasis = EntryEmphasis.Tertiary,
            onClick = onOpenSettings,
        )

        when {
            maxWidth >= 1280.dp -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EntryActionCard(
                        action = continueAction,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(226.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            EntryActionCard(
                                action = listAction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(106.dp),
                            )
                            EntryActionCard(
                                action = addAction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(106.dp),
                            )
                        }
                        EntryActionCard(
                            action = settingsAction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(106.dp),
                        )
                    }
                }
            }
            maxWidth >= 768.dp -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EntryActionCard(
                        action = continueAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(182.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        listOf(listAction, addAction, settingsAction).forEach { action ->
                            EntryActionCard(
                                action = action,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(118.dp),
                            )
                        }
                    }
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EntryActionCard(
                        action = continueAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp),
                    )
                    listOf(listAction, addAction, settingsAction).forEach { action ->
                        EntryActionCard(
                            action = action,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp),
                        )
                    }
                }
            }
        }
    }
}
