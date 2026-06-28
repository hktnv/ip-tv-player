package com.hktnv.iptvbox.state
import android.content.Context
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.hktnv.iptvbox.data.catalog.CatalogStore
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.ui.media.catalogSignature
import com.hktnv.iptvbox.ui.media.stats

@Serializable
internal data class PersistedAppState(
    val playlists: List<LoadedPlaylist> = emptyList(),
    val selectedPlaylistId: String? = null,
    val selectedScreen: String? = null,
    val selectedTab: String? = null,
    val selectedCategory: String? = null,
    val selectedSeriesTitle: String? = null,
    val selectedSeasonNumber: Int? = null,
    val searchDraft: String = "",
    val submittedSearch: String = "",
    val favoriteIds: List<String> = emptyList(),
    val recentIds: List<String> = emptyList(),
)

@Serializable
private data class PersistedSessionState(
    val selectedPlaylistId: String? = null,
    val selectedScreen: String? = null,
    val selectedTab: String? = null,
    val selectedCategory: String? = null,
    val selectedSeriesTitle: String? = null,
    val selectedSeasonNumber: Int? = null,
    val searchDraft: String = "",
    val submittedSearch: String = "",
    val favoriteIds: List<String> = emptyList(),
    val recentIds: List<String> = emptyList(),
)

@Serializable
private data class PersistedPlaylistMetadata(
    val id: String,
    val name: String,
    val type: PlaylistSourceType,
    val endpoint: String,
    val headers: Map<String, String>,
    val epgUrls: List<String>,
    val warnings: List<String>,
    val itemCount: Int,
    val liveCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
)

@Serializable
private data class PersistedMetadataState(
    val playlists: List<PersistedPlaylistMetadata> = emptyList(),
)

