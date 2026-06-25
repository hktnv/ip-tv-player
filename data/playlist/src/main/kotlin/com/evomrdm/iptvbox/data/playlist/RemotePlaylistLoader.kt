package com.evomrdm.iptvbox.data.playlist

import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.core.model.PlaylistSourceType
import com.evomrdm.iptvbox.core.network.HttpClientFactory
import com.evomrdm.iptvbox.core.security.SecretRedactor
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RemotePlaylistLoader(
    private val client: OkHttpClient = HttpClientFactory.create(),
    private val directoryParser: PlaylistDirectoryParser = PlaylistDirectoryParser(),
    private val m3uParser: M3uPlaylistParser = M3uPlaylistParser(),
) {
    suspend fun load(
        playlistId: String,
        request: CreatePlaylistSourceRequest,
    ): PlaylistLoadResult = withContext(Dispatchers.IO) {
        val startedNs = System.nanoTime()
        val result = when (request.type) {
            PlaylistSourceType.M3U_URL -> loadM3u(
                playlistId = playlistId,
                name = request.name,
                url = request.endpoint,
                headers = request.headers,
            )

            PlaylistSourceType.JSON_DIRECTORY -> loadDirectory(playlistId, request)

            PlaylistSourceType.XTREAM -> loadM3u(
                playlistId = playlistId,
                name = request.name,
                url = buildXtreamM3uUrl(
                    serverUrl = request.endpoint,
                    username = request.xtreamUsername.orEmpty(),
                    password = request.xtreamPassword.orEmpty(),
                ),
                headers = request.headers,
            )
        }
        result.copy(metrics = result.metrics.copy(totalMs = elapsedMs(startedNs)))
    }

    private fun loadDirectory(
        playlistId: String,
        request: CreatePlaylistSourceRequest,
    ): PlaylistLoadResult {
        val directoryStartedNs = System.nanoTime()
        val directoryFetch = fetchTextWithFallback(request.endpoint, request.headers)
        val candidates = directoryParser.parse(directoryFetch.text)
        val warnings = directoryFetch.warnings.toMutableList()
        val allItems = mutableListOf<CatalogItem>()
        val epgUrls = linkedSetOf<String>()
        var metrics = PlaylistLoadMetrics(
            urlNormalizeMs = directoryFetch.urlNormalizeMs,
            connectionOpenMs = directoryFetch.connectionOpenMs,
            downloadMs = directoryFetch.downloadMs,
        )

        candidates.forEachIndexed { index, candidate ->
            runCatching {
                loadM3u(
                    playlistId = "$playlistId-$index",
                    name = candidate.name,
                    url = candidate.url,
                    headers = request.headers + candidate.headers,
                )
            }.onSuccess { loaded ->
                epgUrls += loaded.epgUrls
                allItems += loaded.items.map { it.copy(sourceId = playlistId) }
                warnings += loaded.warnings
                metrics += loaded.metrics
            }.onFailure { throwable ->
                warnings += "${candidate.name}: ${safeMessage(throwable)}"
            }
        }

        if (allItems.isEmpty()) {
            error("İçerik bulunamadı.")
        }

        return PlaylistLoadResult(
            playlistName = request.name,
            items = allItems.sortedBy { it.providerOrder },
            epgUrls = epgUrls.toList(),
            warnings = warnings,
            metrics = metrics.copy(directoryMs = elapsedMs(directoryStartedNs)),
        )
    }

    private fun loadM3u(
        playlistId: String,
        name: String,
        url: String,
        headers: Map<String, String>,
    ): PlaylistLoadResult {
        val fetch = fetchTextWithFallback(url, headers)
        val parsed = m3uParser.parse(playlistId, fetch.text)
        if (parsed.items.isEmpty()) {
            error("İçerik bulunamadı.")
        }
        return PlaylistLoadResult(
            playlistName = name,
            items = parsed.items,
            epgUrls = parsed.epgUrls,
            warnings = fetch.warnings,
            metrics = PlaylistLoadMetrics(
                urlNormalizeMs = fetch.urlNormalizeMs,
                connectionOpenMs = fetch.connectionOpenMs,
                downloadMs = fetch.downloadMs,
                lineReadMs = parsed.lineReadMs,
                parseMs = parsed.parseMs,
                parseOtherMs = parsed.parseOtherMs,
                contentCleaningMs = parsed.contentCleaningMs,
                kindSplitMs = parsed.kindSplitMs,
                categoryMs = parsed.categoryMs,
                seriesMs = parsed.seriesMs,
                classificationMs = parsed.classificationMs,
            ),
        )
    }

    private fun fetchTextWithFallback(rawUrl: String, headers: Map<String, String>): TextFetchResult {
        val normalizeStartedNs = System.nanoTime()
        val normalizedUrl = normalizeUserUrl(rawUrl)
        val normalizeMs = elapsedMs(normalizeStartedNs)
        return runCatching {
            val fetch = fetchText(normalizedUrl, headers)
            TextFetchResult(
                text = fetch.text,
                finalUrl = normalizedUrl,
                urlNormalizeMs = normalizeMs,
                connectionOpenMs = fetch.connectionOpenMs,
                downloadMs = fetch.downloadMs,
            )
        }.getOrElse { firstError ->
            val firstConnectionMs = if (firstError is TimedFetchException) firstError.connectionOpenMs else 0L
            val canRetryHttp = normalizedUrl.startsWith("https://", ignoreCase = true) &&
                firstError.isLikelyTlsForPlainHttp()
            if (!canRetryHttp) {
                throw userFacingError(rawUrl, firstError)
            }

            val httpUrl = "http://" + normalizedUrl.substringAfter("://")
            runCatching {
                val fetch = fetchText(httpUrl, headers)
                TextFetchResult(
                    text = fetch.text,
                    finalUrl = httpUrl,
                    warnings = emptyList(),
                    urlNormalizeMs = normalizeMs,
                    connectionOpenMs = firstConnectionMs + fetch.connectionOpenMs,
                    downloadMs = fetch.downloadMs,
                )
            }.getOrElse { secondError ->
                throw userFacingError(httpUrl, secondError)
            }
        }
    }

    private fun fetchText(url: String, headers: Map<String, String>): FetchPayload {
        val builder = Request.Builder().url(url)
        headers.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                builder.header(key, value)
            }
        }
        val request = builder.build()
        val connectionStartedNs = System.nanoTime()
        return try {
            client.newCall(request).execute().use { response ->
                val connectionMs = elapsedMs(connectionStartedNs)
                if (!response.isSuccessful) {
                    throw TimedFetchException(IOException("HTTP ${response.code}"), connectionMs)
                }
                val downloadStartedNs = System.nanoTime()
                val text = response.body.string()
                FetchPayload(
                    text = text,
                    connectionOpenMs = connectionMs,
                    downloadMs = elapsedMs(downloadStartedNs),
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is TimedFetchException) throw throwable
            throw TimedFetchException(throwable, elapsedMs(connectionStartedNs))
        }
    }

    private fun buildXtreamM3uUrl(
        serverUrl: String,
        username: String,
        password: String,
    ): String {
        val normalized = normalizeUserUrl(serverUrl).trimEnd('/')
        val base = when {
            normalized.contains("/get.php", ignoreCase = true) -> normalized.substringBefore("/get.php") + "/get.php"
            normalized.contains("/player_api.php", ignoreCase = true) -> normalized.substringBefore("/player_api.php") + "/get.php"
            else -> "$normalized/get.php"
        }
        return "$base?username=${username.urlEncode()}&password=${password.urlEncode()}&type=m3u_plus&output=mpegts"
    }

    private fun normalizeUserUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        return "http://$trimmed"
    }

    private fun Throwable.isLikelyTlsForPlainHttp(): Boolean {
        val text = (message ?: cause?.message).orEmpty().lowercase()
        return text.contains("tls") ||
            text.contains("ssl") ||
            text.contains("wrong version number") ||
            text.contains("not an ssl") ||
            text.contains("packet header")
    }

    private fun userFacingError(url: String, throwable: Throwable): IllegalStateException {
        val root = if (throwable is TimedFetchException) throwable.cause ?: throwable else throwable
        val detail = safeMessage(root)
        val text = when {
            root.isLikelyTlsForPlainHttp() ->
                "Bağlantı kurulamadı."

            detail.contains("HTTP 401") || detail.contains("HTTP 403") ->
                "Liste yüklenemedi. Bilgileri kontrol edin."

            detail.contains("HTTP 404") ->
                "İçerik bulunamadı."

            detail.contains("Failed to connect", ignoreCase = true) ||
                detail.contains("timeout", ignoreCase = true) ->
                "Bağlantı kurulamadı."

            else ->
                "Liste yüklenemedi."
        }
        return IllegalStateException(text, root)
    }

    private fun safeMessage(throwable: Throwable): String {
        return SecretRedactor.redact(throwable.message ?: throwable.cause?.message ?: "Bilinmeyen hata")
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

    private fun elapsedMs(startedNs: Long): Long = (System.nanoTime() - startedNs) / 1_000_000L
}

