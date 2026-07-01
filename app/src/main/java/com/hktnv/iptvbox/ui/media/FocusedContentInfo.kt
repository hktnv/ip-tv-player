package com.hktnv.iptvbox.ui.media

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.model.SeasonGroup
import com.hktnv.iptvbox.model.SeriesGroup
import com.hktnv.iptvbox.ui.catalog.label

internal data class FocusedContentInfo(
    val title: String,
    val type: String,
    val category: String?,
    val detail: String?,
)

internal fun CatalogItem.focusedContentInfo(): FocusedContentInfo {
    val meta = metaLine()
    val cleanCategory = category?.cleanUiTitle()?.ifBlank { null }
    return FocusedContentInfo(
        title = displayTitle(),
        type = kind.label(),
        category = cleanCategory,
        detail = meta.takeUnless { it == cleanCategory || it.isBlank() },
    )
}

internal fun SeriesGroup.focusedContentInfo(): FocusedContentInfo {
    return FocusedContentInfo(
        title = title.readableContentTitle(),
        type = "Dizi",
        category = category?.cleanUiTitle()?.ifBlank { null },
        detail = "$seasonCount sezon · $episodeCount bölüm",
    )
}

internal fun SeasonGroup.focusedContentInfo(): FocusedContentInfo {
    return FocusedContentInfo(
        title = title.readableContentTitle(),
        type = "Sezon",
        category = null,
        detail = "$episodeCount bölüm",
    )
}
