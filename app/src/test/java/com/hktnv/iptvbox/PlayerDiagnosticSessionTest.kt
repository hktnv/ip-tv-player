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

    @Test
    fun droppedFramesAreAggregatedUntilWindowPasses() {
        val session = PlayerDiagnosticSession()

        assertNull(session.onDroppedVideoFrames(droppedFrames = 4, elapsedMs = 800L, nowMs = 1_000L))
        assertNull(session.onDroppedVideoFrames(droppedFrames = 5, elapsedMs = 900L, nowMs = 3_000L))
        val event = session.onDroppedVideoFrames(droppedFrames = 2, elapsedMs = 600L, nowMs = 6_100L)

        assertEquals(11, event?.count)
        assertEquals(2_300L, event?.elapsedMs)
        assertEquals(5_100L, event?.windowMs)
    }

    @Test
    fun droppedFramesCanBeFlushedOnDetach() {
        val session = PlayerDiagnosticSession()

        session.onDroppedVideoFrames(droppedFrames = 3, elapsedMs = 500L, nowMs = 1_000L)
        val event = session.flushDroppedVideoFrames(nowMs = 2_000L)

        assertEquals(3, event?.count)
        assertEquals(500L, event?.elapsedMs)
        assertEquals(1_000L, event?.windowMs)
        assertNull(session.flushDroppedVideoFrames(nowMs = 3_000L))
    }

    @Test
    fun severeDroppedFrameBurstLogsBeforeWindowPasses() {
        val session = PlayerDiagnosticSession()

        val event = session.onDroppedVideoFrames(droppedFrames = 24, elapsedMs = 1_100L, nowMs = 1_000L)

        assertEquals(24, event?.count)
        assertEquals(1_100L, event?.elapsedMs)
        assertEquals(0L, event?.windowMs)
    }
}
