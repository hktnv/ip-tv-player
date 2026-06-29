package com.hktnv.iptvbox.ui.dialog
import androidx.compose.material3.MaterialTheme
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.common.TvFocusPanel

@Composable
internal fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }
    BackHandler { onConfirm() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { cancelFocusRequester.requestFocus() }
    }
    Dialog(onDismissRequest = onConfirm) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Uygulamadan \u00e7\u0131k\u0131ls\u0131n m\u0131?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Vazge\u00e7 ile uygulamaya d\u00f6n\u00fcn veya \u00c7\u0131k\u0131\u015f ile kapat\u0131n.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ExitDialogButton(
                        text = "Vazge\u00e7",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        focusRequester = cancelFocusRequester,
                        primary = false,
                    )
                    ExitDialogButton(
                        text = "\u00c7\u0131k\u0131\u015f",
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        primary = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    primary: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        focused -> if (primary) MaterialTheme.colorScheme.primaryContainer else TvFocusPanel
        primary -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .tvClickable(onClick = onClick),
        color = background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) TvFocusBorder else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Text(
            text = text,
            color = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
