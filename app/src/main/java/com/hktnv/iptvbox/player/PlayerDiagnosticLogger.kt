@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.hktnv.iptvbox.player

import android.os.SystemClock
import android.util.Log
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
) : AnalyticsListener {
    private val session = PlayerDiagnosticSession()
    private var lastPlayWhenReady = false

    fun logAttached() {
        log("event=diagnostic_attached")
    }

    fun logDetached() {
        log("event=diagnostic_detached")
    }

    fun logSeekRequest(
        targetMs: Long,
        canSeek: Boolean,
        source: String,
    ) {
        log("event=seek_request target_ms=$targetMs can_seek=$canSeek source=$source")
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        log(
            "event=state_change state=${state.diagnosticName()} " +
                "play_when_ready=$lastPlayWhenReady manual_pause=${manualPauseProvider()}",
        )
        session.onPlaybackStateChanged(state, SystemClock.elapsedRealtime())?.let { event ->
            log(
                "event=buffering_end duration_ms=${event.durationMs} " +
                    "buffering_count=${event.count} total_buffering_ms=${event.totalDurationMs}",
            )
        }
        if (state == Player.STATE_BUFFERING) {
            log("event=buffering_start manual_pause=${manualPauseProvider()}")
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

    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {
        log("event=seek_started position_ms=${eventTime.eventPlaybackPositionMs}")
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long,
    ) {
        log("event=dropped_frames count=$droppedFrames elapsed_ms=$elapsedMs")
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
                "sample_mime=${format.sampleMimeType.safeLogValue()} bitrate=${format.bitrate}",
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
                "title=${context.title.quoted()} media=${context.media.quoted()}",
        )
    }
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
