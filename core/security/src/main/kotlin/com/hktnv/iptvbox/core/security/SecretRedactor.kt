package com.hktnv.iptvbox.core.security

object SecretRedactor {
    private val querySecret = Regex(
        pattern = "([?&](?:password|pass|pwd|token|access_token|refresh_token|api_key|apikey|key|auth|username|user)=)[^&#\\s]+",
        option = RegexOption.IGNORE_CASE,
    )
    private val headerSecret = Regex(
        pattern = "((?:authorization|cookie|set-cookie|x-api-key|x-auth-token)\\s*[:=]\\s*)[^\\r\\n]+",
        option = RegexOption.IGNORE_CASE,
    )
    private val userInfo = Regex("(https?://)([^:/@\\s]+):([^@/\\s]+)@")

    fun redact(value: String): String {
        return value
            .replace(userInfo) { match ->
                "${match.groupValues[1]}***:***@"
            }
            .replace(querySecret) { match ->
                "${match.groupValues[1]}***"
            }
            .replace(headerSecret) { match ->
                "${match.groupValues[1]}***"
            }
    }

    fun redactHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (key, value) ->
            if (isSensitiveHeader(key)) "***" else redact(value)
        }
    }

    private fun isSensitiveHeader(key: String): Boolean {
        return key.equals("Authorization", ignoreCase = true) ||
            key.equals("Cookie", ignoreCase = true) ||
            key.equals("Set-Cookie", ignoreCase = true) ||
            key.equals("X-Api-Key", ignoreCase = true) ||
            key.equals("X-Auth-Token", ignoreCase = true)
    }
}
