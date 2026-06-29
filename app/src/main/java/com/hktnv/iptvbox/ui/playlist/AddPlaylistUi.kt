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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val dialogWidth = when {
        compactDialog -> 0.dp
        dialogConfiguration.screenWidthDp >= 1280 -> 880.dp
        else -> (dialogConfiguration.screenWidthDp - 56).coerceIn(560, 720).dp
    }

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
        state = DraftLoadState(loading = true, message = "Bağlantı kuruluyor")
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
                state = DraftLoadState(loading = true, message = "Liste okunuyor")
                withContext(Dispatchers.IO) { loader.load(playlistId, req) }
            }.onSuccess { result ->
                state = DraftLoadState(loading = true, message = "Katalog hazırlanıyor")
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
                state = DraftLoadState(error = simpleUserMessage(throwable.message.orEmpty()).ifBlank { "Liste yüklenemedi" })
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
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.width(dialogWidth)
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
                    .padding(if (compactDialog) 20.dp else 28.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactDialog) 14.dp else 16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "Yeni Oynatma Listesi",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Önce liste adresini girin. Liste adı isteğe bağlıdır.",
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
                            when (type) {
                                PlaylistSourceType.JSON_DIRECTORY -> "JSON URL"
                                PlaylistSourceType.M3U_URL -> "Oynatma listesi URL"
                                PlaylistSourceType.XTREAM -> "Sunucu URL"
                            },
                        )
                    },
                    placeholder = { Text("https://...") },
                    helperText = type.endpointHelperText(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    requestInitialFocus = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TvTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Liste adı") },
                    placeholder = { Text("Oynatma Listem") },
                    helperText = "Boş bırakırsanız otomatik ad verilir.",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == PlaylistSourceType.XTREAM) {
                    TvTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Kullanıcı adı") },
                        helperText = "Xtream hesabınızdaki kullanıcı adını girin.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TvTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Parola") },
                        helperText = "Parola yalnızca bağlantı kurmak için kullanılır.",
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.message?.let {
                    LoadingStepText(it)
                }
                state.error?.let { ErrorText(it) }
                if (!canSubmit && !state.loading) {
                    Text(
                        text = if (type == PlaylistSourceType.XTREAM) {
                            "Sunucu URL, kullanıcı adı ve parola girildiğinde devam edebilirsiniz."
                        } else {
                            "URL girildiğinde Kaydet ve Yükle aktif olur."
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
                        text = "Vazgeç",
                        enabled = !state.loading,
                        modifier = Modifier.weight(0.8f),
                        onClick = onDismiss,
                    )
                    DialogPrimaryAction(
                        text = if (state.loading) "Yükleniyor" else "Kaydet ve Yükle",
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
