package com.hktnv.iptvbox.state

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class StartupBehavior(
    val preferenceValue: String,
) {
    Home("home"),
    LastScreen("last_screen"),
    LastStream("last_stream");

    companion object {
        fun fromPreferenceValue(value: String?): StartupBehavior {
            return entries.firstOrNull { it.preferenceValue == value } ?: Home
        }
    }
}

internal class StartupBehaviorStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE,
    )
    private val _behavior = MutableStateFlow(readBehavior())

    val behavior: StateFlow<StartupBehavior> = _behavior.asStateFlow()

    fun setBehavior(behavior: StartupBehavior) {
        preferences.edit().putString(KeyBehavior, behavior.preferenceValue).apply()
        _behavior.value = behavior
    }

    private fun readBehavior(): StartupBehavior {
        return StartupBehavior.fromPreferenceValue(preferences.getString(KeyBehavior, null))
    }
}

private const val PreferencesName = "startup_behavior"
private const val KeyBehavior = "behavior"
