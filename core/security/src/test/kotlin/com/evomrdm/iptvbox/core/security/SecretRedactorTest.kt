package com.evomrdm.iptvbox.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SecretRedactorTest {
    @Test
    fun redactsQuerySecretsAndUserInfo() {
        val redacted = SecretRedactor.redact(
            "https://alice:secret@example.com/player?username=alice&password=secret&token=abc",
        )

        assertEquals(
            "https://***:***@example.com/player?username=***&password=***&token=***",
            redacted,
        )
        assertFalse(redacted.contains("secret"))
        assertFalse(redacted.contains("alice"))
    }

    @Test
    fun redactsSensitiveHeaders() {
        assertEquals(
            mapOf("Authorization" to "***", "User-Agent" to "IPTV"),
            SecretRedactor.redactHeaders(mapOf("Authorization" to "Bearer abc", "User-Agent" to "IPTV")),
        )
    }
}
