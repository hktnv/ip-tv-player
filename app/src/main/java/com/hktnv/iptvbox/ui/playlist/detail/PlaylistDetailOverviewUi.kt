package com.hktnv.iptvbox.ui.playlist.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.model.CatalogSyncStatus
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.stats

@Composable
internal fun PlaylistOverviewPanel(
    playlist: LoadedPlaylist,
    syncStatus: CatalogSyncStatus?,
) {
    val stats = remember(playlist.id, playlist.items) { playlist.stats() }
    DetailPanel {
        DetailSectionTitle(stringResource(R.string.playlist_detail_info_section))
        OverviewLine(
            label = stringResource(R.string.playlist_source_type),
            value = playlist.type.label(),
            meta = if (playlist.xtreamApiSupported) {
                stringResource(R.string.playlist_xtream_supported)
            } else {
                null
            },
        )
        OverviewLine(
            label = stringResource(R.string.playlist_content_summary),
            value = stringResource(R.string.playlist_content_count, stats.total),
            meta = stringResource(R.string.playlist_content_breakdown, stats.live, stats.movies, stats.series),
            primary = true,
        )
        CatalogStatusLine(status = syncStatus)
    }
}

@Composable
private fun OverviewLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    primary: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.widthIn(min = 86.dp, max = 132.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (primary) 18.sp else 14.sp,
                fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            meta?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CatalogStatusLine(status: CatalogSyncStatus?) {
    val model = status.toPlaylistSyncStatusUiModel()
    val body = model.bodyArg?.let { stringResource(model.bodyRes, it) } ?: stringResource(model.bodyRes)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (model.active) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = if (model.error) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (model.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(model.titleRes),
                color = if (model.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                color = if (model.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}
