package com.hktnv.iptvbox.data.catalog

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.hktnv.iptvbox.core.model.ContentMetadata

internal fun CatalogStore.loadMetadata(playlistId: String, itemId: String): ContentMetadata? {
    return readableDatabase.rawQuery(
        "SELECT * FROM metadata_cache WHERE playlist_id=? AND item_id=?",
        arrayOf(playlistId, itemId),
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        ContentMetadata(
            itemId = cursor.getString(cursor.column("item_id")),
            plot = cursor.nullableString("plot"),
            cast = cursor.nullableString("cast"),
            director = cursor.nullableString("director"),
            youtubeTrailer = cursor.nullableString("youtube_trailer"),
            duration = cursor.nullableString("duration"),
            backdropUrl = cursor.nullableString("backdrop_url"),
        )
    }
}

internal fun CatalogStore.saveMetadata(playlistId: String, metadata: ContentMetadata) {
    writableDatabase.insertWithOnConflict(
        "metadata_cache",
        null,
        ContentValues().apply {
            put("playlist_id", playlistId)
            put("item_id", metadata.itemId)
            put("plot", metadata.plot)
            put("cast", metadata.cast)
            put("director", metadata.director)
            put("youtube_trailer", metadata.youtubeTrailer)
            put("duration", metadata.duration)
            put("backdrop_url", metadata.backdropUrl)
            put("updated_at", System.currentTimeMillis())
        },
        SQLiteDatabase.CONFLICT_REPLACE,
    )
}
