package com.hktnv.iptvbox.telemetry
import android.os.SystemClock
import com.hktnv.iptvbox.data.playlist.PlaylistLoadMetrics

internal fun recordPlaylistLoadMetrics(
    telemetry: AppPerformanceTelemetry,
    metrics: PlaylistLoadMetrics,
) {
    telemetry.recordMany(metrics.toTelemetryMap())
}

internal fun recordCatalogWriteTimings(
    telemetry: AppPerformanceTelemetry,
    timings: Map<String, Long>,
) {
    telemetry.recordMany(timings.toDbTelemetryMap())
}

internal fun finishPlaylistImportTelemetry(
    telemetry: AppPerformanceTelemetry,
    importStartedAtMs: Long,
    firstResponseMs: Long,
    metrics: PlaylistLoadMetrics,
    dbTimings: Map<String, Long>,
    normalizeMs: Long,
    uiUpdateMs: Long,
    itemCount: Int,
) {
    val totalMs = (SystemClock.elapsedRealtime() - importStartedAtMs).coerceAtLeast(0L)
    val dbTotalMs = dbTimings["total_ms"] ?: 0L
    val imageMs = 0L
    val loaderOtherMs = metrics.loaderOtherMs()
    val knownMs = firstResponseMs +
        metrics.urlNormalizeMs +
        metrics.connectionOpenMs +
        metrics.downloadMs +
        metrics.lineReadMs +
        metrics.contentCleaningMs +
        metrics.kindSplitMs +
        metrics.categoryMs +
        metrics.seriesMs +
        metrics.parseOtherMs +
        loaderOtherMs +
        normalizeMs +
        dbTotalMs +
        imageMs +
        uiUpdateMs
    val idleWaitMs = (totalMs - knownMs).coerceAtLeast(0L)
    val overAccountedMs = (knownMs - totalMs).coerceAtLeast(0L)

    telemetry.recordMany(
        numbers = metrics.toTelemetryMap() +
            dbTimings.toDbTelemetryMap() +
            mapOf(
                "playlist_import_press_to_response_ms" to firstResponseMs,
                "playlist_import_known_phase_sum_ms" to knownMs,
                "playlist_import_loader_other_ms" to loaderOtherMs,
                "playlist_import_normalize_ms" to normalizeMs,
                "playlist_import_image_ms" to imageMs,
                "playlist_import_ui_update_ms" to uiUpdateMs,
                "playlist_import_idle_wait_ms" to idleWaitMs,
                "playlist_import_gc_memory_suspect_ms" to 0L,
                "playlist_import_unmeasured_gap_ms" to 0L,
                "playlist_import_overaccounted_ms" to overAccountedMs,
                "playlist_import_total_ms" to totalMs,
                "playlist_import_item_count" to itemCount.toLong(),
            ),
        texts = mapOf(
            "playlist_import_ui_locked" to "measured-by-watch",
        ),
    )
}

private fun PlaylistLoadMetrics.toTelemetryMap(): Map<String, Long> {
    return mapOf(
        "playlist_loader_total_ms" to totalMs,
        "playlist_import_url_normalize_ms" to urlNormalizeMs,
        "playlist_import_connection_open_ms" to connectionOpenMs,
        "playlist_import_download_ms" to downloadMs,
        "playlist_import_line_read_ms" to lineReadMs,
        "playlist_import_parse_ms" to parseMs,
        "playlist_import_parse_other_ms" to parseOtherMs,
        "playlist_import_loader_other_ms" to loaderOtherMs(),
        "playlist_import_content_cleaning_ms" to contentCleaningMs,
        "playlist_import_kind_split_ms" to kindSplitMs,
        "playlist_import_category_extract_ms" to categoryMs,
        "playlist_import_series_extract_ms" to seriesMs,
        "playlist_import_classification_ms" to classificationMs,
        "playlist_import_directory_ms" to directoryMs,
    )
}

private fun PlaylistLoadMetrics.loaderOtherMs(): Long {
    val known = urlNormalizeMs + connectionOpenMs + downloadMs + parseMs + directoryMs
    return (totalMs - known).coerceAtLeast(0L)
}

private fun Map<String, Long>.toDbTelemetryMap(): Map<String, Long> {
    return entries.associate { (key, value) -> "playlist_import_db_$key" to value }
}
