package com.hktnv.iptvbox.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaylistSourceType {
    JSON_DIRECTORY,
    M3U_URL,
    XTREAM,
}

@Serializable
data class PlaylistSource(
    val id: String,
    val name: String,
    val type: PlaylistSourceType,
    val endpoint: String,
    val epgUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val credentialRef: String? = null,
    val enabled: Boolean = true,
    val contentHint: ContentHint = ContentHint.AUTO,
    val status: SourceSyncStatus = SourceSyncStatus.Idle,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
sealed interface SourceSyncStatus {
    @Serializable
    data object Idle : SourceSyncStatus

    @Serializable
    data class Refreshing(val startedAtEpochMillis: Long) : SourceSyncStatus

    @Serializable
    data class Success(
        val refreshedAtEpochMillis: Long,
        val itemCount: Int,
    ) : SourceSyncStatus

    @Serializable
    data class Failed(
        val failedAtEpochMillis: Long,
        val message: String,
        val canRetry: Boolean = true,
    ) : SourceSyncStatus
}
