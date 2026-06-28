package com.hktnv.iptvbox.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchNormalizerTest {
    @Test
    fun normalizesTurkishIAndSpacing() {
        assertEquals(
            "istanbul canli haber",
            SearchNormalizer.normalize("  ISTANBUL / Canli   Haber "),
        )
        assertEquals(
            "izmir film",
            SearchNormalizer.normalize("Izmir Film"),
        )
    }
}
