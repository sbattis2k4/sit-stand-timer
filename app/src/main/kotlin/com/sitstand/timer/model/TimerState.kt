package com.sitstand.timer.model

import java.util.Locale

enum class TimerMode(val label: String) {
    SIT("SIT"),
    STAND("STAND");

    fun next(): TimerMode = if (this == SIT) STAND else SIT
}

data class TimerState(
    val isRunning: Boolean = false,
    val mode: TimerMode = TimerMode.SIT,
    val sitDurationMinutes: Int = 45,
    val standDurationMinutes: Int = 15,
    val endTimeEpochMs: Long = 0L,
    val autoAdvance: Boolean = true
) {
    fun durationMsFor(targetMode: TimerMode): Long {
        val minutes = if (targetMode == TimerMode.SIT) sitDurationMinutes else standDurationMinutes
        return minutes * 60_000L
    }

    fun remainingMs(nowEpochMs: Long): Long {
        return if (isRunning) {
            (endTimeEpochMs - nowEpochMs).coerceAtLeast(0L)
        } else {
            durationMsFor(mode)
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = if (ms <= 0L) 0L else (ms + 999L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun clampMinutes(minutes: Int): Int = minutes.coerceIn(1, 240)
