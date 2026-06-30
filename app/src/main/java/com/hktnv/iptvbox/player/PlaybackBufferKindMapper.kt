package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.player.IptvPlaybackBufferKind

internal fun CatalogItem.toPlaybackBufferKind(): IptvPlaybackBufferKind {
    return when (kind) {
        ContentKind.LIVE_CHANNEL,
        ContentKind.RADIO -> IptvPlaybackBufferKind.LIVE
        ContentKind.MOVIE,
        ContentKind.SERIES,
        ContentKind.SEASON,
        ContentKind.EPISODE -> IptvPlaybackBufferKind.VOD
    }
}
