package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.core.network.HttpClientFactory
import com.hktnv.iptvbox.core.security.SecretRedactor
import com.hktnv.iptvbox.data.playlist.xtream.XtreamApiClient
import com.hktnv.iptvbox.data.playlist.xtream.XtreamPlaylistEnricher
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RemotePlaylistLoader(
    private val client: OkHttpClient = HttpClientFactory.create(),
    private val directoryParser: PlaylistDirectoryParser = PlaylistDirectoryParser(),
    private val m3uParser: M3uPlaylistParser = M3uPlaylistParser(),
    private val xtreamEnricher: XtreamPlaylistEnricher = XtreamPlaylistEnricher(XtreamApiClient(client)),
) {
    suspend fun load(
        playlistId: String,
        request: CreatePlaylistSourceRequest,
        expectedItemCount: Int? = null,
        onProgress: (PlaylistLoadProgress) -> Unit = {},
    ): PlaylistLoadResult = withContext(Dispatchers.IO) {
        val startedNs = System.nanoTime()
        onProgress(PlaylistLoadProgress(PlaylistLoadStage.CONNECTING, totalItems = expectedItemCount))
        val result = when (request.type) {
            PlaylistSourceType.M3U_URL -> loadM3u(
                playlistId = playlistId,
                name = request.name,
                url = request.endpoint,
                headers = request.headers,
                expectedItemCount = expectedItemCount,
                onProgress = onProgress,
            )

            PlaylistSourceType.JSON_DIRECTORY -> loadDirectory(playlistId, request, expectedItemCount, onProgress)

            PlaylistSourceType.XTREAM -> loadM3u(
                playlistId = playlistId,
                name = request.name,
                url = buildXtreamM3uUrl(
                    serverUrl = request.endpoint,
                    username = request.xtreamUsername.orEmpty(),
                    password = request.xtreamPassword.orEmpty(),
                ),
                headers = request.headers,
                expectedItemCount = expectedItemCount,
                onProgress = onProgress,
            )
        }
        onProgress(
            PlaylistLoadProgress(
                stage = PlaylistLoadStage.COMPLETED,
                processedItems = result.items.size,
                totalItems = result.items.size,
            ),
        )
        result.copy(metrics = result.metrics.copy(totalMs = elapsedMs(startedNs)))
    }

    private fun loadDirectory(
        playlistId: String,
        request: CreatePlaylistSourceRequest,
        expectedItemCount: Int?,
        onProgress: (PlaylistLoadProgress) -> Unit,
    ): PlaylistLoadResult {
        val directoryStartedNs = System.nanoTime()
        onProgress(PlaylistLoadProgress(PlaylistLoadStage.DOWNLOADING, totalItems = expectedItemCount))
        val directoryFetch = fetchTextWithFallback(request.endpoint, request.headers)
        val candidates = directoryParser.parse(directoryFetch.text)
        val warnings = directoryFetch.warnings.toMutableList()
        val allItems = mutableListOf<CatalogItem>()
        val epgUrls = linkedSetOf<String>()
        val xtreamCategoryMappings = mutableListOf<com.hktnv.iptvbox.data.playlist.xtream.XtreamCategoryMapping>()
        var xtreamSupported = false
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
                    expectedItemCount = expectedItemCount,
                    onProgress = onProgress,
                )
            }.onSuccess { loaded ->
                epgUrls += loaded.epgUrls
                allItems += loaded.items.map { it.copy(sourceId = playlistId) }
                xtreamCategoryMappings += loaded.xtreamCategoryMappings
                xtreamSupported = xtreamSupported || loaded.xtreamApiSupported
                warnings += loaded.warnings
                metrics += loaded.metrics
            }.onFailure { throwable ->
                warnings += "${candidate.name}: ${safeMessage(throwable)}"
            }
        }

        if (allItems.isEmpty()) {
            error("İçerik bulunamadı. Liste indirildi ama içinde oynatılabilir kanal, film veya dizi yok.")
        }

        return PlaylistLoadResult(
            playlistName = request.name,
            items = allItems.sortedBy { it.providerOrder },
            epgUrls = epgUrls.toList(),
            warnings = warnings,
            metrics = metrics.copy(directoryMs = elapsedMs(directoryStartedNs)),
            xtreamApiSupported = xtreamSupported,
            xtreamCategoryMappings = xtreamCategoryMappings,
        )
    }

    private fun loadM3u(
        playlistId: String,
        name: String,
        url: String,
        headers: Map<String, String>,
        expectedItemCount: Int?,
        onProgress: (PlaylistLoadProgress) -> Unit,
    ): PlaylistLoadResult {
        val fetch = fetchM3uWithFallback(playlistId, url, headers, expectedItemCount, onProgress)
        val parsed = fetch.parsed
        if (parsed.items.isEmpty()) {
            error("İçerik bulunamadı. Liste indirildi ama içinde oynatılabilir kanal, film veya dizi yok.")
        }
        onProgress(
            PlaylistLoadProgress(
                stage = PlaylistLoadStage.PREPARING,
                processedItems = parsed.items.size,
                totalItems = parsed.items.size,
            ),
        )
        val enriched = xtreamEnricher.enrich(url, parsed.items)
        return PlaylistLoadResult(
            playlistName = name,
            items = enriched.items,
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
            xtreamApiSupported = enriched.supported,
            xtreamCategoryMappings = enriched.categoryMappings,
        )
    }

    private fun fetchM3uWithFallback(
        playlistId: String,
        rawUrl: String,
        headers: Map<String, String>,
        expectedItemCount: Int?,
        onProgress: (PlaylistLoadProgress) -> Unit,
    ): M3uFetchResult {
        val normalizeStartedNs = System.nanoTime()
        val normalizedUrl = normalizeUserUrl(rawUrl)
        val normalizeMs = elapsedMs(normalizeStartedNs)
        return runCatching {
            val fetch = fetchM3u(normalizedUrl, headers, playlistId, expectedItemCount, onProgress)
            M3uFetchResult(
                parsed = fetch.parsed,
                urlNormalizeMs = normalizeMs,
                connectionOpenMs = fetch.connectionOpenMs,
                downloadMs = fetch.downloadMs,
            )
        }.getOrElse { firstError ->
            val firstConnectionMs = if (firstError is TimedFetchException) firstError.connectionOpenMs else 0L
            val canRetryHttp = normalizedUrl.startsWith("https://", ignoreCase = true) &&
                firstError.isLikelyTlsForPlainHttp()
            if (!canRetryHttp) {
                throw playlistLoadError(rawUrl, firstError)
            }

            val httpUrl = "http://" + normalizedUrl.substringAfter("://")
            runCatching {
                val fetch = fetchM3u(httpUrl, headers, playlistId, expectedItemCount, onProgress)
                M3uFetchResult(
                    parsed = fetch.parsed,
                    urlNormalizeMs = normalizeMs,
                    connectionOpenMs = firstConnectionMs + fetch.connectionOpenMs,
                    downloadMs = fetch.downloadMs,
                )
            }.getOrElse { secondError ->
                throw playlistLoadError(httpUrl, secondError)
            }
        }
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
                throw playlistLoadError(rawUrl, firstError)
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
                throw playlistLoadError(httpUrl, secondError)
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

    private fun fetchM3u(
        url: String,
        headers: Map<String, String>,
        playlistId: String,
        expectedItemCount: Int?,
        onProgress: (PlaylistLoadProgress) -> Unit,
    ): M3uFetchPayload {
        val builder = Request.Builder().url(url)
        headers.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                builder.header(key, value)
            }
        }
        val request = builder.build()
        val connectionStartedNs = System.nanoTime()
        onProgress(PlaylistLoadProgress(PlaylistLoadStage.DOWNLOADING, totalItems = expectedItemCount))
        return try {
            client.newCall(request).execute().use { response ->
                val connectionMs = elapsedMs(connectionStartedNs)
                if (!response.isSuccessful) {
                    throw TimedFetchException(IOException("HTTP ${response.code}"), connectionMs)
                }
                onProgress(PlaylistLoadProgress(PlaylistLoadStage.READING, totalItems = expectedItemCount))
                val responseCharset = response.body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val parsed = response.body.byteStream()
                    .useM3uLines(responseCharset) { lines ->
                        m3uParser.parse(playlistId, lines) { count ->
                            onProgress(
                                PlaylistLoadProgress(
                                    stage = PlaylistLoadStage.READING,
                                    processedItems = count,
                                    totalItems = expectedItemCount,
                                ),
                            )
                        }
                    }
                M3uFetchPayload(
                    parsed = parsed,
                    connectionOpenMs = connectionMs,
                    downloadMs = 0L,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is TimedFetchException) throw throwable
            throw TimedFetchException(throwable, elapsedMs(connectionStartedNs))
        }
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

    private fun elapsedMs(startedNs: Long): Long = (System.nanoTime() - startedNs) / 1_000_000L
}
