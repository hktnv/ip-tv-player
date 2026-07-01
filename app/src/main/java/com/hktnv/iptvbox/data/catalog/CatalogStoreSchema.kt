package com.hktnv.iptvbox.data.catalog

import android.database.sqlite.SQLiteDatabase

internal const val CatalogDatabaseVersion = 4

internal fun SQLiteDatabase.createCatalogTables() {
    execSQL(
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
            auto_update_hours INTEGER NOT NULL DEFAULT 0,
            xtream_api_supported INTEGER NOT NULL DEFAULT 0,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent(),
    )
    execSQL(
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
            xtream_id INTEGER,
            rating TEXT,
            tmdb_id INTEGER,
            provider_order INTEGER NOT NULL,
            normalized_title TEXT NOT NULL,
            search_text TEXT NOT NULL,
            PRIMARY KEY(playlist_id, item_id)
        )
        """.trimIndent(),
    )
    createMetadataCacheTable()
}

internal fun SQLiteDatabase.upgradeCatalogSchema(oldVersion: Int) {
    if (oldVersion < 2) {
        execSQL("ALTER TABLE playlists ADD COLUMN auto_update_hours INTEGER NOT NULL DEFAULT 0")
    }
    if (oldVersion < 3) {
        execSQL("ALTER TABLE playlists ADD COLUMN xtream_api_supported INTEGER NOT NULL DEFAULT 0")
        execSQL("ALTER TABLE items ADD COLUMN xtream_id INTEGER")
        execSQL("ALTER TABLE items ADD COLUMN rating TEXT")
        execSQL("ALTER TABLE items ADD COLUMN tmdb_id INTEGER")
        createMetadataCacheTable()
    }
    if (oldVersion < 4) {
        execSQL("ALTER TABLE items ADD COLUMN normalized_title TEXT NOT NULL DEFAULT ''")
        execSQL("DROP TABLE IF EXISTS metadata_cache")
        createMetadataCacheTable()
    }
}

internal fun SQLiteDatabase.createMetadataCacheTable() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS metadata_cache(
            cache_key TEXT PRIMARY KEY,
            tmdb_id INTEGER,
            normalized_title TEXT NOT NULL,
            plot TEXT,
            cast TEXT,
            director TEXT,
            youtube_trailer TEXT,
            duration TEXT,
            backdrop_url TEXT,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent(),
    )
    execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS idx_metadata_cache_tmdb ON metadata_cache(tmdb_id) WHERE tmdb_id IS NOT NULL",
    )
    execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_cache_title ON metadata_cache(normalized_title)")
}
