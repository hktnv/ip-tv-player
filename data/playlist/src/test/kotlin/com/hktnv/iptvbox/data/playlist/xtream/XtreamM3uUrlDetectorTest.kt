package com.hktnv.iptvbox.data.playlist.xtream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XtreamM3uUrlDetectorTest {
    @Test
    fun extractsCredentialsFromGetPhpM3uUrl() {
        val credentials = XtreamM3uUrlDetector.detect(
            "http://server.example:8080/get.php?username=user%40mail&password=p%40ss&type=m3u_plus&output=ts",
        )

        assertEquals("http://server.example:8080", credentials?.serverUrl)
        assertEquals("user@mail", credentials?.username)
        assertEquals("p@ss", credentials?.password)
    }

    @Test
    fun ignoresNonXtreamUrls() {
        assertNull(XtreamM3uUrlDetector.detect("http://server.example/playlist.m3u"))
    }
}
