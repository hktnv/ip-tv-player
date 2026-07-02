package com.hktnv.iptvbox.ui.playlist
import androidx.compose.material3.MaterialTheme
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.model.ContentHint
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.data.playlist.CreatePlaylistSourceRequest
import com.hktnv.iptvbox.data.playlist.PlaylistLoadResult
import com.hktnv.iptvbox.data.playlist.RemotePlaylistLoader
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.DraftLoadState
import com.hktnv.iptvbox.state.contentProgressLabel
import com.hktnv.iptvbox.state.toDraftLoadState
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.ui.common.ErrorText
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.normalizeVisibleUrl
import com.hktnv.iptvbox.ui.media.simpleUserMessage
import com.hktnv.iptvbox.ui.media.validatePlaylistRequest

internal data class DraftPlaylist(
    val id: String,
    val name: String,
    val type: PlaylistSourceType,
    val endpoint: String,
    val headers: Map<String, String>,
    val loadStartedAtMs: Long,
    val firstResponseMs: Long,
    val uiWatchId: Long?,
)

@Composable
internal fun AddPlaylistDialog(
    loader: RemotePlaylistLoader,
    telemetry: AppPerformanceTelemetry,
    existingPlaylistNames: List<String>,
    onDismiss: () -> Unit,
    onLoaded: (DraftPlaylist, PlaylistLoadResult) -> Unit,
) {
    var type by remember { mutableStateOf(PlaylistSourceType.M3U_URL) }
    var name by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(DraftLoadState()) }
    val scope = rememberCoroutineScope()
    val canSubmit = !state.loading &&
        endpoint.trim().isNotBlank() &&
        (type != PlaylistSourceType.XTREAM || (username.trim().isNotBlank() && password.isNotBlank()))
    val dialogConfiguration = LocalConfiguration.current
    val compactDialog = dialogConfiguration.screenWidthDp < 768
    val maxDialogHeight = (dialogConfiguration.screenHeightDp - 48).coerceAtLeast(420).dp
    val dialogWidth = when {
        compactDialog -> 0.dp
        dialogConfiguration.screenWidthDp >= 1280 -> 880.dp
        else -> (dialogConfiguration.screenWidthDp - 56).coerceIn(560, 720).dp
    }
    val connectingMessage = stringResource(R.string.playlist_add_connecting)
    val downloadingMessage = stringResource(R.string.playlist_stage_downloading)
    val preparingMessage = stringResource(R.string.playlist_stage_preparing)
    val loadFailedMessage = stringResource(R.string.playlist_load_failed)
    val cancelText = stringResource(R.string.action_cancel)
    val loadingText = stringResource(R.string.playlist_add_loading)
    val saveLoadText = stringResource(R.string.playlist_add_save_load)

    fun request(finalName: String): CreatePlaylistSourceRequest {
        return CreatePlaylistSourceRequest(
            type = type,
            name = finalName,
            endpoint = normalizeVisibleUrl(endpoint),
            contentHint = ContentHint.AUTO,
            xtreamUsername = username.trim().ifBlank { null },
            xtreamPassword = password.ifBlank { null },
        )
    }

    fun submit() {
        if (!canSubmit) return
        val pressStartedAt = android.os.SystemClock.elapsedRealtime()
        val finalName = resolvedPlaylistName(
            requestedName = name,
            type = type,
            endpoint = endpoint,
            existingNames = existingPlaylistNames,
        )
        val req = request(finalName)
        val validation = validatePlaylistRequest(req)
        if (validation != null) {
            state = DraftLoadState(error = validation)
            return
        }
        state = DraftLoadState(loading = true, message = connectingMessage)
        state = DraftLoadState(loading = true, message = downloadingMessage)
        val firstResponseMs = android.os.SystemClock.elapsedRealtime() - pressStartedAt
        telemetry.recordMany(
            mapOf(
                "playlist_add_start_ms" to telemetry.sinceAppStartMs(),
                "playlist_import_press_to_response_ms" to firstResponseMs,
            ),
        )
        val loadStartedAt = pressStartedAt
        val uiWatchId = telemetry.beginUiWatch("playlist_import")
        val playlistId = UUID.randomUUID().toString()
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    loader.load(playlistId, req) { progress ->
                        scope.launch { state = progress.toDraftLoadState() }
                    }
                }
            }.onSuccess { result ->
                state = DraftLoadState(
                    loading = true,
                    message = preparingMessage,
                    processedItems = result.items.size,
                    totalItems = result.items.size,
                )
                state = DraftLoadState(
                    loading = true,
                    message = preparingMessage,
                    processedItems = result.items.size,
                    totalItems = result.items.size,
                )
                onLoaded(
                    DraftPlaylist(
                        id = playlistId,
                        name = req.name,
                        type = req.type,
                        endpoint = req.endpoint,
                        headers = req.headers,
                        loadStartedAtMs = loadStartedAt,
                        firstResponseMs = firstResponseMs,
                        uiWatchId = uiWatchId,
                    ),
                    result,
                )
            }.onFailure { throwable ->
                telemetry.endUiWatch(uiWatchId)
                telemetry.recordError("Oynatma listesi ekleme hatası", throwable)
                state = DraftLoadState(error = simpleUserMessage(throwable.message.orEmpty()).ifBlank { loadFailedMessage })
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!state.loading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.76f))
                .navigationBarsPadding()
                .padding(
                    horizontal = if (compactDialog) 0.dp else 28.dp,
                    vertical = if (compactDialog) 0.dp else 28.dp,
                ),
            contentAlignment = if (compactDialog) Alignment.BottomCenter else Alignment.Center,
        ) {
            Surface(
                modifier = if (compactDialog) {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
                } else {
                    Modifier
                        .width(dialogWidth)
                        .heightIn(max = maxDialogHeight)
                },
                color = MaterialTheme.colorScheme.surface,
                shape = if (compactDialog) {
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                } else {
                    RoundedCornerShape(24.dp)
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
                shadowElevation = 28.dp,
                tonalElevation = 10.dp,
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup()
                    .verticalScroll(rememberScrollState())
                    .padding(if (compactDialog) 18.dp else 22.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactDialog) 10.dp else 12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        stringResource(R.string.playlist_add_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = if (compactDialog) 22.sp else 23.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.playlist_add_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                    )
                }
                TypeSelector(
                    type = type,
                    onTypeChange = {
                        type = it
                        state = DraftLoadState()
                    },
                )
                TvTextField(
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                        if (state.error != null) state = DraftLoadState()
                    },
                    label = {
                        Text(
                            stringResource(type.endpointLabelRes()),
                        )
                    },
                    placeholder = { Text("https://...") },
                    helperText = stringResource(type.endpointHelperTextRes()),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    requestInitialFocus = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TvTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_add_name_label)) },
                    placeholder = { Text(stringResource(R.string.playlist_add_name_placeholder)) },
                    helperText = stringResource(R.string.playlist_add_name_helper),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == PlaylistSourceType.XTREAM) {
                    TvTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.playlist_add_username_label)) },
                        helperText = stringResource(R.string.playlist_add_username_helper),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TvTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.playlist_add_password_label)) },
                        helperText = stringResource(R.string.playlist_add_password_helper),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.message?.let {
                    LoadingStepText(
                        text = it,
                        progress = state.processedItems.takeIf { count -> count > 0 }?.let { count ->
                            contentProgressLabel(count, state.totalItems)
                        },
                    )
                }
                state.error?.let { ErrorText(it) }
                if (!canSubmit && !state.loading) {
                    Text(
                        text = if (type == PlaylistSourceType.XTREAM) {
                            stringResource(R.string.playlist_add_invalid_xtream_hint)
                        } else {
                            stringResource(R.string.playlist_add_invalid_url_hint)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DialogGhostAction(
                        text = cancelText,
                        enabled = !state.loading,
                        modifier = Modifier.weight(0.8f),
                        onClick = onDismiss,
                    )
                    DialogPrimaryAction(
                        text = if (state.loading) loadingText else saveLoadText,
                        enabled = canSubmit,
                        modifier = Modifier.weight(1.35f),
                        onClick = ::submit,
                    )
                }
            }
            }
        }
    }
}
