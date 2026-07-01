package com.hktnv.iptvbox.data.playlist.xtream

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonElement.asObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

internal fun JsonElement.asArrayOrEmpty(): JsonArray = runCatching { jsonArray }.getOrDefault(JsonArray(emptyList()))

internal fun JsonObject.stringField(name: String): String? {
    return primitive(name)?.contentOrNull?.takeIf { it.isNotBlank() }
}

internal fun JsonObject.intField(name: String): Int? {
    val primitive = primitive(name) ?: return null
    return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
}

internal fun JsonObject.firstString(vararg names: String): String? {
    names.forEach { name ->
        stringField(name)?.let { return it }
    }
    return null
}

internal fun JsonObject.firstInt(vararg names: String): Int? {
    names.forEach { name ->
        intField(name)?.let { return it }
    }
    return null
}

internal fun JsonObject.authenticated(): Boolean {
    val userInfo = this["user_info"]?.asObjectOrNull() ?: return false
    return userInfo.intField("auth") == 1 || userInfo.stringField("auth") == "1"
}

internal fun JsonObject.toXtreamBulkEntry(
    idField: String,
    posterFields: List<String>,
): XtreamBulkEntry? {
    val id = intField(idField) ?: return null
    val title = stringField("name") ?: return null
    return XtreamBulkEntry(
        xtreamId = id,
        title = title,
        posterUrl = firstString(*posterFields.toTypedArray()),
        rating = firstString("rating", "rating_5based", "rating_count_kinopoisk"),
        tmdbId = firstInt("tmdb_id", "tmdb"),
    )
}

internal fun JsonObject.toMetadataPayload(): XtreamMetadataPayload {
    val info = this["info"]?.asObjectOrNull() ?: this
    return XtreamMetadataPayload(
        plot = info.firstString("plot", "description"),
        cast = info.stringField("cast"),
        director = info.stringField("director"),
        youtubeTrailer = info.firstString("youtube_trailer", "youtube"),
        duration = info.firstString("duration", "duration_secs"),
        backdropUrl = info.backdropUrl(),
    )
}

private fun JsonObject.backdropUrl(): String? {
    val backdrop = this["backdrop_path"]
    if (backdrop is JsonPrimitive) return backdrop.contentOrNull?.takeIf { it.isNotBlank() }
    if (backdrop is JsonArray) {
        return backdrop.firstOrNull()?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }
    return firstString("backdrop_url", "backdrop")
}

private fun JsonObject.primitive(name: String): JsonPrimitive? {
    return runCatching { this[name]?.jsonPrimitive }.getOrNull()
}
