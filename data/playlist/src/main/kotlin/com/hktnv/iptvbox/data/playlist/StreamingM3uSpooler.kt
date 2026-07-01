package com.hktnv.iptvbox.data.playlist

import java.io.File
import java.io.InputStream

class StreamingM3uSpooler(
    private val progressStep: Int = DEFAULT_PROGRESS_STEP,
) {
    internal fun spool(
        input: InputStream,
        onItemCounted: (Int) -> Unit = {},
    ): SpooledM3uFile {
        val file = File.createTempFile("iptv-m3u-", ".m3u")
        var itemCount = 0
        var lastReportedCount = 0
        try {
            input.bufferedReader(Charsets.UTF_8).use { reader ->
                file.bufferedWriter(Charsets.UTF_8).use { writer ->
                    var line = reader.readLine()
                    while (line != null) {
                        writer.appendLine(line)
                        if (line.trimStart().startsWith("#EXTINF", ignoreCase = true)) {
                            itemCount += 1
                            if (shouldReport(itemCount)) {
                                onItemCounted(itemCount)
                                lastReportedCount = itemCount
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
            if (itemCount > 0 && lastReportedCount != itemCount) {
                onItemCounted(itemCount)
            }
            return SpooledM3uFile(file = file, itemCount = itemCount)
        } catch (throwable: Throwable) {
            file.delete()
            throw throwable
        }
    }

    private fun shouldReport(count: Int): Boolean {
        return count == 1 || count % progressStep == 0
    }

    private companion object {
        const val DEFAULT_PROGRESS_STEP = 100
    }
}

internal data class SpooledM3uFile(
    val file: File,
    val itemCount: Int,
)
