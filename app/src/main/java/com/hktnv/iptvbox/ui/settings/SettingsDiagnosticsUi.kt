package com.hktnv.iptvbox.ui.settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.data.catalog.column
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.telemetry.PerformanceDiagnostics
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.stats

@Composable
internal fun DiagnosticsPanelContent(
    diagnostics: PerformanceDiagnostics,
    playlist: LoadedPlaylist?,
) {
    val rows = diagnosticsRows(diagnostics, playlist)
    BoxWithConstraints {
        val twoColumns = maxWidth >= 420.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Performans / Tanılama",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            if (twoColumns) {
                rows.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pair.forEach { row ->
                            DiagnosticTile(
                                label = row.first,
                                value = row.second,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else {
                rows.forEach { row -> DiagnosticTile(label = row.first, value = row.second) }
            }
        }
    }
}

@Composable
private fun DiagnosticTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, TvRestingBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun diagnosticsRows(
    diagnostics: PerformanceDiagnostics,
    playlist: LoadedPlaylist?,
): List<Pair<String, String>> {
    val stats = playlist?.stats()
    val itemCount = playlist?.cachedItemCount ?: playlist?.items?.size
    val playlistText = if (playlist == null || itemCount == null) {
        "Liste yok"
    } else {
        "$itemCount içerik (${stats?.live ?: 0} canlı, ${stats?.movies ?: 0} film, ${stats?.series ?: 0} dizi)"
    }

    return listOf(
        "Uygulama açılışı" to diagnostics.appOpenMs(),
        "Ana ekran" to diagnostics.ms("home_first_draw_ms"),
        "Liste ekleme" to diagnostics.ms("playlist_import_total_ms"),
        "Katalog hazır" to diagnostics.ms("catalog_screen_ready_ms"),
        "Menü geçişleri" to diagnostics.menuTransitionsText(),
        "Arama sonucu" to diagnostics.ms("search_first_result_ms"),
        "Yaklaşık RAM" to diagnostics.mb("ram_mb"),
        "Son hata" to (diagnostics.lastError ?: "Son hata yok"),
        "Yüklü içerik" to playlistText,
    )
}

private fun PerformanceDiagnostics.appOpenMs(): String {
    return msValue("home_first_draw_ms")
        ?: msValue("cold_start_restore_state_ms")
        ?: msValue("cold_start_on_create_ms")
        ?: "Henüz ölçülmedi"
}

private fun PerformanceDiagnostics.ms(key: String): String = msValue(key) ?: "Henüz ölçülmedi"

private fun PerformanceDiagnostics.mb(key: String): String {
    return values[key]?.toLongOrNull()?.let { "$it MB" } ?: "Henüz ölçülmedi"
}

private fun PerformanceDiagnostics.msValue(key: String): String? {
    return values[key]?.toLongOrNull()?.let { "$it ms" }
}

private fun PerformanceDiagnostics.menuTransitionsText(): String {
    val transitions = values
        .filterKeys { it.startsWith("menu_transition_") && it.endsWith("_ms") }
        .toSortedMap()
    if (transitions.isEmpty()) return "Henüz ölçülmedi"
    return transitions.entries.joinToString(" · ") { (key, value) ->
        "${diagnosticMenuLabel(key)} ${value.toLongOrNull()?.let { "$it ms" } ?: value}"
    }
}

private fun diagnosticMenuLabel(key: String): String {
    return when (key.removePrefix("menu_transition_").removeSuffix("_ms")) {
        "home" -> "Ana"
        "playlists" -> "Listeler"
        "catalog" -> "Katalog"
        "search" -> "Ara"
        "favorites" -> "Favoriler"
        "recent" -> "Son izlenenler"
        "settings" -> "Ayarlar"
        else -> "Menü"
    }
}
