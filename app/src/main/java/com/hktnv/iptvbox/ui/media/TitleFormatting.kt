package com.hktnv.iptvbox.ui.media

internal fun String.readableMovieTitle(): String {
    return readableContentTitle()
}

internal fun String.readableContentTitle(): String {
    return cleanUiTitle()
}
