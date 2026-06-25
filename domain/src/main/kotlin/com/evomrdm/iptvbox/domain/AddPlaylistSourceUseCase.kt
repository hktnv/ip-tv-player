package com.evomrdm.iptvbox.domain

import com.evomrdm.iptvbox.core.model.PlaylistSource
import com.evomrdm.iptvbox.data.playlist.CreatePlaylistSourceRequest
import com.evomrdm.iptvbox.data.playlist.PlaylistSourceRepository

class AddPlaylistSourceUseCase(
    private val repository: PlaylistSourceRepository,
) {
    operator fun invoke(request: CreatePlaylistSourceRequest): Result<PlaylistSource> {
        return runCatching { repository.add(request) }
    }
}
