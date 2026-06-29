package com.hktnv.iptvbox.data.catalog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.SeriesGroup

internal class CatalogStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "iptvbox_catalog.db",
    null,
    1,
) {
    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.execSQL("PRAGMA synchronous=NORMAL")
        db.execSQL("PRAGMA temp_store=MEMORY")
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.ensureCatalogMeta()
        db.repairStrongUrlKindHintsOnce()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE playlists(
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                endpoint TEXT NOT NULL,
                headers_json TEXT NOT NULL,
                epg_json TEXT NOT NULL,
                warnings_json TEXT NOT NULL,
                item_count INTEGER NOT NULL,
                live_count INTEGER NOT NULL,
                movie_count INTEGER NOT NULL,
                series_count INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE items(
                playlist_id TEXT NOT NULL,
                item_id TEXT NOT NULL,
                kind TEXT NOT NULL,
                title TEXT NOT NULL,
                stream_url TEXT NOT NULL,
                category TEXT,
                logo_url TEXT,
                tvg_id TEXT,
                tvg_name TEXT,
                series_title TEXT,
                season_number INTEGER,
                episode_number INTEGER,
                episode_title TEXT,
                provider_order INTEGER NOT NULL,
                search_text TEXT NOT NULL,
                PRIMARY KEY(playlist_id, item_id)
            )
            """.trimIndent(),
        )
        db.createCatalogIndexes()
        db.ensureCatalogMeta()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS items")
        db.execSQL("DROP TABLE IF EXISTS playlists")
        onCreate(db)
    }

    fun loadPlaylists(): List<LoadedPlaylist> {
        return readableDatabase.rawQuery(
            "SELECT * FROM playlists ORDER BY updated_at DESC",
            emptyArray(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toPlaylist())
            }
        }
    }

    fun hasItems(playlistId: String): Boolean {
        return readableDatabase.rawQuery(
            "SELECT 1 FROM items WHERE playlist_id=? LIMIT 1",
            arrayOf(playlistId),
        ).use { it.moveToFirst() }
    }

    fun replacePlaylist(playlist: LoadedPlaylist): LoadedPlaylist = replacePlaylistMeasured(playlist).playlist

    fun replacePlaylistMeasured(playlist: LoadedPlaylist): CatalogWriteResult {
        val timings = linkedMapOf<String, Long>()
        val totalStartedNs = System.nanoTime()
        val stored = measureDb("counter_calc_ms", timings) { playlist.withCachedStoreStats() }
        val db = writableDatabase
        measureDb("transaction_begin_ms", timings) { db.beginTransaction() }
        try {
            measureDb("drop_indexes_ms", timings) { db.dropCatalogIndexes() }
            measureDb("delete_ms", timings) {
                db.delete("items", "playlist_id=?", arrayOf(stored.id))
                db.delete("playlists", "id=?", arrayOf(stored.id))
            }
            measureDb("metadata_save_ms", timings) {
                db.insertWithOnConflict("playlists", null, stored.toPlaylistValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            measureDb("write_ms", timings) {
                db.compileStatement(
                    """
                    INSERT OR REPLACE INTO items(
                        playlist_id,
                        item_id,
                        kind,
                        title,
                        stream_url,
                        category,
                        logo_url,
                        tvg_id,
                        tvg_name,
                        series_title,
                        season_number,
                        episode_number,
                        episode_title,
                        provider_order,
                        search_text
                    ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """.trimIndent(),
                ).use { statement ->
                    stored.items.forEach { item ->
                        statement.clearBindings()
                        statement.bindString(1, stored.id)
                        statement.bindString(2, item.id)
                        statement.bindString(3, item.kind.name)
                        statement.bindString(4, item.title)
                        statement.bindString(5, item.streamUrl)
                        statement.bindNullableString(6, item.category)
                        statement.bindNullableString(7, item.logoUrl)
                        statement.bindNullableString(8, item.tvgId)
                        statement.bindNullableString(9, item.tvgName)
                        statement.bindNullableString(10, item.seriesTitle)
                        statement.bindNullableLong(11, item.seasonNumber?.toLong())
                        statement.bindNullableLong(12, item.episodeNumber?.toLong())
                        statement.bindNullableString(13, item.episodeTitle)
                        statement.bindLong(14, item.providerOrder.toLong())
                        statement.bindString(15, item.searchTextForStore())
                        statement.executeInsert()
                    }
                }
            }
            measureDb("rebuild_indexes_ms", timings) { db.createCatalogIndexes() }
            db.setTransactionSuccessful()
        } finally {
            measureDb("transaction_end_ms", timings) { db.endTransaction() }
        }
        timings["total_ms"] = elapsedMs(totalStartedNs)
        return CatalogWriteResult(stored.copy(items = emptyList()), timings)
    }

    fun updatePlaylistName(playlistId: String, name: String): LoadedPlaylist? {
        writableDatabase.update(
            "playlists",
            ContentValues().apply {
                put("name", name)
                put("updated_at", System.currentTimeMillis())
            },
            "id=?",
            arrayOf(playlistId),
        )
        return readableDatabase.rawQuery(
            "SELECT * FROM playlists WHERE id=?",
            arrayOf(playlistId),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toPlaylist() else null
        }
    }

    fun deletePlaylist(playlistId: String) {
        writableDatabase.transaction {
            delete("items", "playlist_id=?", arrayOf(playlistId))
            delete("playlists", "id=?", arrayOf(playlistId))
        }
    }

    fun clearAll() {
        writableDatabase.transaction {
            delete("items", null, null)
            delete("playlists", null, null)
        }
    }

    fun loadPlaylistWithItems(playlistId: String): LoadedPlaylist? {
        val metadata = readableDatabase.rawQuery(
            "SELECT * FROM playlists WHERE id=?",
            arrayOf(playlistId),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toPlaylist() else null
        } ?: return null
        return metadata.copy(items = loadItems(playlistId))
    }

    fun loadItems(playlistId: String): List<CatalogItem> {
        return readableDatabase.rawQuery(
            "SELECT * FROM items WHERE playlist_id=? ORDER BY provider_order ASC",
            arrayOf(playlistId),
        ).use { cursor -> cursor.toItems() }
    }

    fun loadItems(playlistId: String, tab: CatalogTab, category: String? = null, limit: Int = 500): List<CatalogItem> {
        val kindPlaceholders = tab.kinds.joinToString(",") { "?" }
        val args = mutableListOf(playlistId).apply {
            addAll(tab.kinds.map { it.name })
            if (category != null) add(category)
            add(limit.toString())
        }
        val categoryClause = if (category == null) "" else " AND category=?"
        return readableDatabase.rawQuery(
            """
            SELECT * FROM items
            WHERE playlist_id=? AND kind IN ($kindPlaceholders)$categoryClause
            ORDER BY provider_order ASC
            LIMIT ?
            """.trimIndent(),
            args.toTypedArray(),
        ).use { cursor -> cursor.toItems() }
    }

    fun itemsByIds(playlistId: String, ids: List<String>): List<CatalogItem> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val byId = readableDatabase.rawQuery(
            "SELECT * FROM items WHERE playlist_id=? AND item_id IN ($placeholders)",
            (listOf(playlistId) + ids).toTypedArray(),
        ).use { cursor -> cursor.toItems().associateBy { it.id } }
        return ids.mapNotNull(byId::get)
    }

    fun search(playlistId: String, query: String, limit: Int): List<CatalogItem> {
        val normalized = SearchNormalizer.normalize(query)
        if (normalized.isBlank()) return emptyList()
        return readableDatabase.rawQuery(
            """
            SELECT * FROM items
            WHERE playlist_id=? AND search_text LIKE ?
            ORDER BY provider_order ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(playlistId, "%$normalized%", limit.toString()),
        ).use { cursor -> cursor.toItems() }
    }

    fun seriesGroups(playlistId: String, category: String?, limit: Int): List<SeriesGroup> {
        val categoryClause = if (category == null) "" else " AND category=?"
        val args = mutableListOf(
            playlistId,
            ContentKind.SERIES.name,
            ContentKind.SEASON.name,
            ContentKind.EPISODE.name,
        ).apply {
            if (category != null) add(category)
            add(limit.toString())
        }
        return readableDatabase.rawQuery(
            """
            SELECT
                series_title,
                MIN(category) AS category,
                MIN(logo_url) AS logo_url,
                COUNT(DISTINCT COALESCE(season_number, 1)) AS season_count,
                COUNT(*) AS episode_count,
                MIN(provider_order) AS first_order
            FROM items
            WHERE playlist_id=?
                AND kind IN (?,?,?)
                AND series_title IS NOT NULL
                AND series_title != ''
                $categoryClause
            GROUP BY series_title
            ORDER BY first_order ASC, series_title COLLATE NOCASE ASC
            LIMIT ?
            """.trimIndent(),
            args.toTypedArray(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(0)
                    add(
                        SeriesGroup(
                            id = SearchNormalizer.normalize(title).ifBlank { title },
                            title = title,
                            category = cursor.nullableString("category"),
                            logoUrl = cursor.nullableString("logo_url"),
                            seasonCount = cursor.getInt(cursor.column("season_count")),
                            episodeCount = cursor.getInt(cursor.column("episode_count")),
                            firstOrder = cursor.getInt(cursor.column("first_order")),
                        ),
                    )
                }
            }
        }
    }

}
