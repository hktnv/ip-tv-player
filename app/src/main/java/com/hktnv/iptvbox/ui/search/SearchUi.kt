package com.hktnv.iptvbox.ui.search
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.LocalPerformanceMode
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.model.SearchResultLimit
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.ui.catalog.badgeLabel
import com.hktnv.iptvbox.ui.catalog.tint
import com.hktnv.iptvbox.ui.common.EmptyCatalog
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.common.LoadingPanel
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.media.ContentArtwork
import com.hktnv.iptvbox.ui.media.displayTitle
import com.hktnv.iptvbox.ui.media.label
import com.hktnv.iptvbox.ui.media.metaLine

@Composable
internal fun SearchScreen(
    playlist: LoadedPlaylist?,
    snapshot: CatalogSnapshot?,
    catalogIndexLoading: Boolean,
    catalogRepository: AppCatalogRepository,
    telemetry: AppPerformanceTelemetry,
    query: String,
    submittedQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onAddPlaylist: () -> Unit,
    onRequestSideMenu: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
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
    var queryFocused by remember { mutableStateOf(false) }
    var keyboardRequested by remember { mutableStateOf(false) }

    BackHandler(enabled = television && keyboardRequested) {
        keyboardRequested = false
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
            .onPreviewKeyEvent { event ->
                if (
                    television &&
                    query.isBlank() &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft
                ) {
                    keyboardRequested = false
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    onRequestSideMenu()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = contentPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .then(initialFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                    .onFocusChanged {
                        queryFocused = it.isFocused
                        if (!it.isFocused) keyboardRequested = false
                    }
                    .onPreviewKeyEvent { event ->
                        if (television && queryFocused && event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    keyboardRequested = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    false
                                }
                                Key.DirectionLeft -> {
                                    if (query.isBlank()) {
                                        keyboardRequested = false
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        onRequestSideMenu()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                Key.DirectionCenter,
                                Key.Enter,
                                Key.NumPadEnter -> {
                                    keyboardRequested = true
                                    keyboardController?.show()
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                },
                label = { Text("Kanal, film, dizi veya kategori ara") },
                readOnly = television && !keyboardRequested,
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(showKeyboardOnFocus = !television),
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
                onOpenItem = onOpenItem,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun SearchResultsList(
    items: List<CatalogItem>,
    onOpenItem: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 4.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }, contentType = { "search-${it.kind.name}" }) { item ->
            SearchResultRow(
                item = item,
                onOpen = { onOpenItem(item) },
            )
        }
    }
}

@Composable
internal fun SearchResultRow(
    item: CatalogItem,
    onOpen: () -> Unit,
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
                    .width(if (liveLike) 78.dp else 64.dp)
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
