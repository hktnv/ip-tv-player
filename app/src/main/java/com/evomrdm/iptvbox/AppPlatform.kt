package com.evomrdm.iptvbox

import android.content.Context
import android.os.Build

internal fun installedVersionCode(context: Context): Int {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode
    }
}
