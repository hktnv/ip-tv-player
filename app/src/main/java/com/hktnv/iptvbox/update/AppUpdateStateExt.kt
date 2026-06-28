package com.hktnv.iptvbox.update

internal fun AppUpdateUiState.pendingUpdate(): AppUpdateInfo? = when (this) {
    is AppUpdateUiState.Available -> update
    is AppUpdateUiState.Downloading -> update
    is AppUpdateUiState.PermissionRequired -> update
    else -> null
}
