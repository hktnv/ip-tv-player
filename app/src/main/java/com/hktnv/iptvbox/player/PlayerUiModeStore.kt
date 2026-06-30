package com.hktnv.iptvbox.player

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PlayerUiModeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _mode = MutableStateFlow(readMode())

    val mode: StateFlow<PlayerUiMode> = _mode.asStateFlow()

    fun setMode(mode: PlayerUiMode) {
        preferences.edit().putString(KEY_MODE, mode.preferenceValue).apply()
        _mode.value = mode
    }

    private fun readMode(): PlayerUiMode {
        return PlayerUiMode.fromPreferenceValue(preferences.getString(KEY_MODE, null))
    }

    private companion object {
        const val PREFERENCES_NAME = "player_ui_mode"
        const val KEY_MODE = "mode"
    }
}
