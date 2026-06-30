package com.hktnv.iptvbox.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun PlayerOrientationLock() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity()
        val shouldLock = shouldLockPlayerToLandscape(context.resources.configuration.uiMode)
        val previousOrientation = activity?.requestedOrientation
        if (activity != null && shouldLock) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            if (activity != null && shouldLock && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }
}

internal fun shouldLockPlayerToLandscape(uiMode: Int): Boolean {
    return (uiMode and Configuration.UI_MODE_TYPE_MASK) != Configuration.UI_MODE_TYPE_TELEVISION
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
