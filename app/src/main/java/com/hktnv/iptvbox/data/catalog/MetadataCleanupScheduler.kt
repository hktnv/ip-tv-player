package com.hktnv.iptvbox.data.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal class MetadataCleanupScheduler(
    context: Context,
    private val catalogStore: CatalogStore,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "metadata_cleanup",
        Context.MODE_PRIVATE,
    )

    suspend fun runAfterStartupDelay() {
        delay(MetadataCleanupPolicy.StartupDelayMs)
        val now = nowMs()
        val lastRun = lastRunAtMs()
        if (!MetadataCleanupPolicy.shouldRun(now, lastRun)) return
        withContext(Dispatchers.IO) {
            runCatching {
                catalogStore.pruneOrphanMetadata(limit = MetadataCleanupPolicy.BatchLimit)
                saveLastRunAtMs(nowMs())
            }
        }
    }

    private fun lastRunAtMs(): Long? {
        val value = preferences.getLong(LastRunKey, 0L)
        return value.takeIf { it > 0L }
    }

    private fun saveLastRunAtMs(value: Long) {
        preferences.edit().putLong(LastRunKey, value).commit()
    }

    private companion object {
        const val LastRunKey = "last_run_at_ms"
    }
}

internal object MetadataCleanupPolicy {
    const val StartupDelayMs = 10_000L
    const val WeeklyIntervalMs = 7L * 24L * 60L * 60L * 1000L
    const val BatchLimit = 50

    fun shouldRun(nowMs: Long, lastRunAtMs: Long?): Boolean {
        val lastRun = lastRunAtMs ?: return true
        return nowMs - lastRun >= WeeklyIntervalMs
    }
}
