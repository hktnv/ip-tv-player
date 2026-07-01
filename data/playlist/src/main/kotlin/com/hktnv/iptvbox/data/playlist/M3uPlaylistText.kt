package com.hktnv.iptvbox.data.playlist

import java.util.Locale

internal fun cleanM3uTitle(value: String): String {
    return repairM3uEncoding(value)
        .replace(M3uPlaylistPatterns.extInfRegex, " ")
        .replace(M3uPlaylistPatterns.attributeRegex, " ")
        .replace(M3uPlaylistPatterns.urlRegex, " ")
        .replace(M3uPlaylistPatterns.secretParamRegex, " ")
        .replace(M3uPlaylistPatterns.whitespaceRegex, " ")
        .trim(' ', ',', '-', '|', '\u00BB', '\u2022')
        .stripLanguagePrefix()
        .replace(M3uPlaylistPatterns.whitespaceRegex, " ")
        .trim()
}

internal fun normalizeM3uMarkers(value: String): String {
    val lower = repairM3uEncoding(value).lowercase(Locale.ROOT)
    val builder = StringBuilder(lower.length)
    lower.forEach { char ->
        builder.append(
            when (char) {
                '\u00E7' -> 'c'
                '\u011F' -> 'g'
                '\u0131', '\u00EE', '\u00EF' -> 'i'
                '\u00F6' -> 'o'
                '\u015F' -> 's'
                '\u00FC' -> 'u'
                '\u00E2' -> 'a'
                else -> char
            },
        )
    }
    return builder.toString()
}

internal fun cleanM3uCategory(value: String?): String? {
    return cleanM3uOptional(value)
        ?.replace(M3uPlaylistPatterns.pipeEdgeRegex, "")
        ?.replace(M3uPlaylistPatterns.whitespaceRegex, " ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

internal fun cleanM3uOptional(value: String?): String? {
    return value
        ?.let(::cleanM3uTitle)
        ?.takeIf { it.isNotBlank() }
}

internal fun m3uUrlFileName(url: String): String {
    return url.substringBefore('?')
        .substringAfterLast('/')
        .substringBeforeLast('.', missingDelimiterValue = "")
        .replace('-', ' ')
        .replace('_', ' ')
        .let(::cleanM3uTitle)
        .ifBlank { M3uPlaylistPatterns.UNTITLED }
}

internal fun repairM3uEncoding(value: String): String {
    if (!value.any { it == '\u00C3' || it == '\u00C4' || it == '\u00C5' }) return value
    return runCatching {
        String(value.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
    }.getOrDefault(value)
}

internal fun stableM3uItemId(sourceId: String, url: String, title: String): String {
    var hash = 1125899906842597L
    hash = hash.mix(sourceId)
    hash = hash.mix(title)
    hash = hash.mix(url)
    return java.lang.Long.toUnsignedString(hash, 16)
}

private fun String.stripLanguagePrefix(): String {
    return M3uPlaylistPatterns.languageCodePrefixRegex.replace(this, "")
        .let { M3uPlaylistPatterns.languageWordPrefixRegex.replace(it, "") }
        .trim()
}

private fun Long.mix(value: String): Long {
    var current = this
    value.forEach { char ->
        current = current * 31 + char.code
    }
    return current
}
