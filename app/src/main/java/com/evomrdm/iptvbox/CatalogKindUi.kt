package com.evomrdm.iptvbox

import androidx.compose.ui.graphics.Color
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import com.evomrdm.iptvbox.core.model.ContentKind

internal fun ContentKind.label(): String {
    return when (this) {
        ContentKind.LIVE_CHANNEL -> "Canlı TV"
        ContentKind.MOVIE -> "Film"
        ContentKind.SERIES -> "Dizi"
        ContentKind.SEASON -> "Sezon"
        ContentKind.EPISODE -> "Bölüm"
        ContentKind.RADIO -> "Radyo"
    }
}

internal fun ContentKind.badgeLabel(): String {
    return when (this) {
        ContentKind.LIVE_CHANNEL -> "CANLI"
        ContentKind.MOVIE -> "FİLM"
        ContentKind.SERIES -> "DİZİ"
        ContentKind.SEASON -> "SEZON"
        ContentKind.EPISODE -> "BÖLÜM"
        ContentKind.RADIO -> "RADYO"
    }
}

internal fun ContentKind.shortLabel(): String {
    return when (this) {
        ContentKind.LIVE_CHANNEL -> "TV"
        ContentKind.MOVIE -> "FILM"
        ContentKind.SERIES -> "DIZI"
        ContentKind.SEASON -> "S"
        ContentKind.EPISODE -> "B"
        ContentKind.RADIO -> "FM"
    }
}

internal fun ContentKind.tint(): Color {
    return when (this) {
        ContentKind.LIVE_CHANNEL -> IptvColors.Accent
        ContentKind.MOVIE -> Color(0xFFFFC857)
        ContentKind.SERIES,
        ContentKind.SEASON,
        ContentKind.EPISODE -> Color(0xFF7DD3FC)
        ContentKind.RADIO -> Color(0xFFE6A8FF)
    }
}
