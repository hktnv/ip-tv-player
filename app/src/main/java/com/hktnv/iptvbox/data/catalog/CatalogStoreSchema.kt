package com.hktnv.iptvbox.data.catalog

import android.database.sqlite.SQLiteDatabase

internal const val CatalogDatabaseVersion = 6

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
    createCategoryTable()
    execSQL(
        """
        CREATE TABLE items(
            playlist_id TEXT NOT NULL,
            item_id TEXT NOT NULL,
            kind TEXT NOT NULL,
            title TEXT NOT NULL,
            stream_url TEXT NOT NULL,
            category_id TEXT,
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
            added INTEGER,
            provider_order INTEGER NOT NULL,
            normalized_title TEXT NOT NULL,
            search_text TEXT NOT NULL,
            PRIMARY KEY(playlist_id, item_id),
            FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
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
    if (oldVersion < 5) {
        normalizeCategorySchema()
    }
    if (oldVersion < 6) {
        execSQL("ALTER TABLE items ADD COLUMN added INTEGER")
    }
}

internal fun SQLiteDatabase.createCategoryTable() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS categories(
            id TEXT PRIMARY KEY,
            playlist_id TEXT NOT NULL,
            name TEXT NOT NULL,
            xtream_category_id TEXT,
            kind TEXT NOT NULL,
            UNIQUE(playlist_id, kind, name),
            FOREIGN KEY(playlist_id) REFERENCES playlists(id) ON DELETE CASCADE
        )
        """.trimIndent(),
    )
}

private fun SQLiteDatabase.normalizeCategorySchema() {
    createCategoryTable()
    execSQL(
        """
        INSERT OR IGNORE INTO categories(id, playlist_id, name, kind)
        SELECT DISTINCT
            playlist_id || '|' ||
                CASE
                    WHEN kind IN ('LIVE_CHANNEL', 'RADIO') THEN 'LIVE'
                    WHEN kind = 'MOVIE' THEN 'MOVIE'
                    ELSE 'SERIES'
                END || '|' || COALESCE(NULLIF(category, ''), 'Genel') AS id,
            playlist_id,
            COALESCE(NULLIF(category, ''), 'Genel') AS name,
            CASE
                WHEN kind IN ('LIVE_CHANNEL', 'RADIO') THEN 'LIVE'
                WHEN kind = 'MOVIE' THEN 'MOVIE'
                ELSE 'SERIES'
            END AS kind
        FROM items
        """.trimIndent(),
    )
    execSQL(
        """
        CREATE TABLE items_v5(
            playlist_id TEXT NOT NULL,
            item_id TEXT NOT NULL,
            kind TEXT NOT NULL,
            title TEXT NOT NULL,
            stream_url TEXT NOT NULL,
            category_id TEXT,
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
            PRIMARY KEY(playlist_id, item_id),
            FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
        )
        """.trimIndent(),
    )
    execSQL(
        """
        INSERT INTO items_v5(
            playlist_id, item_id, kind, title, stream_url, category_id, logo_url, tvg_id, tvg_name,
            series_title, season_number, episode_number, episode_title, xtream_id, rating, tmdb_id,
            provider_order, normalized_title, search_text
        )
        SELECT
            playlist_id, item_id, kind, title, stream_url,
            playlist_id || '|' ||
                CASE
                    WHEN kind IN ('LIVE_CHANNEL', 'RADIO') THEN 'LIVE'
                    WHEN kind = 'MOVIE' THEN 'MOVIE'
                    ELSE 'SERIES'
                END || '|' || COALESCE(NULLIF(category, ''), 'Genel') AS category_id,
            logo_url, tvg_id, tvg_name, series_title, season_number, episode_number, episode_title,
            xtream_id, rating, tmdb_id, provider_order, normalized_title, search_text
        FROM items
        """.trimIndent(),
    )
    execSQL("DROP TABLE items")
    execSQL("ALTER TABLE items_v5 RENAME TO items")
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
