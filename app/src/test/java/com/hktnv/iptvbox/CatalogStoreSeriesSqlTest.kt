package com.hktnv.iptvbox

import com.hktnv.iptvbox.data.catalog.seasonFilterClause
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogStoreSeriesSqlTest {
    @Test
    fun seasonFilterCastsBoundSeasonNumberToInteger() {
        assertEquals(
            " AND COALESCE(items.season_number, 1)=CAST(? AS INTEGER)",
            seasonFilterClause(1),
        )
    }

    @Test
    fun seasonFilterIsEmptyWhenSeasonIsNotSelected() {
        assertEquals("", seasonFilterClause(null))
    }
}
