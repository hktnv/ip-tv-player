package com.hktnv.iptvbox.ui.playlist
import androidx.compose.material3.MaterialTheme
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.core.designsystem.transparent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvSelectedPanel
import com.hktnv.iptvbox.ui.media.label

@Composable
internal fun TypeSelector(
    type: PlaylistSourceType,
    onTypeChange: (PlaylistSourceType) -> Unit,
) {
    val options = listOf(PlaylistSourceType.M3U_URL, PlaylistSourceType.XTREAM, PlaylistSourceType.JSON_DIRECTORY)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEach { candidate ->
                    SourceTypeSegment(
                        modifier = Modifier.weight(1f),
                        label = candidate.label(),
                        selected = candidate == type,
                        onClick = { onTypeChange(candidate) },
                    )
                }
            }
        }
        Text(
            text = type.typeHelperText(),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.94f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun SourceTypeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> TvSelectedPanel
            else -> MaterialTheme.colorScheme.transparent
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> TvFocusBorder
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.transparent
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (selected || focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun playlistFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TvFocusBorder,
    focusedLabelColor = TvFocusBorder,
    cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceBorder,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

internal fun PlaylistSourceType.typeHelperText(): String {
    return when (this) {
        PlaylistSourceType.JSON_DIRECTORY -> "JSON seçili: katalog adresini girin."
        PlaylistSourceType.M3U_URL -> "M3U seçili: oynatma listesi URL'sini girin."
        PlaylistSourceType.XTREAM -> "Xtream seçili: sunucu, kullanıcı adı ve parola gerekir."
    }
}

internal fun PlaylistSourceType.endpointHelperText(): String {
    return when (this) {
        PlaylistSourceType.JSON_DIRECTORY -> "JSON dosya veya servis adresini yazın."
        PlaylistSourceType.M3U_URL -> "M3U veya m3u_plus liste adresini yazın."
        PlaylistSourceType.XTREAM -> "Sunucu kök adresini yazın; kullanıcı adı ve parola aşağıda girilir."
    }
}

@Composable
internal fun RenamePlaylistDialog(
    playlist: LoadedPlaylist,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(playlist.id) { mutableStateOf(playlist.name) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Oynatma Listesi Adı", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Liste adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Vazgeç")
                    }
                    Button(
                        onClick = { onSave(name) },
                        modifier = Modifier.weight(1.2f),
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}
