package com.hktnv.iptvbox.player

internal enum class PlayerMediaSwitchPlan {
    SetQueue,
    ResetQueueAtTarget,
    ReuseCurrent,
}

internal fun choosePlayerMediaSwitchPlan(
    activeQueueSignature: String?,
    playerMediaItemCount: Int,
    currentMediaItemIndex: Int,
    currentMediaId: String?,
    queue: PlayerMediaQueue,
): PlayerMediaSwitchPlan {
    return when {
        activeQueueSignature != queue.signature ||
            playerMediaItemCount != queue.mediaItems.size -> PlayerMediaSwitchPlan.SetQueue
        currentMediaItemIndex != queue.currentIndex ||
            currentMediaId != queue.mediaItems.getOrNull(queue.currentIndex)?.mediaId -> {
            PlayerMediaSwitchPlan.ResetQueueAtTarget
        }
        else -> PlayerMediaSwitchPlan.ReuseCurrent
    }
}
