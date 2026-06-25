package com.evomrdm.iptvbox.core.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.evomrdm.iptvbox.core.model.PlaybackCapabilities
import com.evomrdm.iptvbox.core.model.PlaybackRequest

object MediaPlayerFactory {
    fun create(
        context: Context,
        headers: Map<String, String> = emptyMap(),
    ): ExoPlayer {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers.filterValues { it.isNotBlank() })
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
    }
}

fun PlaybackRequest.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(url)
        .setMediaId(itemId)
        .setTag(this)
        .build()
}

fun Player.currentCapabilities(): PlaybackCapabilities {
    val tracks = currentTracks
    val videoGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
    val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    return PlaybackCapabilities(
        hasMultipleVideoTracks = videoGroups.sumOf { it.length } > 1,
        hasAudioTracks = audioGroups.any { it.length > 0 },
        hasSubtitleTracks = textGroups.any { it.length > 0 },
        isSeekable = isCurrentMediaItemSeekable,
        hasLiveWindow = isCurrentMediaItemLive && isCurrentMediaItemDynamic,
    )
}
