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
internal fun BootScreen(contentPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = contentPadding),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "IP TV Player",
            color = IptvColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Açılıyor",
            color = IptvColors.TextSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
internal fun RecoveryScreen(
    message: String,
    hasPlaylist: Boolean,
    contentPadding: Dp,
    onContinue: () -> Unit,
    onReload: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = contentPadding),
        verticalArrangement = Arrangement.Center,
    ) {
        PremiumPanel {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Liste yeniden yüklenebilir",
                    color = IptvColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message,
                    color = IptvColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Devam et", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onReload,
                        enabled = hasPlaylist,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Listeyi yenile", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Sorunlu listeyi kaldır", maxLines = 1)
                    }
                }
            }
        }
    }
}


@Composable
internal fun ScreenHeader(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 520.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderTexts(title, subtitle)
                if (actionLabel != null && onAction != null) {
                    Button(
                        onClick = onAction,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text(actionLabel, maxLines = 1)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderTexts(title, subtitle, Modifier.weight(1f))
                if (actionLabel != null && onAction != null) {
                    Button(
                        onClick = onAction,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text(actionLabel, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HeaderTexts(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = IptvColors.TextPrimary,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            color = IptvColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun EmptyCatalog(onAddPlaylist: () -> Unit, contentPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding, vertical = 16.dp),
    ) {
        ScreenHeader(
            title = "Katalog",
            subtitle = "İçerik için önce oynatma listesi ekleyin",
            actionLabel = null,
            onAction = null,
        )
        EmptyState(
            title = "Katalog hazır değil",
            body = "Liste eklediğinizde canlı TV, filmler ve diziler burada düzenli şekilde görünür.",
            actionLabel = "Liste Ekle",
            onAction = onAddPlaylist,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

@Composable
internal fun EmptyState(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (actionLabel != null && onAction != null) {
        var focused by remember { mutableStateOf(false) }
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .tvClickable(onClick = onAction),
            color = if (focused) Color(0xFF142235) else IptvColors.Panel,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFB9D8FF) else Color(0xFF263240)),
        ) {
            Column(
                modifier = Modifier
                    .defaultMinSize(minHeight = 156.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    title,
                    color = IptvColors.TextPrimary,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    body,
                    color = IptvColors.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                )
                FocusAwarePrimaryLabel(
                    text = actionLabel,
                    focused = focused,
                )
            }
        }
        return
    }

    PremiumPanel(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.defaultMinSize(minHeight = 156.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                title,
                color = IptvColors.TextPrimary,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                body,
                color = IptvColors.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
internal fun FocusAwarePrimaryLabel(
    text: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.defaultMinSize(minWidth = 148.dp),
        color = if (focused) IptvColors.Accent else Color(0xFF00C795),
        shape = RoundedCornerShape(10.dp),
        border = if (focused) BorderStroke(2.dp, Color.White) else null,
    ) {
        Text(
            text = text,
            color = if (focused) Color.White else IptvColors.Night,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
        )
    }
}

@Composable
internal fun LoadingPanel(
    text: String,
    modifier: Modifier = Modifier,
) {
    PremiumPanel(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = text,
                color = IptvColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun InfoPanel(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    PremiumPanel {
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
}

@Composable
internal fun PremiumPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFF263240),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = IptvColors.Panel,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
internal fun StatusBanner(text: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1B22))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            color = IptvColors.TextPrimary,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text("Kapat")
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        color = IptvColors.TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
internal fun WarningText(text: String) {
    Text(
        text = text,
        color = IptvColors.Warning,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    )
}

@Composable
internal fun ErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
}
