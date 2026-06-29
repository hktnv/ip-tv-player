package com.hktnv.iptvbox.ui.search
import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.common.SearchNormalizer
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
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val compactSearchControls = !television && configuration.screenWidthDp < 600
    var results by remember(playlist.id) { mutableStateOf<List<CatalogItem>>(emptyList()) }
    var searchLoading by remember(playlist.id) { mutableStateOf(false) }
    var queryFocused by remember { mutableStateOf(false) }
    var keyboardRequested by remember { mutableStateOf(false) }
    var keyboardActivationReady by remember { mutableStateOf(false) }
    var keyboardWasVisible by remember { mutableStateOf(false) }
    var pendingKeyboardCloseAfterClear by remember { mutableStateOf(false) }
    val inputFocusRequester = initialFocusRequester ?: remember { FocusRequester() }
    val searchButtonFocusRequester = remember { FocusRequester() }
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

    LaunchedEffect(imeVisible, queryFocused, keyboardRequested, query) {
        if (
            television &&
            queryFocused &&
            keyboardRequested &&
            keyboardWasVisible &&
            !imeVisible &&
            query.isNotEmpty()
        ) {
            onQueryChange("")
            pendingKeyboardCloseAfterClear = true
            withFrameNanos { }
            keyboardController?.show()
        }
        keyboardWasVisible = imeVisible
    }

    fun handleSearchKeyboardBack(): Boolean {
        if (query.isNotEmpty()) {
            onQueryChange("")
            keyboardRequested = true
            pendingKeyboardCloseAfterClear = true
            return true
        }
        if (keyboardRequested || pendingKeyboardCloseAfterClear) {
            keyboardRequested = false
            pendingKeyboardCloseAfterClear = false
            keyboardController?.hide()
            runCatching { inputFocusRequester.requestFocus() }
            return true
        }
        return false
    }

    BackHandler(
        enabled = television &&
            queryFocused &&
            (keyboardRequested || pendingKeyboardCloseAfterClear || query.isNotEmpty()),
    ) {
        if (!handleSearchKeyboardBack()) {
            keyboardRequested = false
            pendingKeyboardCloseAfterClear = false
        }
    }
    SearchKeyboardBackGuard(
        enabled = television && queryFocused && keyboardRequested && query.isNotEmpty(),
        onBack = { handleSearchKeyboardBack() },
    )

    LaunchedEffect(queryFocused) {
        if (!queryFocused) {
            pendingKeyboardCloseAfterClear = false
        }
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
        SearchControls(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            compact = compactSearchControls,
            television = television,
            queryFocused = queryFocused,
            keyboardRequested = keyboardRequested,
            keyboardActivationReady = keyboardActivationReady,
            hasResults = results.isNotEmpty(),
            inputFocusRequester = inputFocusRequester,
            searchButtonFocusRequester = searchButtonFocusRequester,
            firstResultFocusRequester = firstResultFocusRequester,
            focusManager = focusManager,
            keyboardController = keyboardController,
            onQueryFocusChanged = {
                queryFocused = it
                if (!it) keyboardRequested = false
            },
            onKeyboardRequestedChange = { keyboardRequested = it },
            onKeyboardBackPressed = { handleSearchKeyboardBack() },
            onRequestSideMenu = onRequestSideMenu,
        )
        Text(
            text = when {
                catalogIndexLoading || snapshot == null -> "Katalog hazırlanıyor"
                submittedQuery.isBlank() -> "Aramak istediğin metni yazıp Ara'ya bas."
                searchLoading -> "Aranıyor"
                results.isEmpty() -> "Sonuç bulunamadı"
                else -> "${results.size} sonuç"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
