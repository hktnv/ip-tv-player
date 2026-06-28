package com.hktnv.iptvbox.ui.dialog

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.media.displayTitle

@Composable
internal fun ContentOptionsDialog(
    item: CatalogItem,
    favorite: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val openFocusRequester = remember { FocusRequester() }
    BackHandler { onDismiss() }
    LaunchedEffect(item.id) {
        withFrameNanos { }
        runCatching { openFocusRequester.requestFocus() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            color = Color(0xFF101821),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF2F4153)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "\u0130\u00e7erik se\u00e7enekleri",
                    color = IptvColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.displayTitle(),
                    color = IptvColors.TextSecondary,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ContentOptionButton(
                        text = "Oynat",
                        onClick = onOpen,
                        modifier = Modifier.weight(1f),
                        focusRequester = openFocusRequester,
                        primary = true,
                    )
                    ContentOptionButton(
                        text = if (favorite) "Favoriden \u00e7\u0131kar" else "Favoriye ekle",
                        onClick = onToggleFavorite,
                        modifier = Modifier.weight(1f),
                        primary = false,
                    )
                    ContentOptionButton(
                        text = "Kapat",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        primary = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentOptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    primary: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -3f)
            .tvClickable(onClick = onClick),
        color = when {
            focused -> if (primary) IptvColors.Accent else TvFocusPanel
            primary -> IptvColors.Accent.copy(alpha = 0.88f)
            else -> Color(0xFF0E1720)
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) TvFocusBorder else Color.White.copy(alpha = 0.12f),
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Text(
            text = text,
            color = if (primary) Color.Black else IptvColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