internal class AppStateStore(
    context: Context,
    private val catalogStore: CatalogStore,
) {
    private val legacyStateFile = File(context.applicationContext.filesDir, "iptvbox_state.json")
    private val sessionFile = File(context.applicationContext.filesDir, "iptvbox_session.json")
    private val sessionTempFile = File(context.applicationContext.filesDir, "iptvbox_session.json.tmp")
    private val catalogFile = File(context.applicationContext.filesDir, "iptvbox_catalog.json")
    private val catalogTempFile = File(context.applicationContext.filesDir, "iptvbox_catalog.json.tmp")
    private val metadataFile = File(context.applicationContext.filesDir, "iptvbox_metadata.json")
    private val metadataTempFile = File(context.applicationContext.filesDir, "iptvbox_metadata.json.tmp")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private var lastCatalogSignature: String = ""
    var lastLoadHadCatalogProblem: Boolean = false
        private set

    fun loadFast(): PersistedAppState {
        lastLoadHadCatalogProblem = false
        val storedPlaylists = runCatching {
            catalogStore.loadPlaylists()
        }.onFailure { lastLoadHadCatalogProblem = true }.getOrDefault(emptyList())
        val session = runCatching {
            if (sessionFile.isFile) json.decodeFromString<PersistedSessionState>(sessionFile.readText()) else null
        }.onFailure { lastLoadHadCatalogProblem = true }.getOrNull()
        val metadata = runCatching {
            if (metadataFile.isFile) json.decodeFromString<PersistedMetadataState>(metadataFile.readText()) else null
        }.onFailure { lastLoadHadCatalogProblem = true }.getOrNull()
        return PersistedAppState(
            playlists = storedPlaylists.ifEmpty { metadata?.playlists?.map { it.toLoadedPlaylist() }.orEmpty() },
            selectedPlaylistId = session?.selectedPlaylistId,
            selectedScreen = session?.selectedScreen,
            selectedTab = session?.selectedTab,
            selectedCategory = session?.selectedCategory,
            selectedSeriesTitle = session?.selectedSeriesTitle,
            selectedSeasonNumber = session?.selectedSeasonNumber,
            searchDraft = session?.searchDraft.orEmpty(),
            submittedSearch = session?.submittedSearch.orEmpty(),
            favoriteIds = session?.favoriteIds.orEmpty(),
            recentIds = session?.recentIds.orEmpty(),
        )
    }

    fun loadCatalog(): List<LoadedPlaylist> {
        lastLoadHadCatalogProblem = false
        val playlists = runCatching {
            catalogStore.loadPlaylists()
        }.onFailure { lastLoadHadCatalogProblem = true }.getOrDefault(emptyList())
        lastCatalogSignature = catalogSignature(playlists)
        return playlists
    }

    fun load(): PersistedAppState {
        val fast = loadFast()
        val fullPlaylists = loadCatalog()
        return PersistedAppState(
            playlists = fullPlaylists.ifEmpty { fast.playlists },
            selectedPlaylistId = fast.selectedPlaylistId,
            selectedScreen = fast.selectedScreen,
            selectedTab = fast.selectedTab,
            selectedCategory = fast.selectedCategory,
            selectedSeriesTitle = fast.selectedSeriesTitle,
            selectedSeasonNumber = fast.selectedSeasonNumber,
            searchDraft = fast.searchDraft,
            submittedSearch = fast.submittedSearch,
            favoriteIds = fast.favoriteIds,
            recentIds = fast.recentIds,
        )
    }

    suspend fun save(state: PersistedAppState) {
        withContext(Dispatchers.IO) {
            runCatching {
                val signature = catalogSignature(state.playlists)
                writeAtomically(
                    file = metadataFile,
                    tempFile = metadataTempFile,
                    text = json.encodeToString(PersistedMetadataState(state.playlists.map { it.toMetadata() })),
                )
                lastCatalogSignature = signature
                runCatching { if (legacyStateFile.exists()) legacyStateFile.delete() }
                runCatching { if (catalogFile.exists()) catalogFile.delete() }
                runCatching { if (catalogTempFile.exists()) catalogTempFile.delete() }
                writeAtomically(
                    file = sessionFile,
                    tempFile = sessionTempFile,
                    text = json.encodeToString(
                        PersistedSessionState(
                            selectedPlaylistId = state.selectedPlaylistId,
                            selectedScreen = state.selectedScreen,
                            selectedTab = state.selectedTab,
                            selectedCategory = state.selectedCategory,
                            selectedSeriesTitle = state.selectedSeriesTitle,
                            selectedSeasonNumber = state.selectedSeasonNumber,
                            searchDraft = state.searchDraft,
                            submittedSearch = state.submittedSearch,
                            favoriteIds = state.favoriteIds,
                            recentIds = state.recentIds,
                        ),
                    ),
                )
            }
        }
    }

    fun clear() {
        runCatching { catalogStore.clearAll() }
        listOf(
            legacyStateFile,
            sessionFile,
            sessionTempFile,
            catalogFile,
            catalogTempFile,
            metadataFile,
            metadataTempFile,
        ).forEach { file ->
            runCatching { if (file.exists()) file.delete() }
        }
        lastCatalogSignature = ""
        lastLoadHadCatalogProblem = false
    }

    private fun writeAtomically(file: File, tempFile: File, text: String) {
        tempFile.writeText(text)
        if (file.exists()) file.delete()
        tempFile.renameTo(file)
    }

    private fun LoadedPlaylist.toMetadata(): PersistedPlaylistMetadata {
        val stats = stats()
        return PersistedPlaylistMetadata(
            id = id,
            name = name,
            type = type,
            endpoint = endpoint,
            headers = headers,
            epgUrls = epgUrls,
            warnings = warnings,
            itemCount = items.size.takeIf { it > 0 } ?: cachedItemCount ?: 0,
            liveCount = stats.live,
            movieCount = stats.movies,
            seriesCount = stats.series,
        )
    }

    private fun PersistedPlaylistMetadata.toLoadedPlaylist(): LoadedPlaylist {
        return LoadedPlaylist(
            id = id,
            name = name,
            type = type,
            endpoint = endpoint,
            headers = headers,
            items = emptyList(),
            epgUrls = epgUrls,
            warnings = warnings,
            cachedItemCount = itemCount,
            cachedLiveCount = liveCount,
            cachedMovieCount = movieCount,
            cachedSeriesCount = seriesCount,
        )
    }
}
