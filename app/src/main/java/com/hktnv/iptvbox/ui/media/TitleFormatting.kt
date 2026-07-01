package com.hktnv.iptvbox.ui.media
import java.util.Locale

private val TurkishLocale = Locale.forLanguageTag("tr-TR")
private const val TurkishLetters = "ÇĞİÖŞÜçğıöşü"

internal fun String.readableMovieTitle(): String {
    return readableContentTitle()
}

internal fun String.readableContentTitle(): String {
    val clean = cleanUiTitle()
    val letters = clean.filter { it.isLetter() }
    if (letters.length < 4 || letters.any { it.isLowerCase() }) return clean

    return clean.split(' ')
        .joinToString(" ") { token ->
            if (token.shouldKeepAllCapsToken()) token else token.titleCaseToken()
        }
        .trim()
}

private fun String.shouldKeepAllCapsToken(): Boolean {
    val letterCount = count { it.isLetter() }
    return letterCount in 1..2
}

private fun String.titleCaseToken(): String {
    if (isBlank() || none { it.isLetter() }) return this
    val locale = if (containsTurkishLetter()) TurkishLocale else Locale.ROOT
    val lower = lowercase(locale)
    return lower.replaceFirstChar { first ->
        if (first.isLetter()) first.uppercase(locale) else first.toString()
    }
}

private fun String.containsTurkishLetter(): Boolean = any { it in TurkishLetters }
