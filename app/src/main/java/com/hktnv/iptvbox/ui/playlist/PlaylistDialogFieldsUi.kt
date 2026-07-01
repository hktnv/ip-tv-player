package com.hktnv.iptvbox.ui.playlist
import androidx.compose.material3.MaterialTheme
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.core.designsystem.transparent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.focusBorder
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.media.label

@Composable
internal fun TvTextField(
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
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
internal fun LoadingStepText(
    text: String,
    progress: String? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            progress?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                )
            }
            Text(
                text = "Lütfen bekleyin, işlem devam ediyor.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
internal fun DialogPrimaryAction(
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
            !enabled -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        shape = RoundedCornerShape(10.dp),
        border = when {
            focused && enabled -> BorderStroke(2.dp, MaterialTheme.colorScheme.focusBorder)
            !enabled -> BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder)
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
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun DialogGhostAction(
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
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.transparent,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) TvFocusBorder else TvRestingBorder,
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
