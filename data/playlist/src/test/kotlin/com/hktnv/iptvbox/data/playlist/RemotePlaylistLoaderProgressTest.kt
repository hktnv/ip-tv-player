package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.model.PlaylistSourceType
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePlaylistLoaderProgressTest {
    @Test
    fun streamsInitialM3uLoadWithoutPreCountingTotal() = runBlocking {
        val progress = mutableListOf<PlaylistLoadProgress>()
        val loader = RemotePlaylistLoader(client = playlistClient(syntheticM3u(itemCount = 125)))

        val result = loader.load(
            playlistId = "initial-load",
            request = CreatePlaylistSourceRequest(
                type = PlaylistSourceType.M3U_URL,
                name = "Test",
                endpoint = "http://playlist.example/test.m3u",
            ),
        ) { progress += it }

        assertEquals(125, result.items.size)
        val readingProgress = progress.filter { it.stage == PlaylistLoadStage.READING }
        assertTrue(readingProgress.any { it.processedItems == 0 && it.totalItems == null })
        assertTrue(readingProgress.any { it.processedItems == 100 && it.totalItems == null })
        assertTrue(progress.any { it.stage == PlaylistLoadStage.COMPLETED && it.totalItems == 125 })
    }

    @Test
    fun keepsExpectedTotalWhenRefreshingKnownM3uLoad() = runBlocking {
        val progress = mutableListOf<PlaylistLoadProgress>()
        val loader = RemotePlaylistLoader(client = playlistClient(syntheticM3u(itemCount = 125)))

        loader.load(
            playlistId = "refresh-load",
            request = CreatePlaylistSourceRequest(
                type = PlaylistSourceType.M3U_URL,
                name = "Test",
                endpoint = "http://playlist.example/test.m3u",
            ),
            expectedItemCount = 125,
        ) { progress += it }

        val readingProgress = progress.filter { it.stage == PlaylistLoadStage.READING }
        assertTrue(readingProgress.any { it.processedItems == 0 && it.totalItems == 125 })
        assertTrue(readingProgress.any { it.processedItems == 100 && it.totalItems == 125 })
        assertTrue(progress.any { it.stage == PlaylistLoadStage.COMPLETED && it.totalItems == 125 })
    }

    @Test
    fun explainsTruncatedUrlInUserLanguage() {
        val message = playlistLoadUserMessage(
            url = "http://playlist.example/get.php?username=test",
            throwable = IOException("Failed to connect"),
        )

        assertEquals("Adres eksik görünüyor. Oynatma listesi URL'sini tam olarak girin.", message)
    }

    @Test
    fun explainsUnauthorizedPlaylistInUserLanguage() {
        val message = playlistLoadUserMessage(
            url = "http://playlist.example/get.php?username=test&password=wrong",
            throwable = IOException("HTTP 401"),
        )

        assertEquals("Sunucu erişimi reddetti. Kullanıcı adı, parola veya liste yetkisini kontrol edin.", message)
    }

    private fun playlistClient(body: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/x-mpegurl".toMediaType()))
                    .build()
            }
            .build()
    }

    private fun syntheticM3u(itemCount: Int): String = buildString {
        appendLine("#EXTM3U")
        repeat(itemCount) { index ->
            val number = index + 1
            appendLine("""#EXTINF:-1 tvg-name="TEST $number" group-title="Test",TEST $number""")
            appendLine("http://media.example/live/user/pass/$number.ts")
        }
    }
}
