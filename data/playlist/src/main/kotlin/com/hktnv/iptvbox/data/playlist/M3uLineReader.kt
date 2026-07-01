package com.hktnv.iptvbox.data.playlist

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private val TurkishFallbackCharsets = listOf(
    Charset.forName("windows-1254"),
    Charset.forName("ISO-8859-9"),
    Charsets.ISO_8859_1,
)

internal fun <T> InputStream.useM3uLines(
    primaryCharset: Charset,
    block: (Sequence<String>) -> T,
): T {
    return StreamingM3uLineReader(this, primaryCharset).use { reader ->
        block(generateSequence { reader.readLine() })
    }
}

private class StreamingM3uLineReader(
    input: InputStream,
    private val primaryCharset: Charset,
) : Closeable {
    private val source = input.buffered()
    private val lineBuffer = ByteArrayOutputStream(256)

    fun readLine(): String? {
        lineBuffer.reset()
        var readAny = false
        while (true) {
            val next = source.read()
            if (next == -1) break
            readAny = true
            when (next) {
                '\n'.code -> break
                '\r'.code -> Unit
                else -> lineBuffer.write(next)
            }
        }
        if (!readAny && lineBuffer.size() == 0) return null
        return decodeM3uLine(lineBuffer.toByteArray(), primaryCharset)
    }

    override fun close() {
        source.close()
    }
}

private fun decodeM3uLine(bytes: ByteArray, primaryCharset: Charset): String {
    if (bytes.all { it >= 0 }) return String(bytes, Charsets.US_ASCII)
    return decodeStrict(bytes, primaryCharset)
        ?: TurkishFallbackCharsets
            .asSequence()
            .filterNot { it.name().equals(primaryCharset.name(), ignoreCase = true) }
            .mapNotNull { decodeStrict(bytes, it) }
            .firstOrNull()
        ?: String(bytes, primaryCharset)
}

private fun decodeStrict(bytes: ByteArray, charset: Charset): String? {
    return try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }
}
