package com.hktnv.iptvbox

import androidx.media3.common.Player
import com.hktnv.iptvbox.player.PlayerDiagnosticSession
import com.hktnv.iptvbox.player.diagnosticName
import com.hktnv.iptvbox.player.toDiagnosticMediaHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerDiagnosticSessionTest {
    @Test
    fun bufferingDurationsAreCountedUntilReady() {
        val session = PlayerDiagnosticSession()

        assertNull(session.onPlaybackStateChanged(Player.STATE_BUFFERING, nowMs = 1_000L))
        val first = session.onPlaybackStateChanged(Player.STATE_READY, nowMs = 2_250L)
        assertEquals(1_250L, first?.durationMs)
        assertEquals(1, first?.count)
        assertEquals(1_250L, first?.totalDurationMs)

        assertNull(session.onPlaybackStateChanged(Player.STATE_BUFFERING, nowMs = 3_000L))
        val second = session.onPlaybackStateChanged(Player.STATE_IDLE, nowMs = 3_400L)
        assertEquals(400L, second?.durationMs)
        assertEquals(2, second?.count)
        assertEquals(1_650L, second?.totalDurationMs)
    }

    @Test
    fun duplicateBufferingStateDoesNotResetStartTime() {
        val session = PlayerDiagnosticSession()

        session.onPlaybackStateChanged(Player.STATE_BUFFERING, nowMs = 1_000L)
        session.onPlaybackStateChanged(Player.STATE_BUFFERING, nowMs = 1_500L)
        val event = session.onPlaybackStateChanged(Player.STATE_READY, nowMs = 2_000L)

        assertEquals(1_000L, event?.durationMs)
    }

    @Test
    fun mediaHintRedactsLongCredentialLikePathSegments() {
        val hint = "http://example.test/live/user1234567890/password9876543210/123.ts"
            .toDiagnosticMediaHint()

        assertEquals("http://example.test/.../123.ts", hint)
    }

    @Test
    fun playbackStateNamesAreReadable() {
        assertEquals("IDLE", Player.STATE_IDLE.diagnosticName())
        assertEquals("BUFFERING", Player.STATE_BUFFERING.diagnosticName())
        assertEquals("READY", Player.STATE_READY.diagnosticName())
        assertEquals("ENDED", Player.STATE_ENDED.diagnosticName())
    }
}
