package com.evomrdm.iptvbox

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import com.evomrdm.iptvbox.core.model.ContentHint
import com.evomrdm.iptvbox.core.model.PlaylistSourceType
import com.evomrdm.iptvbox.data.playlist.CreatePlaylistSourceRequest
import com.evomrdm.iptvbox.data.playlist.PlaylistLoadResult
import com.evomrdm.iptvbox.data.playlist.RemotePlaylistLoader
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    Dialog(onDismissRequest = { if (!state.loading) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0D141C),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF314052)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "Yeni Oynatma Listesi",
                        color = IptvColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Önce liste adresini girin. Liste adı isteğe bağlıdır.",
                        color = IptvColors.TextSecondary,
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                TvTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Liste adı (isteğe bağlı)") },
                    placeholder = { Text("Boş bırakılırsa otomatik ad verilir") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == PlaylistSourceType.XTREAM) {
                    TvTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Kullanıcı adı") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TvTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Parola") },
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
                        color = IptvColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !state.loading,
                        modifier = Modifier.weight(0.8f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text("Vazgeç", maxLines = 1)
                    }
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

@Composable
private fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val configuration = LocalConfiguration.current
    val television = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(focused, editing, television) {
        if (!television) return@LaunchedEffect
        if (focused && editing) {
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    BackHandler(enabled = television && editing) {
        editing = false
        keyboard?.hide()
        focusManager.clearFocus(force = true)
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        singleLine = true,
        readOnly = television && !editing,
        keyboardOptions = keyboardOptions.copy(showKeyboardOnFocus = !television),
        visualTransformation = visualTransformation,
        colors = playlistFieldColors(),
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
                if (television && !it.isFocused) {
                    editing = false
                }
            }
            .onPreviewKeyEvent { event ->
                if (!television) return@onPreviewKeyEvent false
                if (!focused) return@onPreviewKeyEvent false
                when {
                    event.type == KeyEventType.KeyUp && event.key.isDialogSelectKey() -> {
                        editing = true
                        true
                    }
                    event.key == Key.DirectionDown && editing -> {
                        editing = false
                        keyboard?.hide()
                        focusManager.clearFocus(force = true)
                        false
                    }
                    event.key == Key.Back && editing -> {
                        editing = false
                        keyboard?.hide()
                        focusManager.clearFocus(force = true)
                        true
                    }
                    else -> false
                }
            },
    )
}

private fun Key.isDialogSelectKey(): Boolean {
    return this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter
}

@Composable
private fun LoadingStepText(text: String) {
    Surface(
        color = Color(0xFF101B25),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF34465A)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                color = IptvColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Lütfen bekleyin, işlem devam ediyor.",
                color = IptvColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun DialogPrimaryAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onClick),
        color = when {
            !enabled -> Color(0xFF27313C)
            focused -> IptvColors.Accent
            else -> Color(0xFF00C795)
        },
        shape = RoundedCornerShape(10.dp),
        border = if (focused) BorderStroke(2.dp, Color.White) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = when {
                    !enabled -> IptvColors.TextSecondary.copy(alpha = 0.76f)
                    focused -> Color.White
                    else -> IptvColors.Night
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun TypeSelector(
    type: PlaylistSourceType,
    onTypeChange: (PlaylistSourceType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(PlaylistSourceType.JSON_DIRECTORY, PlaylistSourceType.M3U_URL, PlaylistSourceType.XTREAM).forEach { candidate ->
            SourceTypeCard(
                modifier = Modifier.weight(1f),
                label = candidate.label(),
                description = candidate.description(),
                selected = candidate == type,
                onClick = { onTypeChange(candidate) },
            )
        }
    }
}

@Composable
private fun SourceTypeCard(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 78.dp)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = when {
            focused -> Color(0xFF1D3346)
            selected -> Color(0xFF12251F)
            else -> Color(0xFF101821)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> Color(0xFFB9D8FF)
                selected -> IptvColors.Accent
                else -> Color(0xFF334253)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = if (selected) IptvColors.Accent else IptvColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                color = IptvColors.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun playlistFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFFB9D8FF),
    focusedLabelColor = Color(0xFFB9D8FF),
    cursorColor = IptvColors.Accent,
    unfocusedBorderColor = Color(0xFF566272),
    unfocusedLabelColor = IptvColors.TextSecondary,
)

private fun PlaylistSourceType.description(): String {
    return when (this) {
        PlaylistSourceType.JSON_DIRECTORY -> "JSON adresi"
        PlaylistSourceType.M3U_URL -> "IPTV liste URL'si"
        PlaylistSourceType.XTREAM -> "Sunucu girişi"
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
            color = IptvColors.Panel,
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Oynatma Listesi Adı", color = IptvColors.TextPrimary, fontSize = 22.sp)
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
