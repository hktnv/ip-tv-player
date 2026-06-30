package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.player.IptvPlaybackBufferKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackBufferKindMapperTest {
    @Test
    fun liveAndRadioUseLiveBufferProfile() {
        assertEquals(IptvPlaybackBufferKind.LIVE, item(ContentKind.LIVE_CHANNEL).toPlaybackBufferKind())
        assertEquals(IptvPlaybackBufferKind.LIVE, item(ContentKind.RADIO).toPlaybackBufferKind())
    }

    @Test
    fun vodKindsUseVodBufferProfile() {
        assertEquals(IptvPlaybackBufferKind.VOD, item(ContentKind.MOVIE).toPlaybackBufferKind())
        assertEquals(IptvPlaybackBufferKind.VOD, item(ContentKind.SERIES).toPlaybackBufferKind())
        assertEquals(IptvPlaybackBufferKind.VOD, item(ContentKind.SEASON).toPlaybackBufferKind())
        assertEquals(IptvPlaybackBufferKind.VOD, item(ContentKind.EPISODE).toPlaybackBufferKind())
    }

    private fun item(kind: ContentKind): CatalogItem {
        return CatalogItem(
            id = kind.name,
            sourceId = "source",
            kind = kind,
            title = kind.name,
            streamUrl = "https://example.test/stream",
        )
    }
}
