package com.hktnv.iptvbox.model

internal val PlaylistAutoUpdateHourOptions = listOf(0, 6, 12, 24)

internal fun playlistAutoUpdateLabel(hours: Int): String {
    return when (hours) {
        6 -> "6 Saat"
        12 -> "12 Saat"
        24 -> "Günlük"
        else -> "Kapalı"
    }
}
