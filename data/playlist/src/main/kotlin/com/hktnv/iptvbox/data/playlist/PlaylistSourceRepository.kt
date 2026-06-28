package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.ContentHint
import com.hktnv.iptvbox.core.model.PlaylistSource
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.core.model.SourceSyncStatus
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface PlaylistSourceRepository {
    val sources: Flow<List<PlaylistSource>>

    fun add(request: CreatePlaylistSourceRequest): PlaylistSource
}

class InMemoryPlaylistSourceRepository(
    private val clock: Clock = Clock.systemUTC(),
) : PlaylistSourceRepository {
    private val mutableSources = MutableStateFlow<List<PlaylistSource>>(emptyList())
    override val sources: Flow<List<PlaylistSource>> = mutableSources

    override fun add(request: CreatePlaylistSourceRequest): PlaylistSource {
        request.validate()
        val now = clock.millis()
        val id = UUID.randomUUID().toString()
        val source = PlaylistSource(
            id = id,
            name = request.name.trim().ifBlank { "Oynatma Listesi" },
            type = request.type,
            endpoint = request.endpoint.trim(),
            epgUrl = request.epgUrl?.trim()?.ifBlank { null },
            headers = request.headers.filterValues { it.isNotBlank() },
            credentialRef = request.credentialRef(id),
            enabled = true,
            contentHint = request.contentHint,
            status = SourceSyncStatus.Idle,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        mutableSources.update { current -> current + source }
        return source
    }
}

data class CreatePlaylistSourceRequest(
    val type: PlaylistSourceType,
    val name: String,
    val endpoint: String,
    val epgUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val contentHint: ContentHint = ContentHint.AUTO,
    val xtreamUsername: String? = null,
    val xtreamPassword: String? = null,
)

class SourceValidationException(message: String) : IllegalArgumentException(message)

private fun CreatePlaylistSourceRequest.validate() {
    if (!endpoint.isHttpUrl()) {
        throw SourceValidationException("URL http:// veya https:// ile başlamalı.")
    }
    if (!epgUrl.isNullOrBlank() && !epgUrl.isHttpUrl()) {
        throw SourceValidationException("Program rehberi adresi http:// veya https:// ile başlamalı.")
    }
    if (type == PlaylistSourceType.XTREAM) {
        if (xtreamUsername.isNullOrBlank()) {
            throw SourceValidationException("Xtream kullanıcı adı zorunlu.")
        }
        if (xtreamPassword.isNullOrBlank()) {
            throw SourceValidationException("Xtream parolası zorunlu.")
        }
    }
}

private fun CreatePlaylistSourceRequest.credentialRef(sourceId: String): String? {
    return when (type) {
        PlaylistSourceType.XTREAM -> "keystore://xtream/$sourceId"
        PlaylistSourceType.JSON_DIRECTORY,
        PlaylistSourceType.M3U_URL -> null
    }
}
