package com.hktnv.iptvbox.model

internal val PlaylistAutoUpdateHourOptions = listOf(0, 6, 12, 24)

internal fun playlistAutoUpdateLabel(hours: Int): String {
    return when {
        hours <= 0 -> "Kapalı"
        hours == 24 -> "Her gün"
        else -> "$hours saatte bir"
    }
}
