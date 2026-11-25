package com.sitstand.timer

import com.sitstand.timer.model.TimerMode
import com.sitstand.timer.model.TimerState
import com.sitstand.timer.model.clampMinutes
import com.sitstand.timer.model.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateTest {

    @Test
    fun timerModeNext_alternatesBetweenSitAndStand() {
        assertEquals(TimerMode.STAND, TimerMode.SIT.next())
        assertEquals(TimerMode.SIT, TimerMode.STAND.next())
    }

    @Test
    fun durationMsFor_returnsConfiguredMinutesInMillis() {
        val state = TimerState(sitDurationMinutes = 45, standDurationMinutes = 15)
        assertEquals(45 * 60_000L, state.durationMsFor(TimerMode.SIT))
        assertEquals(15 * 60_000L, state.durationMsFor(TimerMode.STAND))
    }

    @Test
    fun remainingMs_handlesRunningExpiredAndStoppedStates() {
        val stopped = TimerState(isRunning = false, mode = TimerMode.SIT, sitDurationMinutes = 30)
        assertEquals(30 * 60_000L, stopped.remainingMs(nowEpochMs = 10_000L))

        val running = TimerState(isRunning = true, endTimeEpochMs = 100_000L)
        assertEquals(40_000L, running.remainingMs(nowEpochMs = 60_000L))
        assertEquals(0L, running.remainingMs(nowEpochMs = 120_000L))
    }

    @Test
    fun formatDuration_formatsSecondsMinutesAndHours() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:45", formatDuration(45_000L))
        assertEquals("01:05", formatDuration(65_000L))
        assertEquals("45:00", formatDuration(45 * 60_000L))
        assertEquals("1:01:01", formatDuration(3_661_000L))
    }

    @Test
    fun clampMinutes_restrictsToValidRange() {
        assertEquals(1, clampMinutes(0))
        assertEquals(1, clampMinutes(-10))
        assertEquals(45, clampMinutes(45))
        assertEquals(240, clampMinutes(300))
    }
}
