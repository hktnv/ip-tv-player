package com.evomrdm.iptvbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.core.model.ContentKind

@Composable
internal fun ContentGrid(
    items: List<CatalogItem>,
    favoriteIds: List<String>,
    onOpenItem: (CatalogItem) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val mostlyLive = remember(items) {
            val sample = items.take(18)
            sample.isNotEmpty() && sample.count { it.kind in CatalogTab.LIVE.kinds } >= sample.size * 2 / 3
        }
        val favoriteKey = favoriteIds.joinToString("|")
        val favoriteSet = remember(favoriteKey) { favoriteIds.toSet() }
        val minCell = when {
            mostlyLive && maxWidth < 600.dp -> 112.dp
            mostlyLive -> 150.dp
            maxWidth >= 900.dp -> 178.dp
            maxWidth >= 600.dp -> 158.dp
            else -> 145.dp
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minCell),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = ScreenBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.id }, contentType = { it.kind.name }) { item ->
                ContentCard(
                    item = item,
                    favorite = item.id in favoriteSet,
                    onOpen = { onOpenItem(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                )
            }
        }
    }
}
