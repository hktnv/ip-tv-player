package com.hktnv.iptvbox.player

internal class PlayerBackPressGuard(
    private val duplicateWindowMs: Long = DEFAULT_DUPLICATE_WINDOW_MS,
) {
    private var suppressExitBackUntilMs: Long = Long.MIN_VALUE

    fun markOverlayBackHandled(nowMs: Long) {
        suppressExitBackUntilMs = nowMs + duplicateWindowMs
    }

    fun shouldSuppressExitBack(nowMs: Long): Boolean {
        return nowMs <= suppressExitBackUntilMs
    }

    private companion object {
        const val DEFAULT_DUPLICATE_WINDOW_MS = 1_500L
    }
}
