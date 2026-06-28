package com.hktnv.iptvbox.ui.media
import java.util.Locale

internal fun String.readableMovieTitle(): String {
    return readableContentTitle()
}

internal fun String.readableContentTitle(): String {
    val clean = cleanUiTitle()
    val letters = clean.filter { it.isLetter() }
    if (letters.length < 4 || letters.any { it.isLowerCase() }) return clean

    return clean.split(' ')
        .joinToString(" ") { token -> token.titleCaseToken() }
        .trim()
}

private fun String.titleCaseToken(): String {
    if (isBlank() || none { it.isLetter() }) return this
    val lower = lowercase(Locale.ROOT).replace("i\u0307", "i")
    return lower.replaceFirstChar { first ->
        if (first.isLetter()) first.uppercase(Locale.ROOT) else first.toString()
    }
}
