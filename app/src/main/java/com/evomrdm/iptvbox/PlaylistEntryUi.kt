package com.evomrdm.iptvbox

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

@Composable
private fun EmptyPrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, tween(150), label = "emptyActionScale")
    val elevation by animateDpAsState(if (focused) 18.dp else 6.dp, tween(150), label = "emptyActionElevation")
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
    Surface(
        modifier = modifier
            .height(62.dp)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (focused) -5f else 0f
            }
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = if (focused) Color(0xFF16E3B3) else Color(0xFF00C795),
        contentColor = IptvColors.Night,
        shape = RoundedCornerShape(18.dp),
        border = if (focused) BorderStroke(2.dp, Color.White) else null,
        shadowElevation = elevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EntryActionCard(
    action: EntryAction,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "entryCardScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (focused) 22.dp else if (action.emphasis == EntryEmphasis.Primary) 8.dp else 2.dp,
        animationSpec = tween(durationMillis = 150),
        label = "entryCardElevation",
    )
    val borderColor = when {
        focused -> Color(0xFFB9D8FF)
        action.selected -> Color(0xFF78AFFF)
        action.emphasis == EntryEmphasis.Primary -> Color(0xFF37536D)
        else -> Color(0xFF263240)
    }
    Surface(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (focused) -8f else 0f
            }
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = action.onClick),
        color = when {
            focused -> Color(0xFF17283B)
            action.selected -> Color(0xFF132538)
            action.emphasis == EntryEmphasis.Primary -> Color(0xFF101C28)
            else -> IptvColors.Panel
        },
        contentColor = IptvColors.TextPrimary,
        shape = RoundedCornerShape(if (action.emphasis == EntryEmphasis.Primary) 22.dp else 16.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
        shadowElevation = elevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .defaultMinSize(minHeight = 104.dp)
                .padding(if (action.emphasis == EntryEmphasis.Primary) 22.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.height(if (action.emphasis == EntryEmphasis.Primary) 30.dp else 22.dp),
                tint = when {
                    focused -> Color(0xFFB9D8FF)
                    action.selected -> Color(0xFF78AFFF)
                    else -> IptvColors.TextPrimary
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = action.title,
                    fontSize = if (action.emphasis == EntryEmphasis.Primary) 26.sp else 18.sp,
                    lineHeight = if (action.emphasis == EntryEmphasis.Primary) 31.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (action.emphasis == EntryEmphasis.Primary) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = action.subtitle,
                    color = IptvColors.TextSecondary.copy(alpha = 0.94f),
                    fontSize = if (action.emphasis == EntryEmphasis.Primary) 15.sp else 13.sp,
                    lineHeight = if (action.emphasis == EntryEmphasis.Primary) 20.sp else 17.sp,
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
    val selected: Boolean,
    val emphasis: EntryEmphasis,
    val onClick: () -> Unit,
)

private enum class EntryEmphasis {
    Primary,
    Secondary,
    Tertiary,
}
