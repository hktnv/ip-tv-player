package com.evomrdm.iptvbox.data.playlist

import com.evomrdm.iptvbox.core.model.ContentHint
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistDirectoryParserTest {
    @Test
    fun parsesEnabledDirectoryEntries() {
        val result = PlaylistDirectoryParser().parse(
            """
            [
              {
                "name": "Canli TV",
                "url": "https://example.com/live.m3u",
                "epgUrl": "https://example.com/epg.xml.gz",
                "headers": {"User-Agent": "IPTV"},
                "contentHint": "live"
              },
              {
                "name": "Kapali",
                "url": "https://example.com/disabled.m3u",
                "enabled": false
              }
            ]
            """.trimIndent(),
        )

        assertEquals(1, result.size)
        assertEquals("Canli TV", result.single().name)
        assertEquals("https://example.com/epg.xml.gz", result.single().epgUrl)
        assertEquals(ContentHint.LIVE, result.single().contentHint)
    }
}
