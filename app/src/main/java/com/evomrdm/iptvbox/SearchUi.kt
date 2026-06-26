package com.evomrdm.iptvbox

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.evomrdm.iptvbox.core.common.SearchNormalizer
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.core.model.ContentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SearchScreen(
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    catalogRepository: AppCatalogRepository,
    telemetry: AppPerformanceTelemetry,
    query: String,
    submittedQuery: String,
    favoriteIds: List<String>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    contentPadding: Dp,
) {
    if (playlist == null) {
        EmptyCatalog(onAddPlaylist, contentPadding)
        return
    }
    val performanceMode = LocalPerformanceMode.current
    val configuration = LocalConfiguration.current
    val television = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var results by remember(playlist.id) { mutableStateOf<List<CatalogItem>>(emptyList()) }
    var searchLoading by remember(playlist.id) { mutableStateOf(false) }
    var queryEditing by remember { mutableStateOf(false) }

    BackHandler(enabled = television && queryEditing) {
        queryEditing = false
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    LaunchedEffect(snapshot, submittedQuery, performanceMode.searchResultLimit) {
        val normalizedQuery = SearchNormalizer.normalize(submittedQuery)
        if (normalizedQuery.isBlank() || snapshot == null) {
            results = emptyList()
            searchLoading = false
        } else {
            searchLoading = true
            val searchStartedAt = android.os.SystemClock.elapsedRealtime()
            results = withContext(Dispatchers.IO) {
                catalogRepository.search(snapshot, normalizedQuery, performanceMode.searchResultLimit)
            }
            telemetry.recordDuration("search_first_result_ms", searchStartedAt)
            searchLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding),
    ) {
        ScreenHeader(
            title = "Arama",
            subtitle = playlist.name,
            actionLabel = null,
            onAction = null,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { queryEditing = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (
                            television &&
                            queryEditing &&
                            event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionDown
                        ) {
                            queryEditing = false
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            false
                        } else {
                            false
                        }
                    },
                label = { Text("Kanal, film, dizi veya kategori ara") },
                singleLine = true,
            )
            Button(
                onClick = onSearch,
                enabled = query.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp),
            ) {
                Text("Ara")
            }
        }
        Text(
            text = when {
                catalogIndexLoading || snapshot == null -> "Katalog hazırlanıyor"
                submittedQuery.isBlank() -> "Aramak istediğin metni yazıp Ara'ya bas."
                searchLoading -> "Aranıyor"
                results.isEmpty() -> "Sonuç bulunamadı"
                else -> "${results.size} sonuç"
            },
            color = IptvColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (catalogIndexLoading || snapshot == null) {
            LoadingPanel("Katalog hazırlanıyor", Modifier.padding(top = 12.dp))
        } else if (submittedQuery.isNotBlank() && !searchLoading && results.isEmpty()) {
            EmptyState(
                title = "Sonuç bulunamadı",
                body = "Farklı bir kanal, film, dizi veya kategori adıyla tekrar ara.",
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            SearchResultsList(
                items = results,
                favoriteIds = favoriteIds,
                onOpenItem = onOpenItem,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun SearchResultsList(
    items: List<CatalogItem>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteKey = favoriteIds.joinToString("|")
    val favoriteSet = remember(favoriteKey) { favoriteIds.toSet() }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }, contentType = { "search-${it.kind.name}" }) { item ->
            SearchResultRow(
                item = item,
                favorite = item.id in favoriteSet,
                onOpen = { onOpenItem(item) },
                onToggleFavorite = { onToggleFavorite(item) },
            )
        }
    }
}

@Composable
internal fun SearchResultRow(
    item: CatalogItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val liveLike = item.kind in CatalogTab.LIVE.kinds
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.015f, liftPx = -3f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onOpen),
        color = if (focused) TvFocusPanel else IptvColors.Panel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContentArtwork(
                title = item.displayTitle(),
                kind = item.kind,
                logoUrl = item.logoUrl,
                showBadge = false,
                modifier = Modifier
                    .fillMaxWidth(if (liveLike) 0.28f else 0.18f)
                    .height(if (liveLike) 58.dp else 86.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = item.displayTitle(),
                    color = IptvColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchKindPill(item.kind)
                    Text(
                        text = item.metaLine(),
                        color = IptvColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onToggleFavorite) {
                Text(if (favorite) "Çıkar" else "Favori", fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun SearchKindPill(kind: ContentKind) {
    Surface(
        color = kind.tint().copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, kind.tint().copy(alpha = 0.32f)),
    ) {
        Text(
            text = kind.badgeLabel(),
            color = IptvColors.TextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}
