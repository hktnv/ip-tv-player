@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.hktnv.iptvbox.player

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.io.IOException

private const val PLAYER_DIAGNOSTIC_TAG = "[PLAYER_DIAGNOSTIC]"

internal class PlayerDiagnosticLogger(
    private val context: PlayerDiagnosticContext,
    private val manualPauseProvider: () -> Boolean,
    private val bufferSnapshotProvider: () -> PlayerBufferDiagnosticSnapshot,
) : AnalyticsListener {
    private val session = PlayerDiagnosticSession()
    private var lastPlayWhenReady = false
    private var channelSwitchStartedAtMs = 0L

    fun logAttached() {
        log("event=diagnostic_attached")
    }

    fun logDetached() {
        session.flushDroppedVideoFrames(SystemClock.elapsedRealtime())?.let(::logDroppedFrameEvent)
        log("event=diagnostic_detached")
    }

    fun syncPlaybackState(playWhenReady: Boolean) {
        lastPlayWhenReady = playWhenReady
    }

    fun logSeekRequest(
        targetMs: Long,
        canSeek: Boolean,
        source: String,
    ) {
        log("event=seek_request target_ms=$targetMs can_seek=$canSeek source=$source")
    }

    fun logChannelSwitchStart(targetItemId: String) {
        channelSwitchStartedAtMs = SystemClock.elapsedRealtime()
        log("event=channel_switch_start target_item_id=${targetItemId.safeLogValue()} ${bufferFields()}")
    }

    fun logChannelSwitchReady(durationMs: Long) {
        log("event=channel_switch_ready duration_ms=$durationMs ${bufferFields()}")
    }

    fun logMediaSwitchPlan(
        plan: String,
        index: Int,
        queueSize: Int,
    ) {
        log("event=media_switch_plan plan=${plan.quoted()} index=$index queue_size=$queueSize ${bufferFields()}")
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        log(
            "event=state_change state=${state.diagnosticName()} " +
                "play_when_ready=$lastPlayWhenReady manual_pause=${manualPauseProvider()} ${bufferFields()}",
        )
        session.onPlaybackStateChanged(state, SystemClock.elapsedRealtime())?.let { event ->
            log(
                "event=buffering_end duration_ms=${event.durationMs} " +
                    "buffering_count=${event.count} total_buffering_ms=${event.totalDurationMs} " +
                    bufferFields(),
            )
        }
        if (state == Player.STATE_BUFFERING) {
            log("event=buffering_start manual_pause=${manualPauseProvider()} ${bufferFields()}")
        }
    }

    override fun onPlayWhenReadyChanged(
        eventTime: AnalyticsListener.EventTime,
        playWhenReady: Boolean,
        reason: Int,
    ) {
        lastPlayWhenReady = playWhenReady
        log(
            "event=play_when_ready_changed play_when_ready=$playWhenReady reason=$reason " +
                "manual_pause=${manualPauseProvider()}",
        )
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        log(
            "event=player_error code=${error.errorCode} code_name=${error.errorCodeName} " +
                "exception=${error::class.java.name} message=${error.message.safeLogValue()}",
        )
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean,
    ) {
        log(
            "event=load_error data_type=${mediaLoadData.dataType} track_type=${mediaLoadData.trackType} " +
                "elapsed_ms=${loadEventInfo.loadDurationMs} bytes=${loadEventInfo.bytesLoaded} " +
                "canceled=$wasCanceled uri=${loadEventInfo.uri.toString().toDiagnosticMediaHint()} " +
                "exception=${error::class.java.name} message=${error.message.safeLogValue()}",
        )
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
    ) {
        if (mediaLoadData.dataType != C.DATA_TYPE_MEDIA || loadEventInfo.bytesLoaded < LOAD_LOG_MIN_BYTES) {
            return
        }
        log(
            "event=load_completed data_type=${mediaLoadData.dataType.dataTypeName()} " +
                "track_type=${mediaLoadData.trackType.trackTypeName()} " +
                "duration_ms=${loadEventInfo.loadDurationMs} bytes=${loadEventInfo.bytesLoaded} " +
                "throughput_kbps=${loadEventInfo.throughputKbps()} " +
                "uri=${loadEventInfo.uri.toString().toDiagnosticMediaHint()} ${bufferFields()}",
        )
    }

    override fun onBandwidthEstimate(
        eventTime: AnalyticsListener.EventTime,
        totalLoadTimeMs: Int,
        totalBytesLoaded: Long,
        bitrateEstimate: Long,
    ) {
        log(
            "event=bandwidth_estimate load_time_ms=$totalLoadTimeMs bytes=$totalBytesLoaded " +
                "bitrate_kbps=${(bitrateEstimate / 1_000).coerceAtLeast(0L)} ${bufferFields()}",
        )
    }

    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {
        log("event=seek_started position_ms=${eventTime.eventPlaybackPositionMs}")
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long,
    ) {
        session.onDroppedVideoFrames(
            droppedFrames = droppedFrames,
            elapsedMs = elapsedMs,
            nowMs = SystemClock.elapsedRealtime(),
        )?.let(::logDroppedFrameEvent)
    }

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long,
    ) {
        val durationMs = if (channelSwitchStartedAtMs > 0L) {
            SystemClock.elapsedRealtime() - channelSwitchStartedAtMs
        } else {
            0L
        }
        log("event=rendered_first_frame duration_ms=$durationMs render_time_ms=$renderTimeMs ${bufferFields()}")
    }

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        log(
            "event=video_decoder_initialized decoder=${decoderName.safeLogValue()} " +
                "init_duration_ms=$initializationDurationMs",
        )
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        log(
            "event=video_format width=${format.width} height=${format.height} " +
                "frame_rate=${format.frameRate} sample_mime=${format.sampleMimeType.safeLogValue()} " +
                "bitrate=${format.bitrate}",
        )
    }

    override fun onVideoCodecError(eventTime: AnalyticsListener.EventTime, videoCodecError: Exception) {
        log(
            "event=video_codec_error exception=${videoCodecError::class.java.name} " +
                "message=${videoCodecError.message.safeLogValue()}",
        )
    }

    private fun log(message: String) {
        Log.i(
            PLAYER_DIAGNOSTIC_TAG,
            "$message type=${context.type.quoted()} category=${context.category.quoted()} " +
                "title=${context.title.quoted()} media=${context.media.quoted()} " +
                "ui_mode=${context.uiMode.quoted()}",
        )
    }

    private fun logDroppedFrameEvent(event: DroppedFrameDiagnosticEvent) {
        log(
            "event=dropped_frames count=${event.count} elapsed_ms=${event.elapsedMs} " +
                "window_ms=${event.windowMs} ${bufferFields()}",
        )
    }

    private fun bufferFields(): String {
        val snapshot = bufferSnapshotProvider()
        return "position_ms=${snapshot.positionMs} buffered_position_ms=${snapshot.bufferedPositionMs} " +
            "buffered_duration_ms=${snapshot.bufferedDurationMs} buffered_percent=${snapshot.bufferedPercent}"
    }

    private companion object {
        const val LOAD_LOG_MIN_BYTES = 32 * 1024
    }
}

