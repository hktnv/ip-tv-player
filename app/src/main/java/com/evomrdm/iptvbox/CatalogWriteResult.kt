package com.evomrdm.iptvbox

internal data class CatalogWriteResult(
    val playlist: LoadedPlaylist,
    val timings: Map<String, Long>,
)
