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

    @Test
    fun normalizesTurkishAndAccentedLettersWithoutRegexWhitespacePasses() {
        assertEquals(
            "col gezegeni bolum iki",
            SearchNormalizer.normalize("ÇÖL GEZEGENİ BÖLÜM İKİ"),
        )
        assertEquals(
            "deutsche komodien elite",
            SearchNormalizer.normalize("Deutsche Komödien élite"),
        )
    }

    @Test
    fun normalizesMultipleCatalogPartsWithoutJoiningFirst() {
        assertEquals(
            "trt 1 hd turk ulusal haber",
            SearchNormalizer.normalizeParts("TRT 1 HD", "Türk Ulusal", null, "Haber"),
        )
    }
}
