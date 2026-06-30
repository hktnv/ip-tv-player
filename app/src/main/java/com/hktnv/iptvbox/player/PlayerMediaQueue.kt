package com.hktnv.iptvbox.player

import androidx.media3.common.MediaItem

internal data class PlayerMediaQueue(
    val signature: String,
    val currentIndex: Int,
    val mediaItems: List<MediaItem>,
)

internal fun PlayerPlaybackQueue.toPlayerMediaQueue(mediaItems: List<MediaItem>): PlayerMediaQueue {
    return PlayerMediaQueue(
        signature = toMediaQueueSignature(),
        currentIndex = currentIndex,
        mediaItems = mediaItems,
    )
}

internal fun PlayerPlaybackQueue.toMediaQueueSignature(): String {
    var hash = 1125899906842597L
    items.forEach { item ->
        hash = 31 * hash + item.id.hashCode()
        hash = 31 * hash + item.streamUrl.hashCode()
    }
    return "${items.size}:$hash"
}
