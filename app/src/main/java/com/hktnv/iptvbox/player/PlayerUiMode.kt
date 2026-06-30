package com.hktnv.iptvbox.player

internal enum class PlayerUiMode(
    val preferenceValue: String,
    val diagnosticValue: String,
    val title: String,
    val description: String,
) {
    CustomOsd(
        preferenceValue = "custom_osd",
        diagnosticValue = "custom_osd",
        title = "Modern OSD",
        description = "IPTV Box kumanda kontrolleri ve içerik listesi.",
    ),
    StandardMedia3(
        preferenceValue = "standard_media3",
        diagnosticValue = "standard_media3",
        title = "Standart Media3",
        description = "Media3/ExoPlayer'ın yerleşik kontrol arayüzü.",
    );

    fun next(): PlayerUiMode = when (this) {
        CustomOsd -> StandardMedia3
        StandardMedia3 -> CustomOsd
    }

    companion object {
        fun fromPreferenceValue(value: String?): PlayerUiMode {
            return entries.firstOrNull { it.preferenceValue == value } ?: CustomOsd
        }
    }
}
