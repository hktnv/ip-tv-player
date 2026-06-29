package com.hktnv.iptvbox

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.player.PlayerExitDialogState
import com.hktnv.iptvbox.player.shouldShowPlayerContentInfo
import com.hktnv.iptvbox.player.toPlayerContentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerContentInfoTest {
    @Test
    fun mapsLiveContentInfo() {
        val info = catalogItem(ContentKind.LIVE_CHANNEL, "TRT 1 HD", "Türk Ulusal").toPlayerContentInfo()

        assertEquals("Canlı TV", info.typeLabel)
        assertEquals("Türk Ulusal", info.category)
        assertEquals("TRT 1 HD", info.title)
    }

    @Test
    fun mapsMovieContentInfo() {
        val info = catalogItem(ContentKind.MOVIE, "Pressure (2026)", "Yeni Filmler").toPlayerContentInfo()

        assertEquals("Film", info.typeLabel)
        assertEquals("Yeni Filmler", info.category)
        assertEquals("Pressure (2026)", info.title)
    }

    @Test
    fun mapsEpisodeContentInfoAsSeries() {
        val info = catalogItem(ContentKind.EPISODE, "Dune S1 E1", "BluTV").toPlayerContentInfo()

        assertEquals("Dizi", info.typeLabel)
        assertEquals("BluTV", info.category)
        assertEquals("Dune S1 E1", info.title)
    }

    @Test
    fun contentInfoVisibilityFollowsControlsAndDialog() {
        assertTrue(shouldShowPlayerContentInfo(true, PlayerExitDialogState.Hidden))
        assertFalse(shouldShowPlayerContentInfo(false, PlayerExitDialogState.Hidden))
        assertFalse(shouldShowPlayerContentInfo(true, PlayerExitDialogState.Visible))
    }

    private fun catalogItem(
        kind: ContentKind,
        title: String,
        category: String,
    ): CatalogItem {
        return CatalogItem(
            id = "$kind-$title",
            sourceId = "playlist",
            kind = kind,
            title = title,
            streamUrl = "https://example.test/stream",
            category = category,
        )
    }
}
