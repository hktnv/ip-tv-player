package com.hktnv.iptvbox.model
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import kotlinx.serialization.Serializable

internal enum class AppScreen {
    HOME,
    PLAYLISTS,
    CATALOG,
    SEARCH,
    LATEST,
    FAVORITES,
    RECENT,
    SETTINGS,
    PLAYER,
}

internal enum class CatalogTab(
    val label: String,
    val emptyLabel: String,
    val kinds: Set<ContentKind>,
) {
    LIVE("Canlı TV", "Canlı TV bulunamadı", setOf(ContentKind.LIVE_CHANNEL, ContentKind.RADIO)),
    MOVIES("Filmler", "Film bulunamadı", setOf(ContentKind.MOVIE)),
    SERIES("Diziler", "Dizi bulunamadı", setOf(ContentKind.SERIES, ContentKind.SEASON, ContentKind.EPISODE)),
}

@Serializable
internal data class LoadedPlaylist(
    val id: String,
    val name: String,
    val type: PlaylistSourceType,
    val endpoint: String,
    val headers: Map<String, String>,
    val items: List<CatalogItem>,
    val epgUrls: List<String>,
    val warnings: List<String>,
    val cachedItemCount: Int? = null,
    val cachedLiveCount: Int? = null,
    val cachedMovieCount: Int? = null,
    val cachedSeriesCount: Int? = null,
    val autoUpdateHours: Int = 0,
    val xtreamApiSupported: Boolean = false,
    val updatedAtEpochMillis: Long = 0L,
)

internal data class PlaylistStats(
    val live: Int,
    val movies: Int,
    val series: Int,
) {
    val total: Int get() = live + movies + series

    fun count(tab: CatalogTab): Int = when (tab) {
        CatalogTab.LIVE -> live
        CatalogTab.MOVIES -> movies
        CatalogTab.SERIES -> series
    }
}

internal data class DraftLoadState(
    val loading: Boolean = false,
    val message: String? = null,
    val processedItems: Int = 0,
    val totalItems: Int? = null,
    val error: String? = null,
)

internal data class PlaylistImportProgress(
    val playlistId: String,
    val message: String,
    val processedItems: Int = 0,
    val totalItems: Int? = null,
    val complete: Boolean = false,
    val error: String? = null,
) {
    val active: Boolean get() = !complete && error == null
}

internal val ScreenBottomPadding = 88.dp
internal const val HomePreviewLimit = 8
internal const val SearchResultLimit = 300

internal data class AppPerformanceMode(
    val loadImages: Boolean,
    val homePreviewLimit: Int,
    val searchResultLimit: Int,
) {
    companion object {
        fun from(@Suppress("UNUSED_PARAMETER") context: Context): AppPerformanceMode = AppPerformanceMode(
            loadImages = true,
            homePreviewLimit = HomePreviewLimit,
            searchResultLimit = SearchResultLimit,
        )
    }
}

internal val LocalPerformanceMode = staticCompositionLocalOf {
    AppPerformanceMode(
        loadImages = true,
        homePreviewLimit = HomePreviewLimit,
        searchResultLimit = SearchResultLimit,
    )
}
