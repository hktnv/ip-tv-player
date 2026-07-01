package com.hktnv.iptvbox.data.catalog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import com.hktnv.iptvbox.core.common.SearchNormalizer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.data.playlist.xtream.XtreamCategoryMapping
import com.hktnv.iptvbox.core.model.PlaylistSourceType
import com.hktnv.iptvbox.model.CatalogTab
import com.hktnv.iptvbox.model.LoadedPlaylist
import com.hktnv.iptvbox.model.SeriesGroup

internal class CatalogStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "iptvbox_catalog.db",
    null,
    CatalogDatabaseVersion,
) {
    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("PRAGMA synchronous=NORMAL")
        db.execSQL("PRAGMA temp_store=MEMORY")
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.ensureCatalogMeta()
        db.repairStrongUrlKindHintsOnce()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createCatalogTables()
        db.createCatalogIndexes()
        db.ensureCatalogMeta()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.upgradeCatalogSchema(oldVersion)
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

    fun replacePlaylistMeasured(
        playlist: LoadedPlaylist,
        xtreamCategoryMappings: List<XtreamCategoryMapping> = emptyList(),
    ): CatalogWriteResult {
        val timings = linkedMapOf<String, Long>()
        val totalStartedNs = System.nanoTime()
        val stored = measureDb("counter_calc_ms", timings) { playlist.withCachedStoreStats() }
        val categories = buildCategoryRows(stored.id, stored.items, xtreamCategoryMappings)
        val db = writableDatabase
        measureDb("transaction_begin_ms", timings) { db.beginTransaction() }
        try {
            measureDb("drop_indexes_ms", timings) { db.dropCatalogIndexes() }
            measureDb("delete_ms", timings) {
                db.delete("items", "playlist_id=?", arrayOf(stored.id))
                db.delete("categories", "playlist_id=?", arrayOf(stored.id))
                db.delete("playlists", "id=?", arrayOf(stored.id))
            }
            measureDb("metadata_save_ms", timings) {
                db.insertWithOnConflict("playlists", null, stored.toPlaylistValues(), SQLiteDatabase.CONFLICT_REPLACE)
                db.insertCategoryRows(categories.values, stored.id)
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
                        category_id,
                        logo_url,
                        tvg_id,
                        tvg_name,
                        series_title,
                        season_number,
                        episode_number,
                        episode_title,
                        xtream_id,
                        rating,
                        tmdb_id,
                        added,
                        provider_order,
                        normalized_title,
                        search_text
                    ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """.trimIndent(),
                ).use { statement ->
                    stored.items.forEach { item ->
                        statement.clearBindings()
                        statement.bindString(1, stored.id)
                        statement.bindString(2, item.id)
                        statement.bindString(3, item.kind.name)
                        statement.bindString(4, item.title)
                        statement.bindString(5, item.streamUrl)
                        statement.bindNullableString(
                            6,
                            categories[catalogCategoryId(stored.id, item.categoryKindForStore(), item.categoryNameForStore())]?.id,
                        )
                        statement.bindNullableString(7, item.logoUrl)
                        statement.bindNullableString(8, item.tvgId)
                        statement.bindNullableString(9, item.tvgName)
                        statement.bindNullableString(10, item.seriesTitle)
                        statement.bindNullableLong(11, item.seasonNumber?.toLong())
                        statement.bindNullableLong(12, item.episodeNumber?.toLong())
                        statement.bindNullableString(13, item.episodeTitle)
                        statement.bindNullableLong(14, item.xtreamId?.toLong())
                        statement.bindNullableString(15, item.rating)
                        statement.bindNullableLong(16, item.tmdbId?.toLong())
                        statement.bindNullableLong(17, item.addedSecondsForStore())
                        statement.bindLong(18, item.providerOrder.toLong())
                        statement.bindString(19, item.normalizedTitleForStore())
                        statement.bindString(20, item.searchTextForStore())
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

    fun updatePlaylistAutoUpdateHours(playlistId: String, hours: Int): LoadedPlaylist? {
        writableDatabase.update(
            "playlists",
            ContentValues().apply {
                put("auto_update_hours", hours.coerceAtLeast(0))
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

    fun itemIds(playlistId: String): Set<String> {
        return readableDatabase.rawQuery(
            "SELECT item_id FROM items WHERE playlist_id=?",
            arrayOf(playlistId),
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        writableDatabase.transaction {
            delete("items", "playlist_id=?", arrayOf(playlistId))
            delete("categories", "playlist_id=?", arrayOf(playlistId))
            delete("playlists", "id=?", arrayOf(playlistId))
        }
    }

    fun clearAll() {
        writableDatabase.transaction {
            delete("metadata_cache", null, null)
            delete("items", null, null)
            delete("categories", null, null)
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
            itemSelect("WHERE items.playlist_id=? ORDER BY items.provider_order ASC"),
            arrayOf(playlistId),
        ).use { cursor -> cursor.toItems() }
    }

    fun loadItems(playlistId: String, tab: CatalogTab, category: String? = null, limit: Int = 500): List<CatalogItem> {
        val kindPlaceholders = itemKindPlaceholders(tab.kinds.size)
        val args = mutableListOf(playlistId).apply {
            addAll(tab.kinds.map { it.name })
            if (category != null) add(category)
            add(limit.toString())
        }
        val categoryClause = if (category == null) "" else " AND categories.name=?"
        return readableDatabase.rawQuery(
            itemSelect(
                """
                WHERE items.playlist_id=? AND items.kind IN ($kindPlaceholders)$categoryClause
                ORDER BY items.provider_order ASC
                LIMIT ?
                """.trimIndent(),
            ),
            args.toTypedArray(),
        ).use { cursor -> cursor.toItems() }
    }

    fun itemsByIds(playlistId: String, ids: List<String>): List<CatalogItem> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val byId = readableDatabase.rawQuery(
            itemSelect("WHERE items.playlist_id=? AND items.item_id IN ($placeholders)"),
            (listOf(playlistId) + ids).toTypedArray(),
        ).use { cursor -> cursor.toItems().associateBy { it.id } }
        return ids.mapNotNull(byId::get)
    }

    fun search(playlistId: String, query: String, limit: Int): List<CatalogItem> {
        val normalized = SearchNormalizer.normalize(query)
        if (normalized.isBlank()) return emptyList()
        return readableDatabase.rawQuery(
            itemSelect(
                """
                WHERE items.playlist_id=? AND items.search_text LIKE ?
                ORDER BY items.provider_order ASC
                LIMIT ?
                """.trimIndent(),
            ),
            arrayOf(playlistId, "%$normalized%", limit.toString()),
        ).use { cursor -> cursor.toItems() }
    }

    fun seriesGroups(playlistId: String, category: String?, limit: Int): List<SeriesGroup> {
        val categoryClause = if (category == null) "" else " AND categories.name=?"
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
                items.series_title,
                MIN(categories.name) AS category,
                MIN(items.logo_url) AS logo_url,
                COUNT(DISTINCT COALESCE(items.season_number, 1)) AS season_count,
                COUNT(*) AS episode_count,
                MIN(items.provider_order) AS first_order
            FROM items
            LEFT JOIN categories ON categories.id = items.category_id
            WHERE items.playlist_id=?
                AND items.kind IN (?,?,?)
                AND items.series_title IS NOT NULL
                AND items.series_title != ''
                $categoryClause
            GROUP BY items.series_title
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
