package com.hktnv.iptvbox.core.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.hktnv.iptvbox.core.model.PlaybackCapabilities
import com.hktnv.iptvbox.core.model.PlaybackRequest

object MediaPlayerFactory {
    @OptIn(UnstableApi::class)
    fun create(
        context: Context,
        headers: Map<String, String> = emptyMap(),
    ): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setRenderersFactory(createIptvRenderersFactory(context))
            .setMediaSourceFactory(createIptvMediaSourceFactory(context, headers))
            .setLoadControl(defaultIptvPlayerBufferProfile().toLoadControl())
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
