package com.hktnv.iptvbox.data.playlist.xtream

import java.io.IOException
import java.net.URLEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.Request

class XtreamApiClient(
    private val client: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun authenticate(credentials: XtreamCredentials): Boolean {
        return fetchElement(credentials).asObjectOrNull()?.authenticated() == true
    }

    fun fetchVodStreams(credentials: XtreamCredentials): List<XtreamBulkEntry> {
        return fetchElement(credentials, action = "get_vod_streams")
            .asArrayOrEmpty()
            .mapNotNull { it.asObjectOrNull()?.toXtreamBulkEntry("stream_id", listOf("stream_icon", "cover")) }
    }

    fun fetchSeries(credentials: XtreamCredentials): List<XtreamBulkEntry> {
        return fetchElement(credentials, action = "get_series")
            .asArrayOrEmpty()
            .mapNotNull { it.asObjectOrNull()?.toXtreamBulkEntry("series_id", listOf("cover", "stream_icon")) }
    }

    fun fetchVodInfo(credentials: XtreamCredentials, streamId: Int): XtreamMetadataPayload? {
        return fetchElement(credentials, action = "get_vod_info", idName = "vod_id", id = streamId)
            .asObjectOrNull()
            ?.toMetadataPayload()
    }

    fun fetchSeriesInfo(credentials: XtreamCredentials, seriesId: Int): XtreamMetadataPayload? {
        return fetchElement(credentials, action = "get_series_info", idName = "series_id", id = seriesId)
            .asObjectOrNull()
            ?.toMetadataPayload()
    }

    private fun fetchElement(
        credentials: XtreamCredentials,
        action: String? = null,
        idName: String? = null,
        id: Int? = null,
    ): JsonElement {
        val request = Request.Builder()
            .url(credentials.apiUrl(action, idName, id))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return json.parseToJsonElement(response.body.string())
        }
    }

    private fun XtreamCredentials.apiUrl(action: String?, idName: String?, id: Int?): String {
        val params = mutableListOf(
            "username=${username.urlEncode()}",
            "password=${password.urlEncode()}",
        )
        if (!action.isNullOrBlank()) params += "action=${action.urlEncode()}"
        if (!idName.isNullOrBlank() && id != null) params += "$idName=$id"
        return "${serverUrl.trimEnd('/')}/player_api.php?${params.joinToString("&")}"
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
