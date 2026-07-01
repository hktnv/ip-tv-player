package com.hktnv.iptvbox.ui.playlist.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.model.CatalogSyncStatus

@Composable
internal fun PlaylistSyncStatusPanel(
    status: CatalogSyncStatus?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val model = status.toPlaylistSyncStatusUiModel()
    val horizontalPadding = if (compact) 12.dp else 16.dp
    val verticalPadding = if (compact) 12.dp else 16.dp
    val indicatorSize = if (compact) 18.dp else 22.dp
    val titleSize = if (compact) 13.sp else 15.sp
    val bodySize = if (compact) 12.sp else 13.sp
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (model.active) {
                CircularProgressIndicator(
                    modifier = Modifier.size(indicatorSize),
                    strokeWidth = 2.dp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(model.titleRes),
                    color = if (model.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = model.bodyText(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = bodySize,
                    lineHeight = if (compact) 16.sp else 17.sp,
                )
            }
        }
    }
}

@Composable
private fun PlaylistSyncStatusUiModel.bodyText(): String {
    val arg = bodyArg
    return if (arg == null) stringResource(bodyRes) else stringResource(bodyRes, arg)
}
