package com.hktnv.iptvbox

import java.security.MessageDigest
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.hktnv.iptvbox.update.AppUpdateCheckResult
import com.hktnv.iptvbox.update.AppUpdateRelease
import com.hktnv.iptvbox.update.AppUpdateService

class AppUpdateServiceTest {
    @Test
    fun returnsNoUpdateForCurrentRelease() = runBlocking {
        val service = serviceWithJson(
            """
            {
              "data": {
                "status": "UP_TO_DATE",
                "package_name": "com.hktnv.iptvbox.personal",
                "channel": "stable",
                "current_version_code": 12,
                "published_version_code": 12
              }
            }
            """.trimIndent(),
        )

        val result = service.checkForUpdate("com.hktnv.iptvbox.personal", 12)

        assertTrue(result is AppUpdateCheckResult.NoUpdate)
    }

    @Test
    fun returnsAvailableUpdateFromPublicResponse() = runBlocking {
        val service = serviceWithJson(
            """
            {
              "data": {
                "status": "UPDATE_AVAILABLE",
                "package_name": "com.hktnv.iptvbox.personal",
                "channel": "stable",
                "current_version_code": 12,
                "published_version_code": 13,
                "required": false,
                "release": {
                  "id": "rel_13",
                  "version_code": 13,
                  "version_name": "1.0.13",
                  "release_notes": "Küçük düzeltmeler",
                  "sha256": "abc123",
                  "size_bytes": 9,
                  "download_url": "https://apk.habersoft.com/api/v1/artifacts/rel_13/download"
                }
              }
            }
            """.trimIndent(),
        )

        val result = service.checkForUpdate("com.hktnv.iptvbox.personal", 12)

        assertTrue(result is AppUpdateCheckResult.Available)
        val update = (result as AppUpdateCheckResult.Available).update
        assertEquals(13, update.publishedVersionCode)
        assertEquals("rel_13", update.release.id)
        assertEquals(false, update.required)
    }

    @Test
    fun downloadsApkAndVerifiesSha256() = runBlocking {
        val apkBytes = "fake-apk-body".toByteArray()
        val service = serviceWithBinary(apkBytes)
        val tempDir = Files.createTempDirectory("iptv-update").toFile()
        val progressValues = mutableListOf<Int?>()

        try {
            val file = service.downloadVerifiedApk(
                cacheDir = tempDir,
                release = releaseFor(apkBytes),
                onProgress = { progressValues += it },
            )

            assertTrue(file.isFile)
            assertEquals(apkBytes.toList(), file.readBytes().toList())
            assertTrue(progressValues.contains(100))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun rejectsApkWhenSha256DoesNotMatch() = runBlocking {
        val service = serviceWithBinary("wrong-body".toByteArray())
        val tempDir = Files.createTempDirectory("iptv-update-bad").toFile()

        try {
            val result = runCatching {
                service.downloadVerifiedApk(
                    cacheDir = tempDir,
                    release = releaseFor("expected-body".toByteArray()),
                    onProgress = {},
                )
            }

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("doğrulanamadı"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun serviceWithJson(body: String): AppUpdateService {
        return AppUpdateService(
            client = clientReturning(body.toByteArray(), "application/json"),
            baseUrl = "https://example.test",
            progressDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun serviceWithBinary(body: ByteArray): AppUpdateService {
        return AppUpdateService(
            client = clientReturning(body, "application/vnd.android.package-archive"),
            baseUrl = "https://example.test",
            progressDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun clientReturning(body: ByteArray, contentType: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody(contentType.toMediaType()))
                    .build()
            }
            .build()
    }

    private fun releaseFor(apkBytes: ByteArray): AppUpdateRelease {
        return AppUpdateRelease(
            id = "rel_13",
            versionCode = 13,
            versionName = "1.0.13",
            releaseNotes = "",
            sha256 = sha256(apkBytes),
            sizeBytes = apkBytes.size.toLong(),
            downloadUrl = "https://apk.habersoft.com/api/v1/artifacts/rel_13/download",
        )
    }

    private fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
