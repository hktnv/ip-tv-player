package com.hktnv.iptvbox.ui.media

import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.core.security.SecretRedactor
import com.hktnv.iptvbox.data.playlist.CreatePlaylistSourceRequest

internal fun safeUrl(url: String): String = SecretRedactor.redact(url)

internal fun validatePlaylistRequest(request: CreatePlaylistSourceRequest): String? {
    val endpointWithoutScheme = request.endpoint
        .removePrefix("https://")
        .removePrefix("HTTPS://")
        .removePrefix("http://")
        .removePrefix("HTTP://")
        .trim()
    if (endpointWithoutScheme.isBlank()) {
        return when (request.type) {
            PlaylistSourceType.JSON_DIRECTORY -> "JSON URL zorunlu."
            PlaylistSourceType.M3U_URL -> "Oynatma listesi URL zorunlu."
            PlaylistSourceType.XTREAM -> "Sunucu URL zorunlu."
        }
    }
    if (!request.endpoint.startsWith("https://", ignoreCase = true) &&
        !request.endpoint.startsWith("http://", ignoreCase = true)
    ) {
        return "URL http:// veya https:// ile ba\u015flamal\u0131."
    }
    if (request.type == PlaylistSourceType.XTREAM) {
        if (request.xtreamUsername.isNullOrBlank()) return "Kullan\u0131c\u0131 ad\u0131 zorunlu."
        if (request.xtreamPassword.isNullOrBlank()) return "Parola zorunlu."
    }
    return null
}

internal fun normalizeVisibleUrl(value: String): String {
    val trimmed = value.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    return "http://$trimmed"
}

internal fun PlaylistSourceType.label(): String {
    return when (this) {
        PlaylistSourceType.JSON_DIRECTORY -> "JSON"
        PlaylistSourceType.M3U_URL -> "M3U"
        PlaylistSourceType.XTREAM -> "Xtream"
    }
}

internal fun String?.looksLikeLogo(): Boolean {
    val value = this?.lowercase().orEmpty()
    return value.contains("logo") ||
        value.contains("picon") ||
        value.contains("resim.yayins.com") ||
        value.contains("channel")
}
