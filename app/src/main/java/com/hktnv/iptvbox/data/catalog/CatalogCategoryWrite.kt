package com.hktnv.iptvbox.data.catalog

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.data.playlist.xtream.XtreamCategoryMapping

internal fun buildCategoryRows(
    playlistId: String,
    items: List<CatalogItem>,
    mappings: List<XtreamCategoryMapping>,
): Map<String, CatalogCategoryRow> {
    val xtreamByCategory = mappings.associateBy { categoryMappingKey(it.kind, it.localName) }
    return buildMap {
        items.forEach { item ->
            val kind = item.categoryKindForStore()
            val name = item.categoryNameForStore()
            val id = catalogCategoryId(playlistId, kind, name)
            if (containsKey(id)) return@forEach
            val mapping = xtreamByCategory[categoryMappingKey(kind, name)]
            put(
                id,
                CatalogCategoryRow(
                    id = id,
                    name = name,
                    kind = kind,
                    xtreamCategoryId = mapping?.xtreamCategoryId,
                ),
            )
        }
    }
}

internal fun SQLiteDatabase.insertCategoryRows(rows: Collection<CatalogCategoryRow>, playlistId: String) {
    compileStatement(
        """
        INSERT OR REPLACE INTO categories(
            id,
            playlist_id,
            name,
            xtream_category_id,
            kind
        ) VALUES(?,?,?,?,?)
        """.trimIndent(),
    ).use { statement ->
        rows.forEach { row ->
            statement.clearBindings()
            statement.bindString(1, row.id)
            statement.bindString(2, playlistId)
            statement.bindString(3, row.name)
            statement.bindNullableString(4, row.xtreamCategoryId)
            statement.bindString(5, row.kind)
            statement.executeInsert()
        }
    }
}
