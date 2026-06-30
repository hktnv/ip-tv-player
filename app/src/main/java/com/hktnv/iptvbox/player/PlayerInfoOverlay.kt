package com.hktnv.iptvbox.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PlayerInfoOverlay(
    info: PlayerContentInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = 44.dp, top = 34.dp, end = 44.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PlayerOsdText(
                text = info.typeLabel,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            PlayerOsdText(
                text = info.category,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        PlayerOsdText(
            text = info.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        PlayerNeighborInfo(label = "Önceki", title = info.previousTitle)
        PlayerNeighborInfo(label = "Sonraki", title = info.nextTitle)
    }
}

@Composable
private fun PlayerNeighborInfo(
    label: String,
    title: String?,
) {
    if (title.isNullOrBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PlayerOsdText(
            text = "$label:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        PlayerOsdText(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerOsdText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    maxLines: Int,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            shadow = Shadow(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                offset = Offset(0f, 1.5f),
                blurRadius = 8f,
            ),
        ),
    )
}
