package com.hktnv.iptvbox.ui.catalog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hktnv.iptvbox.core.designsystem.accentSubtle
import com.hktnv.iptvbox.core.designsystem.accentText
import com.hktnv.iptvbox.core.designsystem.badgeFilm
import com.hktnv.iptvbox.core.designsystem.badgeFilmText
import com.hktnv.iptvbox.core.model.ContentKind

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

@Composable
internal fun ContentKind.badgeContainerColor(): Color {
    return when (this) {
        ContentKind.LIVE_CHANNEL -> MaterialTheme.colorScheme.accentSubtle
        ContentKind.MOVIE -> MaterialTheme.colorScheme.badgeFilm
        ContentKind.SERIES,
        ContentKind.SEASON,
        ContentKind.EPISODE,
        ContentKind.RADIO -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
internal fun ContentKind.badgeContentColor(): Color {
    return when (this) {
        ContentKind.LIVE_CHANNEL -> MaterialTheme.colorScheme.accentText
        ContentKind.MOVIE -> MaterialTheme.colorScheme.badgeFilmText
        ContentKind.SERIES,
        ContentKind.SEASON,
        ContentKind.EPISODE,
        ContentKind.RADIO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
