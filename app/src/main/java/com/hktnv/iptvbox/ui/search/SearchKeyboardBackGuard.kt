package com.hktnv.iptvbox.ui.search

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun SearchKeyboardBackGuard(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val latestOnBack by rememberUpdatedState(onBack)

    DisposableEffect(activity, enabled) {
        if (!enabled || activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onDispose { }
        } else {
            val callback = OnBackInvokedCallback { latestOnBack() }
            activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                callback,
            )
            onDispose {
                activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
