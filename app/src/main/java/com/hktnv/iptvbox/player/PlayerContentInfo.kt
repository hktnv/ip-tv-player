package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind

internal data class PlayerContentInfo(
    val typeLabel: String,
    val category: String,
    val title: String,
)

internal fun CatalogItem.toPlayerContentInfo(): PlayerContentInfo {
    return PlayerContentInfo(
        typeLabel = kind.playerTypeLabel(),
        category = category.cleanPlayerText().ifBlank { kind.playerTypeLabel() },
        title = title.cleanPlayerText()
            .ifBlank { tvgName.cleanPlayerText() }
            .ifBlank { seriesTitle.cleanPlayerText() }
            .ifBlank { kind.playerTypeLabel() },
    )
}

internal fun shouldShowPlayerContentInfo(
    controlsVisible: Boolean,
    exitDialogState: PlayerExitDialogState,
): Boolean {
    return controlsVisible && exitDialogState == PlayerExitDialogState.Hidden
}

private fun ContentKind.playerTypeLabel(): String {
    return when (this) {
        ContentKind.LIVE_CHANNEL,
        ContentKind.RADIO -> "Canlı TV"
        ContentKind.MOVIE -> "Film"
        ContentKind.SERIES,
        ContentKind.SEASON,
        ContentKind.EPISODE -> "Dizi"
    }
}

private fun String?.cleanPlayerText(): String {
    return orEmpty()
        .replace(Regex("""#EXTINF[^,]*,?""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("[\\w-]+=\"[^\"]*\"", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""https?://\S+""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim { it == ' ' || it == ',' || it == '-' || it == '|' }
}
