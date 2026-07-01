package com.hktnv.iptvbox.data.playlist

internal fun cleanM3uTitle(value: String): String {
    var cleaned = repairM3uEncoding(value)
    if (cleaned.indexOf('#') >= 0) {
        cleaned = cleaned.replace(M3uPlaylistPatterns.extInfRegex, " ")
    }
    if (cleaned.indexOf('=') >= 0 && cleaned.indexOf('"') >= 0) {
        cleaned = cleaned.replace(M3uPlaylistPatterns.attributeRegex, " ")
    }
    if (cleaned.contains("http://", ignoreCase = true) || cleaned.contains("https://", ignoreCase = true)) {
        cleaned = cleaned.replace(M3uPlaylistPatterns.urlRegex, " ")
    }
    if (cleaned.mayContainSecretParam()) {
        cleaned = cleaned.replace(M3uPlaylistPatterns.secretParamRegex, " ")
    }
    return cleaned
        .compactWhitespace()
        .trimTitleEdges()
        .stripLanguagePrefix()
        .compactWhitespace()
        .trim()
}

internal fun normalizeM3uMarkers(value: String): String {
    val repaired = repairM3uEncoding(value)
    val builder = StringBuilder(repaired.length)
    repaired.forEach { char ->
        builder.append(char.toM3uMarkerChar())
    }
    return builder.toString()
}

internal fun cleanM3uCategory(value: String?): String? {
    return cleanM3uOptional(value)
        ?.trim('|')
        ?.compactWhitespace()
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
    return stripLanguageCodePrefix()
        .stripLanguageWordPrefix()
        .trim()
}

private fun String.stripLanguageCodePrefix(): String {
    var index = 0
    var letters = 0
    while (index < length && letters < 3 && this[index] in 'A'..'Z') {
        index += 1
        letters += 1
    }
    if (letters !in 2..3) return this
    while (index < length && this[index].isWhitespace()) index += 1
    if (index >= length || this[index] !in LANGUAGE_CODE_SEPARATORS) return this
    index += 1
    while (index < length && this[index].isWhitespace()) index += 1
    return substring(index)
}

private fun String.stripLanguageWordPrefix(): String {
    LANGUAGE_WORD_PREFIXES.forEach { prefix ->
        if (length > prefix.length &&
            regionMatches(0, prefix, 0, prefix.length, ignoreCase = true) &&
            this[prefix.length].isWhitespace()
        ) {
            var index = prefix.length + 1
            while (index < length && this[index].isWhitespace()) index += 1
            return substring(index)
        }
    }
    return this
}

private fun String.compactWhitespace(): String {
    var builder: StringBuilder? = null
    var pendingSpace = false
    forEachIndexed { index, char ->
        if (char.isWhitespace()) {
            if (builder == null) builder = StringBuilder(length).append(this, 0, index)
            pendingSpace = builder?.isNotEmpty() == true
        } else {
            val current = builder
            if (current != null) {
                if (pendingSpace) current.append(' ')
                pendingSpace = false
                current.append(char)
            }
        }
    }
    return builder?.toString() ?: this
}

private fun String.trimTitleEdges(): String {
    var start = 0
    var end = length
    while (start < end && this[start].isTitleEdgeChar()) start += 1
    while (end > start && this[end - 1].isTitleEdgeChar()) end -= 1
    return if (start == 0 && end == length) this else substring(start, end)
}

private fun String.mayContainSecretParam(): Boolean {
    return contains("username=", ignoreCase = true) ||
        contains("password=", ignoreCase = true) ||
        contains("token=", ignoreCase = true) ||
        contains("output=", ignoreCase = true) ||
        contains("type=", ignoreCase = true)
}

private fun Char.toM3uMarkerChar(): Char {
    return when (this) {
        in 'A'..'Z' -> (code + ASCII_CASE_OFFSET).toChar()
        '\u00C7', '\u00E7' -> 'c'
        '\u011E', '\u011F' -> 'g'
        '\u0130', '\u0131', '\u00CC', '\u00EC', '\u00CD', '\u00ED', '\u00CE', '\u00EE', '\u00CF', '\u00EF' -> 'i'
        '\u00D6', '\u00F6' -> 'o'
        '\u015E', '\u015F' -> 's'
        '\u00DC', '\u00FC' -> 'u'
        '\u00C2', '\u00E2' -> 'a'
        else -> lowercaseChar()
    }
}

private fun Char.isTitleEdgeChar(): Boolean {
    return isWhitespace() || this == ',' || this == '-' || this == '|' || this == '\u00BB' || this == '\u2022'
}

private fun Long.mix(value: String): Long {
    var current = this
    value.forEach { char ->
        current = current * 31 + char.code
    }
    return current
}

private val LANGUAGE_CODE_SEPARATORS = setOf(':', '|', '-')
private val LANGUAGE_WORD_PREFIXES = listOf("TR", "DE", "UK", "US", "EN")
private const val ASCII_CASE_OFFSET = 32
