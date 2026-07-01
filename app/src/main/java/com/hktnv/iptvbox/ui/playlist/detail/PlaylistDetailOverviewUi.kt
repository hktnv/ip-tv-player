package com.hktnv.iptvbox.ui.playlist.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
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
        BoxWithConstraints {
            val rows = if (maxWidth < 520.dp) {
                listOf(
                    listOf(playlist.sourceSummaryCard()),
                    listOf(playlist.contentSummaryCard(stats.total, stats.live, stats.movies, stats.series)),
                )
            } else {
                listOf(
                    listOf(
                        playlist.sourceSummaryCard(),
                        playlist.contentSummaryCard(stats.total, stats.live, stats.movies, stats.series),
                    ),
                )
            }
            SummaryInfoRows(rows)
        }
        if (playlist.xtreamApiSupported || syncStatus != null) {
            PlaylistSyncStatusPanel(
                status = syncStatus,
                modifier = Modifier.fillMaxWidth(),
                compact = true,
            )
        }
    }
}

@Composable
private fun LoadedPlaylist.sourceSummaryCard(): SummaryInfoCard {
    return SummaryInfoCard(
        label = stringResource(R.string.playlist_source_type),
        value = type.label(),
        detail = if (xtreamApiSupported) stringResource(R.string.playlist_xtream_supported) else null,
    )
}

@Composable
private fun LoadedPlaylist.contentSummaryCard(
    total: Int,
    live: Int,
    movies: Int,
    series: Int,
): SummaryInfoCard {
    return SummaryInfoCard(
        label = stringResource(R.string.playlist_content_summary),
        value = stringResource(R.string.playlist_content_count, total),
        detail = stringResource(R.string.playlist_content_breakdown, live, movies, series),
    )
}

@Composable
private fun SummaryInfoRows(rows: List<List<SummaryInfoCard>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { card ->
                    SummaryInfoCardView(card, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryInfoCardView(
    card: SummaryInfoCard,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 74.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = card.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            card.detail?.let {
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

private data class SummaryInfoCard(
    val label: String,
    val value: String,
    val detail: String? = null,
)
