package com.hktnv.iptvbox.data.catalog

internal fun itemSelect(whereClause: String): String {
    return """
        SELECT items.*, categories.name AS category
        FROM items
        LEFT JOIN categories ON categories.id = items.category_id
        $whereClause
    """.trimIndent()
}

internal fun itemKindPlaceholders(count: Int): String = List(count) { "?" }.joinToString(",")
