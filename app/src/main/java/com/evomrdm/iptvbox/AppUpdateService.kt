package com.evomrdm.iptvbox

import com.evomrdm.iptvbox.core.network.HttpClientFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

internal class AppUpdateService(
    private val client: OkHttpClient = HttpClientFactory.create(),
    private val baseUrl: String = ApkUpdateBaseUrl,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val progressDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    suspend fun checkForUpdate(
        packageName: String,
        currentVersionCode: Int,
        channel: String = ApkUpdateChannel,
    ): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl()
            .newBuilder()
            .addPathSegments("api/v1/applications")
            .addPathSegment(packageName)
            .addPathSegment("channels")
            .addPathSegment(channel)
            .addPathSegment("update-check")
            .addQueryParameter("current_version_code", currentVersionCode.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    return@withContext AppUpdateCheckResult.Failed(readErrorMessage(body))
                }
                val envelope = json.decodeFromString<UpdateCheckEnvelope>(body)
                val data = envelope.data ?: return@withContext AppUpdateCheckResult.NoUpdate
                if (data.status != "UPDATE_AVAILABLE") {
                    return@withContext AppUpdateCheckResult.NoUpdate
                }
                val release = data.release ?: return@withContext AppUpdateCheckResult.Failed("Güncelleme bilgisi eksik.")
                AppUpdateCheckResult.Available(
                    AppUpdateInfo(
                        packageName = data.packageName,
                        channel = data.channel,
                        currentVersionCode = data.currentVersionCode,
                        publishedVersionCode = data.publishedVersionCode ?: release.versionCode,
                        required = data.required == true,
                        release = AppUpdateRelease(
                            id = release.id,
                            versionCode = release.versionCode,
                            versionName = release.versionName,
                            releaseNotes = release.releaseNotes,
                            sha256 = release.sha256,
                            sizeBytes = release.sizeBytes,
                            downloadUrl = release.downloadUrl,
                        ),
                    ),
                )
            }
        }.getOrElse {
            AppUpdateCheckResult.Failed("Güncelleme kontrol edilemedi.")
        }
    }

    suspend fun downloadVerifiedApk(
        cacheDir: File,
        release: AppUpdateRelease,
        onProgress: suspend (Int?) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(release.downloadUrl)
            .header("Accept", "application/vnd.android.package-archive")
            .build()

        val updateDir = File(cacheDir, "updates").apply { mkdirs() }
        updateDir.listFiles()?.forEach { it.delete() }
        val tempFile = File(updateDir, "update-${release.versionCode}.apk.part")
        val finalFile = File(updateDir, "update-${release.versionCode}.apk")
        val digest = MessageDigest.getInstance("SHA-256")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("APK indirilemedi.")
            }
            val body = response.body
            val totalBytes = body.contentLength().takeIf { it > 0L }
            withContext(progressDispatcher) { onProgress(if (totalBytes == null) null else 0) }
            body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var written = 0L
                    var lastProgress = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        written += read
                        if (totalBytes != null) {
                            val progress = ((written * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                withContext(progressDispatcher) { onProgress(progress) }
                            }
                        }
                    }
                }
            }
        }

        val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actualSha256.equals(release.sha256, ignoreCase = true)) {
            tempFile.delete()
            throw IOException("Güncelleme dosyası doğrulanamadı.")
        }
        finalFile.delete()
        if (!tempFile.renameTo(finalFile)) {
            throw IOException("Güncelleme dosyası hazırlanamadı.")
        }
        finalFile
    }

    private fun readErrorMessage(body: String): String {
        return runCatching {
            json.decodeFromString<UpdateCheckEnvelope>(body).error?.message
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Güncelleme kontrol edilemedi."
    }
}
