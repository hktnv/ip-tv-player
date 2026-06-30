package com.hktnv.iptvbox.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.player.IptvPlaybackBufferKind
import com.hktnv.iptvbox.core.player.MediaPlayerFactory

internal data class PlayerSessionKey(
    val headersSignature: String,
    val bufferKind: IptvPlaybackBufferKind,
)

@Composable
internal fun rememberIptvPlayerSession(
    context: Context,
    headers: Map<String, String>,
    item: CatalogItem,
): ExoPlayer {
    val sessionKey = remember(headers, item.kind) {
        playerSessionKey(headers, item)
    }
    return remember(sessionKey) {
        MediaPlayerFactory.create(
            context = context,
            headers = headers,
            bufferKind = sessionKey.bufferKind,
        ).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }
}

internal fun playerSessionKey(
    headers: Map<String, String>,
    item: CatalogItem,
): PlayerSessionKey {
    return PlayerSessionKey(
        headersSignature = headers.toStableHeaderSignature(),
        bufferKind = item.toPlaybackBufferKind(),
    )
}

internal fun CatalogItem.toPlayerMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(streamUrl)
        .setMediaId(id)
        .build()
}

private fun Map<String, String>.toStableHeaderSignature(): String {
    return entries
        .sortedBy { it.key }
        .joinToString(separator = "\n") { (key, value) -> "$key=$value" }
}
