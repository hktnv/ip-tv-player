package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.BuildConfig

internal val isPlayerDiagnosticEnabled: Boolean
    get() = playerDiagnosticEnabledFor(BuildConfig.DEBUG)

internal fun playerDiagnosticEnabledFor(isDebugBuild: Boolean): Boolean {
    return isDebugBuild
}
