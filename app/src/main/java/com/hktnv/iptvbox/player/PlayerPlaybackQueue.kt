package com.hktnv.iptvbox.player

import android.view.KeyEvent
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind

internal data class PlayerPlaybackQueue(
    val items: List<CatalogItem>,
    val currentIndex: Int,
) {
    val current: CatalogItem?
        get() = items.getOrNull(currentIndex)

    val previous: CatalogItem?
        get() = items.getOrNull(currentIndex - 1)

    val next: CatalogItem?
        get() = items.getOrNull(currentIndex + 1)
}

internal enum class PlayerRemoteCommand {
    TogglePlayPause,
    NextItem,
    PreviousItem,
    Left,
    Right,
    None,
}

internal fun buildPlayerPlaybackQueue(
    contextItems: List<CatalogItem>,
    currentItem: CatalogItem,
): PlayerPlaybackQueue {
    val uniqueItems = LinkedHashMap<String, CatalogItem>()
    contextItems
        .filter { it.streamUrl.isNotBlank() && it.belongsToPlaybackContextOf(currentItem) }
        .forEach { uniqueItems[it.id] = it }
    if (currentItem.streamUrl.isNotBlank() && currentItem.id !in uniqueItems) {
        uniqueItems[currentItem.id] = currentItem
    }
    val items = uniqueItems.values.toList()
    val currentIndex = items.indexOfFirst { it.id == currentItem.id }.coerceAtLeast(0)
    return PlayerPlaybackQueue(items = items, currentIndex = currentIndex)
}

internal fun PlayerPlaybackQueue.relatedItems(): List<CatalogItem> {
    return items.filterIndexed { index, _ -> index != currentIndex }
}

private fun CatalogItem.belongsToPlaybackContextOf(currentItem: CatalogItem): Boolean {
    return when (currentItem.kind) {
        ContentKind.LIVE_CHANNEL,
        ContentKind.RADIO -> kind == currentItem.kind
        ContentKind.MOVIE -> kind == ContentKind.MOVIE
        ContentKind.EPISODE -> kind == ContentKind.EPISODE &&
            seriesContextKey() == currentItem.seriesContextKey()
        ContentKind.SERIES,
        ContentKind.SEASON -> kind == currentItem.kind
    }
}

private fun CatalogItem.seriesContextKey(): String {
    return seriesTitle.orEmpty()
        .ifBlank { title.substringBefore(" S", title).substringBefore(" Sezon", title) }
        .trim()
        .lowercase()
}

internal fun playerRemoteCommandForKeyCode(keyCode: Int): PlayerRemoteCommand {
    return when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> PlayerRemoteCommand.TogglePlayPause
        KeyEvent.KEYCODE_DPAD_UP -> PlayerRemoteCommand.NextItem
        KeyEvent.KEYCODE_DPAD_DOWN -> PlayerRemoteCommand.PreviousItem
        KeyEvent.KEYCODE_DPAD_LEFT -> PlayerRemoteCommand.Left
        KeyEvent.KEYCODE_DPAD_RIGHT -> PlayerRemoteCommand.Right
        else -> PlayerRemoteCommand.None
    }
}