data class PlaylistLoadResult(
    val playlistName: String,
    val items: List<CatalogItem>,
    val epgUrls: List<String>,
    val warnings: List<String>,
    val metrics: PlaylistLoadMetrics = PlaylistLoadMetrics(),
)

data class PlaylistLoadMetrics(
    val totalMs: Long = 0L,
    val urlNormalizeMs: Long = 0L,
    val connectionOpenMs: Long = 0L,
    val downloadMs: Long = 0L,
    val lineReadMs: Long = 0L,
    val parseMs: Long = 0L,
    val parseOtherMs: Long = 0L,
    val contentCleaningMs: Long = 0L,
    val kindSplitMs: Long = 0L,
    val categoryMs: Long = 0L,
    val seriesMs: Long = 0L,
    val classificationMs: Long = 0L,
    val directoryMs: Long = 0L,
)

private operator fun PlaylistLoadMetrics.plus(other: PlaylistLoadMetrics): PlaylistLoadMetrics {
    return PlaylistLoadMetrics(
        totalMs = totalMs + other.totalMs,
        urlNormalizeMs = urlNormalizeMs + other.urlNormalizeMs,
        connectionOpenMs = connectionOpenMs + other.connectionOpenMs,
        downloadMs = downloadMs + other.downloadMs,
        lineReadMs = lineReadMs + other.lineReadMs,
        parseMs = parseMs + other.parseMs,
        parseOtherMs = parseOtherMs + other.parseOtherMs,
        contentCleaningMs = contentCleaningMs + other.contentCleaningMs,
        kindSplitMs = kindSplitMs + other.kindSplitMs,
        categoryMs = categoryMs + other.categoryMs,
        seriesMs = seriesMs + other.seriesMs,
        classificationMs = classificationMs + other.classificationMs,
        directoryMs = directoryMs + other.directoryMs,
    )
}

private data class TextFetchResult(
    val text: String,
    val finalUrl: String,
    val warnings: List<String> = emptyList(),
    val urlNormalizeMs: Long = 0L,
    val connectionOpenMs: Long = 0L,
    val downloadMs: Long = 0L,
)

private data class FetchPayload(
    val text: String,
    val connectionOpenMs: Long,
    val downloadMs: Long,
)

private class TimedFetchException(
    cause: Throwable,
    val connectionOpenMs: Long,
) : IOException(cause)
