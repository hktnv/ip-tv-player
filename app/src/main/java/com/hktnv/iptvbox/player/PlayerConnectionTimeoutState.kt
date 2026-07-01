package com.hktnv.iptvbox.player

internal data class PlayerConnectionAttemptSnapshot(
    val awaitingConnection: Boolean,
    val elapsedMs: Long,
)

internal data class PlayerConnectionTimeoutUiState(
    val showLoading: Boolean,
    val message: String?,
    val showTimeoutDialog: Boolean,
)

internal fun playerConnectionTimeoutUiState(
    awaitingConnection: Boolean,
    elapsedMs: Long,
    timeoutDismissed: Boolean,
): PlayerConnectionTimeoutUiState {
    if (!awaitingConnection) {
        return PlayerConnectionTimeoutUiState(
            showLoading = false,
            message = null,
            showTimeoutDialog = false,
        )
    }
    return PlayerConnectionTimeoutUiState(
        showLoading = true,
        message = playerConnectionWaitMessage(elapsedMs),
        showTimeoutDialog = elapsedMs >= ConnectionTimeoutDialogMs && !timeoutDismissed,
    )
}

private fun playerConnectionWaitMessage(elapsedMs: Long): String? {
    return when {
        elapsedMs < SlowSourceMessageMs -> null
        elapsedMs < BusyServerMessageMs -> "Yayın kaynağı yavaş yanıt veriyor, bağlanmaya çalışıyoruz..."
        else -> "Yayın sunucusu yoğun olabilir, bağlantı sürdürülüyor..."
    }
}

private const val SlowSourceMessageMs = 4_000L
private const val BusyServerMessageMs = 8_000L
private const val ConnectionTimeoutDialogMs = 15_000L
