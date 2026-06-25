package com.evomrdm.iptvbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.evomrdm.iptvbox.core.model.CatalogItem

@Composable
internal fun SavedItemsScreen(
    title: String,
    emptyText: String,
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    itemIds: List<String>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    contentPadding: Dp,
) {
    if (playlist == null) {
        EmptyCatalog(onAddPlaylist, contentPadding)
        return
    }
    val idSignature = itemIds.joinToString("|")
    val items = remember(snapshot, idSignature) { snapshot?.itemsByIds(itemIds).orEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        ScreenHeader(
            title = title,
            subtitle = playlist.name,
            actionLabel = null,
            onAction = null,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (snapshot == null || catalogIndexLoading) {
            LoadingPanel("Katalog hazırlanıyor", Modifier.padding(top = 18.dp))
        } else if (items.isEmpty()) {
            EmptyState(
                title = emptyText,
                body = "Katalogdaki içerikleri seçtikçe bu ekran otomatik dolar.",
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 18.dp),
            )
        } else {
            ContentGrid(
                items = items,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun SettingsScreen(
    playlist: LoadedPlaylist?,
    diagnostics: PerformanceDiagnostics,
    onReload: () -> Unit,
    onAddPlaylist: () -> Unit,
    contentPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
        contentPadding = PaddingValues(top = 16.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = "Ayarlar",
                subtitle = "Kişisel sürüm",
                actionLabel = null,
                onAction = null,
            )
        }
        item {
            InfoPanel(
                title = "Başlangıç",
                body = "Son oynatma listesi, favoriler ve son izlenenler bu cihazda hatırlanır.",
            )
        }
        item {
            InfoPanel(
                title = "Gizlilik",
                body = "Oynatma listesi bilgileri yalnızca bu cihazda saklanır.",
            )
        }
        item {
            InfoPanel(
                title = "Performans / Tanılama",
                body = diagnosticsText(diagnostics, playlist),
            )
        }
        item {
            if (playlist == null) {
                EmptyState(
                    title = "Oynatma listesi yok",
                    body = "Liste eklediğinizde yenileme ve katalog bilgileri burada görünür.",
                    actionLabel = "Liste Ekle",
                    onAction = onAddPlaylist,
                )
            } else {
                InfoPanel(
                    title = playlist.name,
                    body = playlist.catalogSummary(),
                    actionLabel = "Yenile",
                    onAction = onReload,
                )
            }
        }
    }
}

private fun diagnosticsText(
    diagnostics: PerformanceDiagnostics,
    playlist: LoadedPlaylist?,
): String {
    val stats = playlist?.stats()
    val itemCount = playlist?.cachedItemCount ?: playlist?.items?.size
    val playlistText = if (playlist == null || itemCount == null) {
        "Liste yok"
    } else {
        "$itemCount içerik (${stats?.live ?: 0} canlı, ${stats?.movies ?: 0} film, ${stats?.series ?: 0} dizi)"
    }

    return buildString {
        appendLine("Uygulama açılış süresi: ${diagnostics.appOpenMs()}")
        appendLine("Oynatma listesi ekleme süresi: ${diagnostics.ms("playlist_import_total_ms")}")
        appendLine("Katalog hazır olma süresi: ${diagnostics.ms("catalog_screen_ready_ms")}")
        appendLine("Arama ilk sonuç süresi: ${diagnostics.ms("search_first_result_ms")}")
        appendLine("Yaklaşık RAM kullanımı: ${diagnostics.mb("ram_mb")}")
        appendLine("Son hata / çökme bilgisi: ${diagnostics.lastError ?: "Son hata yok"}")
        append("Yüklü oynatma listesi içerik sayısı: $playlistText")
    }
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
        "${menuLabel(key)} ${value.toLongOrNull()?.let { "$it ms" } ?: value}"
    }
}

private fun menuLabel(key: String): String {
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
