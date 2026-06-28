package com.hktnv.iptvbox.data.catalog

import com.hktnv.iptvbox.model.LoadedPlaylist



internal data class CatalogWriteResult(
    val playlist: LoadedPlaylist,
    val timings: Map<String, Long>,
)
