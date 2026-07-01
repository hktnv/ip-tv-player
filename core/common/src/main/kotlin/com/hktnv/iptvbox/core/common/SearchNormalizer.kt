package com.hktnv.iptvbox.core.common

import java.text.Normalizer
import java.util.Locale

object SearchNormalizer {
    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        return NormalizedBuilder(input.length)
            .append(input)
            .build()
    }

    fun normalizeParts(
        first: String?,
        second: String? = null,
        third: String? = null,
        fourth: String? = null,
        fifth: String? = null,
        sixth: String? = null,
    ): String {
        val capacity = (first?.length ?: 0) +
            (second?.length ?: 0) +
            (third?.length ?: 0) +
            (fourth?.length ?: 0) +
            (fifth?.length ?: 0) +
            (sixth?.length ?: 0)
        return NormalizedBuilder(capacity)
            .appendPart(first)
            .appendPart(second)
            .appendPart(third)
            .appendPart(fourth)
            .appendPart(fifth)
            .appendPart(sixth)
            .build()
    }

    private class NormalizedBuilder(capacity: Int) {
        private val output = StringBuilder(capacity)
        private var pendingSpace = false

        fun appendPart(value: String?): NormalizedBuilder {
            if (value.isNullOrBlank()) return this
            if (output.isNotEmpty()) pendingSpace = true
            return append(value)
        }

        fun append(value: String): NormalizedBuilder {
            value.forEach(::appendChar)
            return this
        }

        fun build(): String = output.toString()

        private fun appendChar(char: Char) {
            when {
                char in 'a'..'z' || char in '0'..'9' -> appendSearchChar(char)
                char in 'A'..'Z' -> appendSearchChar((char.code + ASCII_CASE_OFFSET).toChar())
                char.isFastSeparator() -> appendSeparator()
                else -> appendMappedChar(char)
            }
        }

        private fun appendMappedChar(char: Char) {
            when (char) {
                '\u0130', '\u0131', '\u00EC', '\u00ED', '\u00EE', '\u00EF', '\u012B' -> appendSearchChar('i')
                '\u00C7', '\u00E7', '\u0106', '\u0107', '\u010C', '\u010D' -> appendSearchChar('c')
                '\u011E', '\u011F' -> appendSearchChar('g')
                '\u00D6', '\u00F6', '\u00D2', '\u00F2', '\u00D3', '\u00F3',
                '\u00D4', '\u00F4', '\u00D5', '\u00F5', '\u00D8', '\u00F8', '\u014D', '\u014F' -> appendSearchChar('o')
                '\u015E', '\u015F' -> appendSearchChar('s')
                '\u00DC', '\u00FC', '\u00D9', '\u00F9', '\u00DA', '\u00FA',
                '\u00DB', '\u00FB', '\u016B' -> appendSearchChar('u')
                '\u00C0', '\u00E0', '\u00C1', '\u00E1', '\u00C2', '\u00E2',
                '\u00C3', '\u00E3', '\u00C4', '\u00E4', '\u00C5', '\u00E5',
                '\u0101', '\u0103', '\u0105' -> appendSearchChar('a')
                '\u00C8', '\u00E8', '\u00C9', '\u00E9', '\u00CA', '\u00EA',
                '\u00CB', '\u00EB', '\u0113', '\u0117', '\u0119' -> appendSearchChar('e')
                '\u00D1', '\u00F1' -> appendSearchChar('n')
                '\u00DD', '\u00FD', '\u0178', '\u00FF' -> appendSearchChar('y')
                '\u017D', '\u017E', '\u0179', '\u017A', '\u017B', '\u017C' -> appendSearchChar('z')
                '\u0141', '\u0142' -> appendSearchChar('l')
                '\u00DF' -> appendSearchText("ss")
                else -> appendDecomposed(char)
            }
        }

        private fun appendDecomposed(char: Char) {
            val decomposed = Normalizer.normalize(
                char.toString().lowercase(ROOT_LOCALE),
                Normalizer.Form.NFD,
            )
            var appended = false
            decomposed.forEach { mapped ->
                if (Character.getType(mapped) == Character.NON_SPACING_MARK.toInt()) return@forEach
                when {
                    mapped in 'a'..'z' || mapped in '0'..'9' -> {
                        appendSearchChar(mapped)
                        appended = true
                    }
                    mapped in 'A'..'Z' -> {
                        appendSearchChar((mapped.code + ASCII_CASE_OFFSET).toChar())
                        appended = true
                    }
                }
            }
            if (!appended) appendSeparator()
        }

        private fun appendSearchText(value: String) {
            value.forEach(::appendSearchChar)
        }

        private fun appendSearchChar(char: Char) {
            if (pendingSpace && output.isNotEmpty()) {
                output.append(' ')
            }
            pendingSpace = false
            output.append(char)
        }

        private fun appendSeparator() {
            if (output.isNotEmpty()) pendingSpace = true
        }
    }

    private fun Char.isFastSeparator(): Boolean {
        return this <= ' ' ||
            this == '/' ||
            this == '\\' ||
            this == '-' ||
            this == '_' ||
            this == '.' ||
            this == ':' ||
            this == '|' ||
            this == ',' ||
            this == ';' ||
            this == '(' ||
            this == ')' ||
            this == '[' ||
            this == ']' ||
            this == '{' ||
            this == '}'
    }

    private const val ASCII_CASE_OFFSET = 32
    private val ROOT_LOCALE = Locale.ROOT
}
