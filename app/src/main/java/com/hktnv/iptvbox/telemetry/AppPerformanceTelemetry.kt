package com.hktnv.iptvbox.telemetry
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.hktnv.iptvbox.core.security.SecretRedactor
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.system.exitProcess
import com.hktnv.iptvbox.ui.media.label

internal class AppPerformanceTelemetry(context: Context) {
    private val appContext = context.applicationContext
    private val startedAtMs = SystemClock.elapsedRealtime()
    private val reportFile = File(appContext.filesDir, "performance_runtime_latest.md")
    private val prefs = appContext.getSharedPreferences("performance_diagnostics", Context.MODE_PRIVATE)
    private val values = ConcurrentHashMap<String, String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val watchIds = AtomicLong()
    private val watches = ConcurrentHashMap<Long, UiWatch>()
    private val _diagnostics = MutableStateFlow(buildDiagnostics())

    val diagnostics: StateFlow<PerformanceDiagnostics> = _diagnostics.asStateFlow()

    init {
        installCrashHandler()
        values["ram_mb"] = usedRamMb().toString()
        publishSnapshot()
        writeReport()
    }

    fun sinceAppStartMs(): Long = SystemClock.elapsedRealtime() - startedAtMs

    fun mark(name: String) {
        record("${name}_ms", sinceAppStartMs())
    }

    fun record(name: String, value: Long) {
        values[name] = value.toString()
        values["ram_mb"] = usedRamMb().toString()
        emitMetric(name, value.toString())
        publishSnapshot()
        writeReport()
    }

    fun recordText(name: String, value: String) {
        val safeValue = SecretRedactor.redact(value)
        values[name] = safeValue
        values["ram_mb"] = usedRamMb().toString()
        emitMetric(name, safeValue)
        publishSnapshot()
        writeReport()
    }

    fun recordMany(numbers: Map<String, Long>, texts: Map<String, String> = emptyMap()) {
        numbers.forEach { (key, value) -> values[key] = value.toString() }
        texts.forEach { (key, value) -> values[key] = SecretRedactor.redact(value) }
        values["ram_mb"] = usedRamMb().toString()
        emitMetrics(numbers, texts.mapValues { SecretRedactor.redact(it.value) })
        publishSnapshot()
        writeReport()
    }

    fun recordDuration(name: String, startedAtMs: Long) {
        record(name, SystemClock.elapsedRealtime() - startedAtMs)
    }

    fun recordError(label: String, throwable: Throwable? = null, message: String? = null) {
        saveLastError(
            label = label,
            threadName = Thread.currentThread().name,
            throwable = throwable,
            fallbackMessage = message,
        )
    }

    fun beginUiWatch(name: String): Long {
        val id = watchIds.incrementAndGet()
        val watch = UiWatch(name = name, lastTickMs = SystemClock.elapsedRealtime())
        watches[id] = watch
        tickUiWatch(id)
        return id
    }

    fun endUiWatch(id: Long?) {
        if (id == null) return
        val watch = watches.remove(id) ?: return
        recordText("${watch.name}_ui_locked", if (watch.maxGapMs > 700) "yes" else "no")
        record("${watch.name}_max_main_thread_gap_ms", watch.maxGapMs)
    }

    private fun tickUiWatch(id: Long) {
        val watch = watches[id] ?: return
        mainHandler.postDelayed(
            {
                val now = SystemClock.elapsedRealtime()
                val gap = now - watch.lastTickMs
                watch.lastTickMs = now
                if (gap > watch.maxGapMs) watch.maxGapMs = gap
                if (watches.containsKey(id)) tickUiWatch(id)
            },
            250L,
        )
    }

    private fun installCrashHandler() {
        synchronized(AppPerformanceTelemetry::class.java) {
            if (crashHandlerInstalled) return
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                saveLastError("Çökme", thread.name, throwable, null)
                if (previous != null) {
                    previous.uncaughtException(thread, throwable)
                } else {
                    exitProcess(10)
                }
            }
            crashHandlerInstalled = true
        }
    }

    private fun saveLastError(
        label: String,
        threadName: String,
        throwable: Throwable?,
        fallbackMessage: String?,
    ) {
        val timeText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date())
        val message = throwable?.message?.takeIf { it.isNotBlank() }
            ?: fallbackMessage?.takeIf { it.isNotBlank() }
            ?: throwable?.javaClass?.simpleName
            ?: "Bilinmeyen hata"
        val safeMessage = SecretRedactor.redact(message)
        val topFrame = throwable?.stackTrace?.firstOrNull()?.let {
            "${it.className}.${it.methodName}:${it.lineNumber}"
        }
        val detail = buildString {
            append(label)
            append(" • ")
            append(timeText)
            append(" • ")
            append(safeMessage)
            append(" • ")
            append(threadName)
            if (topFrame != null) {
                append(" • ")
                append(topFrame)
            }
        }
        prefs.edit()
            .putString("last_error", detail)
            .putLong("last_error_time_ms", System.currentTimeMillis())
            .commit()
        values["last_error"] = detail
        values["ram_mb"] = usedRamMb().toString()
        Log.e(TAG, "error=$detail")
        publishSnapshot()
        writeReport()
    }

    private fun publishSnapshot() {
        _diagnostics.value = buildDiagnostics()
    }

    private fun buildDiagnostics(): PerformanceDiagnostics {
        return PerformanceDiagnostics(
            values = values.toMap(),
            lastError = prefs.getString("last_error", null),
            lastErrorTimeMs = prefs.getLong("last_error_time_ms", 0L).takeIf { it > 0L },
        )
    }

    private fun usedRamMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)
    }

    private fun writeReport() {
        runCatching {
            val lines = buildList {
                add("# Çalışma Zamanı Performans Ölçümleri")
                add("")
                add("Uygulama içinde üretildi: ${System.currentTimeMillis()}.")
                add("")
                values.toSortedMap().forEach { (key, value) ->
                    add("- $key: $value")
                }
            }
            reportFile.writeText(lines.joinToString("\n"))
        }
    }

    private fun emitMetric(name: String, value: String) {
        Log.i(TAG, "$name=$value")
    }

    private fun emitMetrics(numbers: Map<String, Long>, texts: Map<String, String>) {
        if (numbers.isEmpty() && texts.isEmpty()) return
        val numericText = numbers.toSortedMap().entries.joinToString(" ") { (key, value) -> "$key=$value" }
        val textValues = texts.toSortedMap().entries.joinToString(" ") { (key, value) -> "$key=$value" }
        Log.i(TAG, listOf(numericText, textValues).filter { it.isNotBlank() }.joinToString(" "))
    }

    private companion object {
        private const val TAG = "IptvPerf"
        var crashHandlerInstalled = false
    }
}

private data class UiWatch(
    val name: String,
    var lastTickMs: Long,
    var maxGapMs: Long = 0L,
)

internal data class PerformanceDiagnostics(
    val values: Map<String, String>,
    val lastError: String?,
    val lastErrorTimeMs: Long?,
)
