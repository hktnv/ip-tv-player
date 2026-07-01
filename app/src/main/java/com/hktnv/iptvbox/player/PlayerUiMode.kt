package com.hktnv.iptvbox.player

internal enum class PlayerUiMode(
    val preferenceValue: String,
    val diagnosticValue: String,
) {
    CustomOsd(
        preferenceValue = "custom_osd",
        diagnosticValue = "custom_osd",
    );

    companion object {
        fun fromPreferenceValue(value: String?): PlayerUiMode {
            return entries.firstOrNull { it.preferenceValue == value } ?: CustomOsd
        }
    }
}
