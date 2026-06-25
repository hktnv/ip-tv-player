package com.evomrdm.iptvbox

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

internal fun Modifier.tvClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val keyHandler = if (enabled) {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key.isSelectKey()) {
                onClick()
                true
            } else {
                false
            }
        }
    } else {
        Modifier
    }
    return this
        .then(keyHandler)
        .clickable(enabled = enabled, onClick = onClick)
}

private fun Key.isSelectKey(): Boolean {
    return this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter
}
