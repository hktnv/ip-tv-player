package com.evomrdm.iptvbox

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
                .background(Color.Black.copy(alpha = 0.76f))
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
                color = Color(0xFF0D141C),
                shape = if (compactDialog) {
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                } else {
                    RoundedCornerShape(24.dp)
                },
                border = BorderStroke(1.dp, Color(0xFF405267)),
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

@Composable
private fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    helperText: String? = null,
    requestInitialFocus: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val configuration = LocalConfiguration.current
    val television = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
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

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (television) 70.dp else 58.dp)
                .focusRequester(focusRequester)
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
        if (helperText != null) {
            Text(
                text = helperText,
                color = IptvColors.TextSecondary.copy(alpha = 0.92f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
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
    val scale by animateFloatAsState(if (focused && enabled) 1.025f else 1f, tween(140), label = "dialogPrimaryScale")
    val elevation by animateDpAsState(if (focused && enabled) 12.dp else 2.dp, tween(140), label = "dialogPrimaryElevation")
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (focused && enabled) -3f else 0f
            }
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onClick),
        color = when {
            !enabled -> Color(0xFF242D38)
            focused -> Color(0xFF16E3B3)
            else -> Color(0xFF00C795)
        },
        shape = RoundedCornerShape(10.dp),
        border = when {
            focused && enabled -> BorderStroke(2.dp, Color.White)
            !enabled -> BorderStroke(1.dp, Color(0xFF3B4652))
            else -> null
        },
        shadowElevation = elevation,
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
private fun DialogGhostAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onClick),
        color = if (focused) Color(0xFF182638) else Color.Transparent,
        contentColor = if (enabled) IptvColors.TextSecondary else IptvColors.TextSecondary.copy(alpha = 0.42f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) Color(0xFFB9D8FF) else Color(0xFF344050),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
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
    val options = listOf(PlaylistSourceType.JSON_DIRECTORY, PlaylistSourceType.M3U_URL, PlaylistSourceType.XTREAM)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF101821),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF34465A)),
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
            color = IptvColors.TextSecondary.copy(alpha = 0.94f),
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
            focused -> Color(0xFF1D3346)
            selected -> Color(0xFF203044)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> Color(0xFFB9D8FF)
                selected -> Color(0xFF78AFFF)
                else -> Color.Transparent
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
                color = if (selected || focused) IptvColors.TextPrimary else IptvColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
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

private fun PlaylistSourceType.typeHelperText(): String {
    return when (this) {
        PlaylistSourceType.JSON_DIRECTORY -> "JSON seçili: katalog adresini girin."
        PlaylistSourceType.M3U_URL -> "M3U seçili: oynatma listesi URL'sini girin."
        PlaylistSourceType.XTREAM -> "Xtream seçili: sunucu, kullanıcı adı ve parola gerekir."
    }
}

private fun PlaylistSourceType.endpointHelperText(): String {
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
