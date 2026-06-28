package com.hktnv.iptvbox.core.common

import java.text.Normalizer
import java.util.Locale

object SearchNormalizer {
    private val marksRegex = Regex("\\p{Mn}+")
    private val nonSearchCharRegex = Regex("[^a-z0-9]+")
    private val whitespaceRegex = Regex("\\s+")
    private val turkishLocale = Locale("tr", "TR")

    fun normalize(input: String): String {
        val turkishSafe = input
            .replace('I', 'i')
            .replace('İ', 'i')
            .replace('ı', 'i')
            .lowercase(turkishLocale)

        val withoutMarks = Normalizer
            .normalize(turkishSafe, Normalizer.Form.NFD)
            .replace(marksRegex, "")

        return withoutMarks
            .replace(nonSearchCharRegex, " ")
            .trim()
            .replace(whitespaceRegex, " ")
    }
}
