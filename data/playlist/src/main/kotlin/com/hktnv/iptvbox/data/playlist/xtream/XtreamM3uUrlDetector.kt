package com.hktnv.iptvbox.data.playlist.xtream

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object XtreamM3uUrlDetector {
    fun detect(rawUrl: String): XtreamCredentials? {
        val url = normalize(rawUrl).toHttpUrlOrNull() ?: return null
        if (!url.encodedPath.endsWith("/get.php", ignoreCase = true)) return null
        val username = url.queryParameter("username")?.takeIf { it.isNotBlank() } ?: return null
        val password = url.queryParameter("password")?.takeIf { it.isNotBlank() } ?: return null
        val pathPrefix = url.encodedPath.removeSuffix("/get.php").trimEnd('/')
        val server = url.newBuilder()
            .encodedPath(pathPrefix.ifBlank { "/" })
            .query(null)
            .fragment(null)
            .build()
            .toString()
            .trimEnd('/')
        return XtreamCredentials(
            serverUrl = server,
            username = username,
            password = password,
        )
    }

    private fun normalize(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }
}
