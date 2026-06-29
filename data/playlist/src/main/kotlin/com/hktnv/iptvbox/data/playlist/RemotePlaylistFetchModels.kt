package com.hktnv.iptvbox.data.playlist

import java.io.IOException

internal data class TextFetchResult(
    val text: String,
    val finalUrl: String,
    val warnings: List<String> = emptyList(),
    val urlNormalizeMs: Long = 0L,
    val connectionOpenMs: Long = 0L,
    val downloadMs: Long = 0L,
)

internal data class FetchPayload(
    val text: String,
    val connectionOpenMs: Long,
    val downloadMs: Long,
)

internal data class M3uFetchResult(
    val parsed: ParsedM3uPlaylist,
    val warnings: List<String> = emptyList(),
    val urlNormalizeMs: Long = 0L,
    val connectionOpenMs: Long = 0L,
    val downloadMs: Long = 0L,
)

internal data class M3uFetchPayload(
    val parsed: ParsedM3uPlaylist,
    val connectionOpenMs: Long,
    val downloadMs: Long,
)

internal class TimedFetchException(
    cause: Throwable,
    val connectionOpenMs: Long,
) : IOException(cause)
