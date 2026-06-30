package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.player.IptvPlaybackBufferKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlayerSessionTest {
    @Test
    fun sameHeadersAndLiveProfileReuseSamePlayerSessionAcrossChannels() {
        val headers = mapOf("User-Agent" to "IPTV")
        val first = item(id = "trt-1", kind = ContentKind.LIVE_CHANNEL)
        val second = item(id = "trt-2", kind = ContentKind.LIVE_CHANNEL)

        assertEquals(playerSessionKey(headers, first), playerSessionKey(headers, second))
    }

    @Test
    fun sameHeadersAndVodProfileReuseSamePlayerSessionAcrossVodItems() {
        val headers = mapOf("User-Agent" to "IPTV")
        val movie = item(id = "movie", kind = ContentKind.MOVIE)
        val episode = item(id = "episode", kind = ContentKind.EPISODE)

        assertEquals(playerSessionKey(headers, movie), playerSessionKey(headers, episode))
    }

    @Test
    fun liveAndVodProfilesUseSeparatePlayerSessions() {
        val headers = mapOf("User-Agent" to "IPTV")
        val live = item(id = "live", kind = ContentKind.LIVE_CHANNEL)
        val movie = item(id = "movie", kind = ContentKind.MOVIE)

        assertNotEquals(playerSessionKey(headers, live), playerSessionKey(headers, movie))
        assertEquals(IptvPlaybackBufferKind.LIVE, playerSessionKey(headers, live).bufferKind)
        assertEquals(IptvPlaybackBufferKind.VOD, playerSessionKey(headers, movie).bufferKind)
    }

    @Test
    fun headerChangesUseSeparatePlayerSessions() {
        val item = item(id = "live", kind = ContentKind.LIVE_CHANNEL)

        assertNotEquals(
            playerSessionKey(mapOf("User-Agent" to "IPTV"), item),
            playerSessionKey(mapOf("User-Agent" to "Other"), item),
        )
    }

    private fun item(
        id: String,
        kind: ContentKind,
    ): CatalogItem {
        return CatalogItem(
            id = id,
            sourceId = "playlist",
            kind = kind,
            title = id,
            streamUrl = "https://example.test/$id",
        )
    }
}
