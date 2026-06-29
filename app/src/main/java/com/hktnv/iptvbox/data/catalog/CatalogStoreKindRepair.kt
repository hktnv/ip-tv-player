package com.hktnv.iptvbox.data.catalog

import android.database.sqlite.SQLiteDatabase
import com.hktnv.iptvbox.core.model.ContentKind

private const val KindRepairKey = "kind_repair_version"
private const val CurrentKindRepairVersion = "1"

internal fun SQLiteDatabase.ensureCatalogMeta() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS catalog_meta(
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        )
        """.trimIndent(),
    )
}

internal fun SQLiteDatabase.repairStrongUrlKindHintsOnce() {
    if (catalogMetaValue(KindRepairKey) == CurrentKindRepairVersion) return
    beginTransaction()
    try {
        dropCatalogIndexes()
        repairLiveUrlRows()
        repairMovieUrlRows()
        refreshStoredPlaylistCounts()
        createCatalogIndexes()
        setCatalogMetaValue(KindRepairKey, CurrentKindRepairVersion)
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

private fun SQLiteDatabase.repairLiveUrlRows() {
    updateRows(
        """
        UPDATE items
        SET
            kind=?,
            series_title=NULL,
            season_number=NULL,
            episode_number=NULL,
            episode_title=NULL
        WHERE kind != ?
            AND (
                LOWER(stream_url) LIKE '%/live/%'
                OR (
                    (
                        LOWER(stream_url) LIKE '%.ts'
                        OR LOWER(stream_url) LIKE '%.ts?%'
                        OR LOWER(stream_url) LIKE '%.m3u8'
                        OR LOWER(stream_url) LIKE '%.m3u8?%'
                    )
                    AND LOWER(stream_url) NOT LIKE '%/movie/%'
                    AND LOWER(stream_url) NOT LIKE '%/series/%'
                )
            )
        """.trimIndent(),
        ContentKind.LIVE_CHANNEL.name,
        ContentKind.LIVE_CHANNEL.name,
    )
}

private fun SQLiteDatabase.repairMovieUrlRows() {
    updateRows(
        """
        UPDATE items
        SET
            kind=?,
            series_title=NULL,
            season_number=NULL,
            episode_number=NULL,
            episode_title=NULL
        WHERE kind != ?
            AND (
                LOWER(stream_url) LIKE '%/movie/%'
                OR (
                    (
                        LOWER(stream_url) LIKE '%.mp4'
                        OR LOWER(stream_url) LIKE '%.mp4?%'
                        OR LOWER(stream_url) LIKE '%.avi'
                        OR LOWER(stream_url) LIKE '%.avi?%'
                        OR LOWER(stream_url) LIKE '%.mov'
                        OR LOWER(stream_url) LIKE '%.mov?%'
                        OR LOWER(stream_url) LIKE '%.m4v'
                        OR LOWER(stream_url) LIKE '%.m4v?%'
                    )
                    AND LOWER(stream_url) NOT LIKE '%/live/%'
                    AND LOWER(stream_url) NOT LIKE '%/series/%'
                )
            )
        """.trimIndent(),
        ContentKind.MOVIE.name,
        ContentKind.MOVIE.name,
    )
}

private fun SQLiteDatabase.refreshStoredPlaylistCounts() {
    updateRows(
        """
        UPDATE playlists
        SET
            live_count = (
                SELECT COUNT(*)
                FROM items
                WHERE items.playlist_id = playlists.id
                    AND kind IN (?,?)
            ),
            movie_count = (
                SELECT COUNT(*)
                FROM items
                WHERE items.playlist_id = playlists.id
                    AND kind = ?
            ),
            series_count = (
                SELECT COUNT(DISTINCT series_title)
                FROM items
                WHERE items.playlist_id = playlists.id
                    AND kind IN (?,?,?)
                    AND series_title IS NOT NULL
                    AND series_title != ''
            )
        """.trimIndent(),
        ContentKind.LIVE_CHANNEL.name,
        ContentKind.RADIO.name,
        ContentKind.MOVIE.name,
        ContentKind.SERIES.name,
        ContentKind.SEASON.name,
        ContentKind.EPISODE.name,
    )
}

private fun SQLiteDatabase.catalogMetaValue(key: String): String? {
    return rawQuery(
        "SELECT value FROM catalog_meta WHERE key=?",
        arrayOf(key),
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}

private fun SQLiteDatabase.setCatalogMetaValue(key: String, value: String) {
    updateRows(
        "INSERT OR REPLACE INTO catalog_meta(key, value) VALUES(?,?)",
        key,
        value,
    )
}

private fun SQLiteDatabase.updateRows(sql: String, vararg args: String) {
    compileStatement(sql).use { statement ->
        args.forEachIndexed { index, value -> statement.bindString(index + 1, value) }
        statement.executeUpdateDelete()
    }
}
