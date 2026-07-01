package com.hktnv.iptvbox.data.playlist

internal fun parseExtInf(line: String): ExtInf {
    val (metadata, rawTitle) = splitExtInf(line)
    val attrs = parseAttributes(metadata)
    val title = cleanM3uTitle(rawTitle)
        .ifBlank { cleanM3uTitle(attrs["tvg-name"].orEmpty()) }
        .ifBlank { M3uPlaylistPatterns.UNTITLED }
    val tvgName = cleanM3uOptional(attrs["tvg-name"])
    val groupTitle = cleanM3uCategory(attrs["group-title"])
    return ExtInf(
        title = title,
        tvgId = cleanM3uOptional(attrs["tvg-id"]),
        tvgName = tvgName,
        groupTitle = groupTitle,
        logoUrl = attrs["tvg-logo"]?.let(::repairM3uEncoding)?.trim()?.takeIf { it.isNotBlank() },
        normalizedGroupTitle = groupTitle?.let(::normalizeM3uMarkers).orEmpty(),
        normalizedTvgName = tvgName?.let(::normalizeM3uMarkers).orEmpty(),
    )
}

private fun splitExtInf(line: String): Pair<String, String> {
    var quoted = false
    line.forEachIndexed { index, char ->
        when (char) {
            '"' -> quoted = !quoted
            ',' -> if (!quoted) return line.substring(0, index) to line.substring(index + 1)
        }
    }
    return line to ""
}

internal fun parseAttributes(value: String): Map<String, String> {
    val attrs = LinkedHashMap<String, String>(6)
    var index = 0
    while (index < value.length) {
        while (index < value.length && !value[index].isAttributeKeyStart()) index += 1
        val keyStart = index
        while (index < value.length && value[index].isAttributeKeyChar()) index += 1
        if (keyStart == index || index >= value.length || value[index] != '=') {
            index += 1
            continue
        }
        val key = value.substring(keyStart, index).lowercase()
        index += 1
        if (index >= value.length || value[index] != '"') continue
        index += 1
        val valueStart = index
        while (index < value.length && value[index] != '"') index += 1
        attrs[key] = repairM3uEncoding(value.substring(valueStart, index)).trim()
        if (index < value.length && value[index] == '"') index += 1
    }
    return attrs
}

private fun Char.isAttributeKeyStart(): Boolean = isLetter()

private fun Char.isAttributeKeyChar(): Boolean {
    return isLetterOrDigit() || this == '-' || this == '_'
}
