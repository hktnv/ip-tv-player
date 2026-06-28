package com.hktnv.iptvbox.domain

import com.hktnv.iptvbox.core.model.PlaylistSource
import com.hktnv.iptvbox.data.playlist.CreatePlaylistSourceRequest
import com.hktnv.iptvbox.data.playlist.PlaylistSourceRepository

class AddPlaylistSourceUseCase(
    private val repository: PlaylistSourceRepository,
) {
    operator fun invoke(request: CreatePlaylistSourceRequest): Result<PlaylistSource> {
        return runCatching { repository.add(request) }
    }
}
