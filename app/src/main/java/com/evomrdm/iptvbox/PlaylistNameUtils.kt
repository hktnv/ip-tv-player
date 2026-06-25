package com.evomrdm.iptvbox

import com.evomrdm.iptvbox.core.model.PlaylistSourceType
import java.net.URI

internal fun resolvedPlaylistName(
    requestedName: String,
    type: PlaylistSourceType,
    endpoint: String,
    existingNames: Collection<String>,
): String {
    val base = requestedName.trim().ifBlank {
        when (type) {
            PlaylistSourceType.XTREAM -> xtreamNameFromEndpoint(endpoint)
            PlaylistSourceType.JSON_DIRECTORY,
            PlaylistSourceType.M3U_URL -> "Oynatma Listesi"
        }
    }.ifBlank { "Oynatma Listesi" }
    return uniquePlaylistName(base, existingNames)
}

internal fun uniquePlaylistName(baseName: String, existingNames: Collection<String>): String {
    val cleanBase = baseName.trim().ifBlank { "Oynatma Listesi" }
    val existing = existingNames.map { it.trim().lowercase() }.toSet()
    if (cleanBase.lowercase() !in existing) return cleanBase
    var index = 2
    while (true) {
        val candidate = "$cleanBase $index"
        if (candidate.lowercase() !in existing) return candidate
        index += 1
    }
}

private fun xtreamNameFromEndpoint(endpoint: String): String {
    val normalized = normalizeVisibleUrl(endpoint)
    val host = runCatching { URI(normalized).host.orEmpty() }.getOrDefault("")
        .removePrefix("www.")
        .substringBefore(':')
        .trim()
    if (host.isBlank()) return "Oynatma Listesi"
    return host.split('.', '-', '_')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
        .ifBlank { host }
}
