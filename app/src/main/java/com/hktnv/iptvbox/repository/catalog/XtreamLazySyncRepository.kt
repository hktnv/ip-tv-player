package com.hktnv.iptvbox.repository.catalog

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.ContentMetadata
import com.hktnv.iptvbox.core.network.HttpClientFactory
import com.hktnv.iptvbox.data.catalog.CategoryKindMovie
import com.hktnv.iptvbox.data.catalog.CategoryKindSeries
import com.hktnv.iptvbox.data.catalog.CatalogRemoteEntry
import com.hktnv.iptvbox.data.catalog.CatalogStore
import com.hktnv.iptvbox.data.catalog.QueuedXtreamCategory
import com.hktnv.iptvbox.data.catalog.SeriesEpisodeRemoteDetails
import com.hktnv.iptvbox.data.catalog.loadMetadata
import com.hktnv.iptvbox.data.catalog.queuedXtreamCategories
import com.hktnv.iptvbox.data.catalog.saveMetadata
import com.hktnv.iptvbox.data.catalog.updateItemsFromXtreamCategory
import com.hktnv.iptvbox.data.catalog.updateSeriesEpisodeDetails
import com.hktnv.iptvbox.data.playlist.xtream.XtreamApiClient
import com.hktnv.iptvbox.data.playlist.xtream.XtreamBulkEntry
import com.hktnv.iptvbox.data.playlist.xtream.XtreamM3uUrlDetector
import com.hktnv.iptvbox.data.playlist.xtream.XtreamSeriesEpisodeEntry
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class XtreamLazySyncRepository(
    private val catalogStore: CatalogStore,
    private val client: XtreamApiClient = XtreamApiClient(HttpClientFactory.create()),
) {
    suspend fun syncQueuedCategories(
        playlist: LoadedPlaylist,
        delayBetweenRequestsMs: Long = 1_000L,
    ): Int {
        val credentials = XtreamM3uUrlDetector.detect(playlist.endpoint) ?: return 0
        val categories = withContext(Dispatchers.IO) {
            catalogStore.queuedXtreamCategories(playlist.id)
        }
        var updated = 0
        categories.forEachIndexed { index, category ->
            val tab = category.catalogTab() ?: return@forEachIndexed
            val remote = withContext(Dispatchers.IO) {
                runCatching {
                    when (category.kind) {
                        CategoryKindMovie -> client.fetchVodStreams(credentials, category.xtreamCategoryId)
                        CategoryKindSeries -> client.fetchSeries(credentials, category.xtreamCategoryId)
                        else -> emptyList()
                    }
                }.getOrDefault(emptyList())
            }
            updated += withContext(Dispatchers.IO) {
                catalogStore.updateItemsFromXtreamCategory(
                    playlistId = playlist.id,
                    tab = tab,
                    category = category.name,
                    entries = remote.map { it.toCatalogRemoteEntry() },
                )
            }
            if (index < categories.lastIndex) delay(delayBetweenRequestsMs)
        }
        return updated
    }

    suspend fun syncSeries(
        playlist: LoadedPlaylist,
        seriesTitle: String?,
        representativeItem: CatalogItem?,
    ): Boolean = withContext(Dispatchers.IO) {
        val seriesId = representativeItem?.xtreamId ?: return@withContext false
        if (seriesTitle.isNullOrBlank()) return@withContext false
        val credentials = XtreamM3uUrlDetector.detect(playlist.endpoint) ?: return@withContext false
        val payload = runCatching {
            client.fetchSeriesInfo(credentials, seriesId)
        }.getOrNull() ?: return@withContext false
        catalogStore.saveMetadata(
            representativeItem,
            ContentMetadata(
                cacheKey = "",
                tmdbId = representativeItem.tmdbId,
                normalizedTitle = "",
                plot = payload.metadata.plot,
                cast = payload.metadata.cast,
                director = payload.metadata.director,
                youtubeTrailer = payload.metadata.youtubeTrailer,
                duration = payload.metadata.duration,
                backdropUrl = payload.metadata.backdropUrl,
            ),
        )
        catalogStore.updateSeriesEpisodeDetails(
            playlistId = playlist.id,
            seriesTitle = seriesTitle,
            episodes = payload.episodes.map { it.toRemoteDetails() },
        ) > 0
    }

    suspend fun syncVodMetadata(
        playlist: LoadedPlaylist?,
        item: CatalogItem,
    ): ContentMetadata? = withContext(Dispatchers.IO) {
        val cached = catalogStore.loadMetadata(item)
        if (cached?.hasUsefulDetails() == true) return@withContext cached
        if (item.kind != ContentKind.MOVIE) return@withContext cached
        val streamId = item.xtreamId ?: return@withContext cached
        val credentials = playlist?.endpoint?.let(XtreamM3uUrlDetector::detect) ?: return@withContext cached
        val payload = runCatching {
            client.fetchVodInfo(credentials, streamId)
        }.getOrNull() ?: return@withContext cached
        catalogStore.saveMetadata(
            item,
            ContentMetadata(
                cacheKey = "",
                tmdbId = item.tmdbId,
                normalizedTitle = "",
                plot = payload.plot,
                cast = payload.cast,
                director = payload.director,
                youtubeTrailer = payload.youtubeTrailer,
                duration = payload.duration,
                backdropUrl = payload.backdropUrl,
            ),
        )
        catalogStore.loadMetadata(item)
    }
}

private fun ContentMetadata.hasUsefulDetails(): Boolean {
    return !plot.isNullOrBlank() ||
        !cast.isNullOrBlank() ||
        !director.isNullOrBlank() ||
        !youtubeTrailer.isNullOrBlank() ||
        !duration.isNullOrBlank() ||
        !backdropUrl.isNullOrBlank()
}

private fun XtreamBulkEntry.toCatalogRemoteEntry(): CatalogRemoteEntry {
    return CatalogRemoteEntry(
        xtreamId = xtreamId,
        title = title,
        posterUrl = posterUrl,
        rating = rating,
        tmdbId = tmdbId,
        addedAtEpochSeconds = addedAtEpochSeconds,
    )
}

private fun XtreamSeriesEpisodeEntry.toRemoteDetails(): SeriesEpisodeRemoteDetails {
    return SeriesEpisodeRemoteDetails(
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title,
        plot = plot,
        imageUrl = imageUrl,
    )
}

private fun QueuedXtreamCategory.catalogTab(): CatalogTab? {
    return when (kind) {
        CategoryKindMovie -> CatalogTab.MOVIES
        CategoryKindSeries -> CatalogTab.SERIES
        else -> null
    }
}
