package com.hktnv.iptvbox.data.playlist

data class PlaylistLoadProgress(
    val stage: PlaylistLoadStage,
    val processedItems: Int = 0,
    val totalItems: Int? = null,
)

enum class PlaylistLoadStage {
    CONNECTING,
    DOWNLOADING,
    READING,
    PREPARING,
    WRITING,
    COMPLETED,
}