internal data class PlayerBufferDiagnosticSnapshot(
    val positionMs: Long,
    val bufferedPositionMs: Long,
    val bufferedDurationMs: Long,
    val bufferedPercent: Int,
)

internal fun Player.toBufferDiagnosticSnapshot(): PlayerBufferDiagnosticSnapshot {
    return PlayerBufferDiagnosticSnapshot(
        positionMs = currentPosition.coerceAtLeast(0L),
        bufferedPositionMs = bufferedPosition.coerceAtLeast(0L),
        bufferedDurationMs = totalBufferedDuration.coerceAtLeast(0L),
        bufferedPercent = bufferedPercentage.coerceIn(0, 100),
    )
}

internal fun Int.diagnosticName(): String {
    return when (this) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN_$this"
    }
}

private fun String?.safeLogValue(): String {
    return orEmpty()
        .replace('\n', ' ')
        .replace('\r', ' ')
        .take(180)
        .quoted()
}

private fun String.quoted(): String {
    return "\"${replace("\"", "'")}\""
}

private fun LoadEventInfo.throughputKbps(): Long {
    if (loadDurationMs <= 0L) return 0L
    return (bytesLoaded * 8L / loadDurationMs).coerceAtLeast(0L)
}

private fun Int.dataTypeName(): String {
    return when (this) {
        C.DATA_TYPE_MEDIA -> "MEDIA"
        C.DATA_TYPE_MANIFEST -> "MANIFEST"
        C.DATA_TYPE_DRM -> "DRM"
        else -> "TYPE_$this"
    }
}

private fun Int.trackTypeName(): String {
    return when (this) {
        C.TRACK_TYPE_VIDEO -> "VIDEO"
        C.TRACK_TYPE_AUDIO -> "AUDIO"
        C.TRACK_TYPE_TEXT -> "TEXT"
        C.TRACK_TYPE_UNKNOWN -> "UNKNOWN"
        else -> "TRACK_$this"
    }
}
