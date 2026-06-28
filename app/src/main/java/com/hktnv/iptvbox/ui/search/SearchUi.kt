package com.hktnv.iptvbox.ui.search
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.core.model.CatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.LocalPerformanceMode
import com.hktnv.iptvbox.model.SearchResultLimit
import com.hktnv.iptvbox.repository.catalog.AppCatalogRepository
import com.hktnv.iptvbox.repository.catalog.CatalogSnapshot
import com.hktnv.iptvbox.telemetry.AppPerformanceTelemetry
import com.hktnv.iptvbox.ui.common.EmptyCatalog
import com.hktnv.iptvbox.ui.common.EmptyState
import com.hktnv.iptvbox.ui.common.LoadingPanel
import kotlinx.coroutines.delay

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
    onOpenSeries: (String) -> Unit,
    favoriteIds: List<String>,
    onShowItemOptions: (CatalogItem) -> Unit,
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
    var keyboardActivationReady by remember { mutableStateOf(false) }
    val inputFocusRequester = initialFocusRequester ?: remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        keyboardActivationReady = false
        var focused = false
        repeat(3) { attempt ->
            withFrameNanos { }
            if (attempt > 0) delay(100L)
            focused = runCatching { inputFocusRequester.requestFocus() }.getOrDefault(false)
            if (focused) return@repeat
        }
        delay(250L)
        keyboardActivationReady = true
    }

    LaunchedEffect(keyboardRequested) {
        if (keyboardRequested) {
            withFrameNanos { }
            runCatching { inputFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

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
            val rawResults = withContext(Dispatchers.IO) {
                catalogRepository.search(snapshot, normalizedQuery, performanceMode.searchResultLimit)
            }
            results = rawResults.collapseSeriesSearchResults()
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
                    .focusRequester(inputFocusRequester)
                    .onFocusChanged {
                        queryFocused = it.hasFocus
                        if (!it.hasFocus) keyboardRequested = false
                    }
                    .onPreviewKeyEvent { event ->
                        if (television && queryFocused && event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    keyboardRequested = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    if (results.isNotEmpty()) {
                                        runCatching { firstResultFocusRequester.requestFocus() }.getOrDefault(false)
                                    } else {
                                        false
                                    }
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
                                    if (keyboardActivationReady) {
                                        keyboardRequested = true
                                    }
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
                keyboardOptions = KeyboardOptions.Default.copy(showKeyboardOnFocus = !television || keyboardRequested),
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
                onOpenSeries = onOpenSeries,
                onShowItemOptions = onShowItemOptions,
                onRequestSideMenu = onRequestSideMenu,
                initialFocusRequester = firstResultFocusRequester,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
