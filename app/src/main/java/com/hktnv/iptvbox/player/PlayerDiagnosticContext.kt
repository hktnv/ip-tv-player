package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.core.model.CatalogItem
import java.net.URI

internal data class PlayerDiagnosticContext(
    val type: String,
    val category: String,
    val title: String,
    val media: String,
)

internal fun CatalogItem.toPlayerDiagnosticContext(): PlayerDiagnosticContext {
    val info = toPlayerContentInfo()
    return PlayerDiagnosticContext(
        type = info.typeLabel,
        category = info.category,
        title = info.title,
        media = streamUrl.toDiagnosticMediaHint(),
    )
}

internal fun String.toDiagnosticMediaHint(): String {
    val uri = runCatching { URI(this) }.getOrNull()
    val scheme = uri?.scheme.orEmpty().ifBlank { "unknown" }
    val host = uri?.host.orEmpty().ifBlank { "unknown" }
    val lastSegment = uri?.path.orEmpty()
        .substringAfterLast('/')
        .takeIf { it.isNotBlank() }
        ?.replace(Regex("""[A-Za-z0-9]{12,}"""), "***")
        ?: "***"
    return "$scheme://$host/.../$lastSegment"
}
