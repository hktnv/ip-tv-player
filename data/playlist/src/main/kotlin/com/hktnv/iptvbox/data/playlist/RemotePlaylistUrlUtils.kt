package com.hktnv.iptvbox.data.playlist

import java.net.URLEncoder

internal fun buildXtreamM3uUrl(
    serverUrl: String,
    username: String,
    password: String,
): String {
    val normalized = normalizeUserUrl(serverUrl).trimEnd('/')
    val base = when {
        normalized.contains("/get.php", ignoreCase = true) -> normalized.substringBefore("/get.php") + "/get.php"
        normalized.contains("/player_api.php", ignoreCase = true) -> {
            normalized.substringBefore("/player_api.php") + "/get.php"
        }
        else -> "$normalized/get.php"
    }
    return "$base?username=${username.urlEncode()}&password=${password.urlEncode()}&type=m3u_plus&output=mpegts"
}

internal fun normalizeUserUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    return "http://$trimmed"
}

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
