package com.hktnv.iptvbox

import com.hktnv.iptvbox.data.catalog.queuedXtreamCategoriesSql
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamCategoryQueueSqlTest {
    @Test
    fun queueOnlySelectsCategoriesWithMissingXtreamItems() {
        val sql = queuedXtreamCategoriesSql().replace("\\s+".toRegex(), " ")

        assertTrue(sql.contains("EXISTS ( SELECT 1 FROM items"))
        assertTrue(sql.contains("items.category_id = categories.id"))
        assertTrue(sql.contains("items.xtream_id IS NULL"))
    }
}
