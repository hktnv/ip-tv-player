package com.hktnv.iptvbox.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackRequest(
    val itemId: String,
    val title: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val resumePositionMillis: Long = 0L,
    val live: Boolean = false,
)

@Serializable
data class PlaybackCapabilities(
    val hasMultipleVideoTracks: Boolean = false,
    val hasAudioTracks: Boolean = false,
    val hasSubtitleTracks: Boolean = false,
    val isSeekable: Boolean = false,
    val hasLiveWindow: Boolean = false,
)
