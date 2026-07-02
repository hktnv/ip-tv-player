package com.hktnv.iptvbox.ui.playlist
import androidx.compose.material3.MaterialTheme
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.core.designsystem.transparent
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hktnv.iptvbox.R
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
            text = stringResource(type.typeHelperTextRes()),
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

@StringRes
internal fun PlaylistSourceType.typeHelperTextRes(): Int {
    return when (this) {
        PlaylistSourceType.JSON_DIRECTORY -> R.string.playlist_type_json_helper
        PlaylistSourceType.M3U_URL -> R.string.playlist_type_m3u_helper
        PlaylistSourceType.XTREAM -> R.string.playlist_type_xtream_helper
    }
}

@StringRes
internal fun PlaylistSourceType.endpointHelperTextRes(): Int {
    return when (this) {
        PlaylistSourceType.JSON_DIRECTORY -> R.string.playlist_endpoint_json_helper
        PlaylistSourceType.M3U_URL -> R.string.playlist_endpoint_m3u_helper
        PlaylistSourceType.XTREAM -> R.string.playlist_endpoint_xtream_helper
    }
}

@StringRes
internal fun PlaylistSourceType.endpointLabelRes(): Int {
    return when (this) {
        PlaylistSourceType.JSON_DIRECTORY -> R.string.playlist_add_json_label
        PlaylistSourceType.M3U_URL -> R.string.playlist_add_m3u_label
        PlaylistSourceType.XTREAM -> R.string.playlist_add_xtream_label
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
                Text(
                    text = stringResource(R.string.playlist_rename_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_add_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = { onSave(name) },
                        modifier = Modifier.weight(1.2f),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}
