package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamHybridEnrichmentTest {
    @Test
    fun matchesXtreamCategoriesWithoutHeavyBulkStreamsOnImport() = runBlocking {
        val calls = mutableListOf<String>()
        val loader = RemotePlaylistLoader(client = xtreamClient(calls))

        val result = loader.load(
            playlistId = "playlist-1",
            request = CreatePlaylistSourceRequest(
                type = PlaylistSourceType.M3U_URL,
                name = "Hybrid",
                endpoint = "http://xtream.example/get.php?username=user&password=pass&type=m3u_plus&output=ts",
            ),
        )

        assertTrue(result.xtreamApiSupported)
        assertEquals(2, result.items.size)
        val movie = result.items.first { it.kind == ContentKind.MOVIE }
        assertEquals("http://media.example/movie/user/pass/100.mp4", movie.streamUrl)
        assertEquals(null, movie.xtreamId)

        val episode = result.items.first { it.kind == ContentKind.EPISODE }
        assertEquals("http://media.example/series/user/pass/201.mkv", episode.streamUrl)
        assertEquals(null, episode.xtreamId)
        assertEquals(2, result.xtreamCategoryMappings.size)
        assertTrue(result.xtreamCategoryMappings.any { it.kind == "MOVIE" && it.localName == "Filmler" })
        assertTrue(result.xtreamCategoryMappings.any { it.kind == "SERIES" && it.localName == "Diziler" })
        assertTrue(calls.any { it.contains("action=get_live_categories") })
        assertTrue(calls.any { it.contains("action=get_vod_categories") })
        assertTrue(calls.any { it.contains("action=get_series_categories") })
        assertFalse(calls.any { it.contains("action=get_vod_streams") || it.matches(Regex(""".*[?&]action=get_series(&.*)?$""")) })
        assertFalse(calls.any { it.contains("get_vod_info") || it.contains("get_series_info") })
    }

    @Test
    fun keepsPlainM3uLoadWhenXtreamAuthFails() = runBlocking {
        val loader = RemotePlaylistLoader(client = xtreamClient(mutableListOf(), authenticated = false))

        val result = loader.load(
            playlistId = "playlist-1",
            request = CreatePlaylistSourceRequest(
                type = PlaylistSourceType.M3U_URL,
                name = "Plain",
                endpoint = "http://xtream.example/get.php?username=user&password=bad&type=m3u_plus&output=ts",
            ),
        )

        assertFalse(result.xtreamApiSupported)
        assertEquals(2, result.items.size)
        assertEquals(null, result.items.first { it.kind == ContentKind.MOVIE }.xtreamId)
    }

    private fun xtreamClient(
        calls: MutableList<String>,
        authenticated: Boolean = true,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                calls += request.url.toString()
                val body = when {
                    request.url.encodedPath.endsWith("/get.php") -> m3uBody()
                    request.url.queryParameter("action") == null -> {
                        if (authenticated) """{"user_info":{"auth":1}}""" else """{"user_info":{"auth":0}}"""
                    }
                    request.url.queryParameter("action") == "get_live_categories" -> "[]"
                    request.url.queryParameter("action") == "get_vod_categories" -> vodCategoriesBody()
                    request.url.queryParameter("action") == "get_series_categories" -> seriesCategoriesBody()
                    else -> throw IOException("Unexpected Xtream detail call: ${request.url}")
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    private fun m3uBody(): String = """
        #EXTM3U
        #EXTINF:-1 tvg-name="THE MOVIE" tvg-logo="http://old.example/movie.jpg" group-title="Filmler",THE MOVIE
        http://media.example/movie/user/pass/100.mp4
        #EXTINF:-1 tvg-name="DUNE PROPHECY S1 E1" group-title="Diziler",DUNE PROPHECY S1 E1
        http://media.example/series/user/pass/201.mkv
    """.trimIndent()

    private fun vodCategoriesBody(): String = """
        [
          {"category_id":"10","category_name":"Filmler"}
        ]
    """.trimIndent()

    private fun seriesCategoriesBody(): String = """
        [
          {"category_id":"20","category_name":"Diziler"}
        ]
    """.trimIndent()
}
