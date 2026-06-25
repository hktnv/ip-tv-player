package com.evomrdm.iptvbox

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

internal const val ApkUpdateBaseUrl = "https://apk.habersoft.com"
internal const val ApkUpdateChannel = "stable"

internal sealed interface AppUpdateCheckResult {
    data object NoUpdate : AppUpdateCheckResult
    data class Available(val update: AppUpdateInfo) : AppUpdateCheckResult
    data class Failed(val message: String) : AppUpdateCheckResult
}

internal data class AppUpdateInfo(
    val packageName: String,
    val channel: String,
    val currentVersionCode: Int,
    val publishedVersionCode: Int,
    val required: Boolean,
    val release: AppUpdateRelease,
)

internal data class AppUpdateRelease(
    val id: String,
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val sha256: String,
    val sizeBytes: Long,
    val downloadUrl: String,
)

internal sealed interface AppUpdateUiState {
    data object Hidden : AppUpdateUiState
    data class Available(val update: AppUpdateInfo) : AppUpdateUiState
    data class Downloading(val update: AppUpdateInfo, val progress: Int?) : AppUpdateUiState
    data class PermissionRequired(val update: AppUpdateInfo, val file: File) : AppUpdateUiState
    data class Error(val message: String, val required: Boolean) : AppUpdateUiState
}

@Serializable
internal data class UpdateCheckEnvelope(
    val data: UpdateCheckData? = null,
    val error: UpdateErrorData? = null,
)

@Serializable
internal data class UpdateCheckData(
    val status: String,
    @SerialName("package_name") val packageName: String,
    val channel: String,
    @SerialName("current_version_code") val currentVersionCode: Int,
    @SerialName("published_version_code") val publishedVersionCode: Int? = null,
    val required: Boolean? = null,
    val release: UpdateReleaseData? = null,
)

@Serializable
internal data class UpdateReleaseData(
    val id: String,
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    @SerialName("release_notes") val releaseNotes: String,
    val sha256: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("download_url") val downloadUrl: String,
)

@Serializable
internal data class UpdateErrorData(
    val code: String,
    val message: String,
)
