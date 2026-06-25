package com.evomrdm.iptvbox.data.playlist

import com.evomrdm.iptvbox.core.model.ContentHint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PlaylistDirectoryParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    fun parse(text: String): List<DirectoryPlaylistCandidate> {
        return json.decodeFromString<List<DirectoryEntryDto>>(text)
            .filter { it.enabled != false }
            .mapIndexed { index, dto ->
                val name = dto.name?.trim().orEmpty()
                val url = dto.url?.trim().orEmpty()
                require(name.isNotEmpty()) { "JSON directory item $index is missing name." }
                require(url.isHttpUrl()) { "JSON directory item $index has invalid url." }
                DirectoryPlaylistCandidate(
                    name = name,
                    url = url,
                    epgUrl = dto.epgUrl?.takeIf { it.isHttpUrl() },
                    headers = dto.headers.orEmpty(),
                    refreshHours = dto.refreshHours?.coerceAtLeast(1),
                    contentHint = dto.contentHint?.toContentHint() ?: ContentHint.AUTO,
                )
            }
    }
}

data class DirectoryPlaylistCandidate(
    val name: String,
    val url: String,
    val epgUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val refreshHours: Int? = null,
    val contentHint: ContentHint = ContentHint.AUTO,
)

@Serializable
private data class DirectoryEntryDto(
    val name: String? = null,
    val url: String? = null,
    val epgUrl: String? = null,
    val enabled: Boolean? = null,
    val headers: Map<String, String>? = null,
    val refreshHours: Int? = null,
    val contentHint: String? = null,
)

private fun String.toContentHint(): ContentHint {
    return when (trim().lowercase()) {
        "live", "canli", "channel", "channels" -> ContentHint.LIVE
        "movie", "movies", "film", "vod" -> ContentHint.MOVIES
        "series", "dizi", "shows" -> ContentHint.SERIES
        "mixed", "mix" -> ContentHint.MIXED
        else -> ContentHint.AUTO
    }
}

internal fun String.isHttpUrl(): Boolean {
    return startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)
}
