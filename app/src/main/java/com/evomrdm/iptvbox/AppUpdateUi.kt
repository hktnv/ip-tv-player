package com.evomrdm.iptvbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.evomrdm.iptvbox.core.designsystem.IptvColors

@Composable
internal fun AppUpdateDialog(
    state: AppUpdateUiState,
    onDownload: () -> Unit,
    onOpenPermission: () -> Unit,
    onOpenInstaller: () -> Unit,
    onDismiss: () -> Unit,
) {
    val required = when (state) {
        is AppUpdateUiState.Available -> state.update.required
        is AppUpdateUiState.Downloading -> state.update.required
        is AppUpdateUiState.PermissionRequired -> state.update.required
        is AppUpdateUiState.Error -> state.required
        AppUpdateUiState.Hidden -> false
    }
    Dialog(onDismissRequest = { if (!required) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            color = Color(0xFF101821),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF2F4153)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (state) {
                    is AppUpdateUiState.Available -> UpdateAvailableContent(state.update, onDownload, onDismiss)
                    is AppUpdateUiState.Downloading -> UpdateDownloadingContent(state)
                    is AppUpdateUiState.PermissionRequired -> UpdatePermissionContent(state, onOpenPermission, onOpenInstaller, onDismiss)
                    is AppUpdateUiState.Error -> UpdateErrorContent(state, onDismiss)
                    AppUpdateUiState.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
private fun UpdateAvailableContent(
    update: AppUpdateInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    Text(
        text = if (update.required) "Güncelleme gerekli" else "Yeni sürüm hazır",
        color = IptvColors.TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Sürüm ${update.release.versionName} kullanılabilir.",
        color = IptvColors.TextSecondary,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    )
    if (update.release.releaseNotes.isNotBlank()) {
        Text(
            text = update.release.releaseNotes,
            color = IptvColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
    UpdateActions(
        primaryText = "Güncelle",
        onPrimary = onDownload,
        secondaryText = if (update.required) null else "Daha sonra",
        onSecondary = onDismiss,
    )
}

@Composable
private fun UpdateDownloadingContent(state: AppUpdateUiState.Downloading) {
    Text(
        text = "Güncelleme indiriliyor",
        color = IptvColors.TextPrimary,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "APK indiriliyor ve doğrulanacak. Uygulama arka planda yanıt vermeye devam eder.",
        color = IptvColors.TextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    if (state.progress == null) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
        Text(
            text = "%${state.progress}",
            color = IptvColors.TextSecondary,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun UpdatePermissionContent(
    state: AppUpdateUiState.PermissionRequired,
    onOpenPermission: () -> Unit,
    onOpenInstaller: () -> Unit,
    onDismiss: () -> Unit,
) {
    Text(
        text = "Kurulum izni gerekli",
        color = IptvColors.TextPrimary,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Android, APK kurmak için bu kaynağa izin vermenizi isteyebilir. İzni verdikten sonra kurulumu tekrar açın.",
        color = IptvColors.TextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    UpdateActions(
        primaryText = "İzin ver",
        onPrimary = onOpenPermission,
        secondaryText = "Kurulumu aç",
        onSecondary = onOpenInstaller,
        tertiaryText = if (state.update.required) null else "Daha sonra",
        onTertiary = onDismiss,
    )
}

@Composable
private fun UpdateErrorContent(
    state: AppUpdateUiState.Error,
    onDismiss: () -> Unit,
) {
    Text(
        text = "Güncelleme tamamlanamadı",
        color = IptvColors.TextPrimary,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = state.message,
        color = IptvColors.TextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    if (!state.required) {
        UpdateActions(primaryText = "Kapat", onPrimary = onDismiss)
    }
}

@Composable
private fun UpdateActions(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryText: String? = null,
    onTertiary: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPrimary,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(primaryText, maxLines = 1)
            }
            if (secondaryText != null && onSecondary != null) {
                OutlinedButton(
                    onClick = onSecondary,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IptvColors.TextPrimary),
                ) {
                    Text(secondaryText, maxLines = 1)
                }
            }
        }
        if (tertiaryText != null && onTertiary != null) {
            Spacer(Modifier.height(2.dp))
            OutlinedButton(
                onClick = onTertiary,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IptvColors.TextPrimary),
                modifier = Modifier.width(160.dp),
            ) {
                Text(tertiaryText, maxLines = 1)
            }
        }
    }
}
