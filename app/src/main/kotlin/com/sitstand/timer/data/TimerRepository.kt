package com.sitstand.timer.data

import android.content.Context
import android.content.SharedPreferences
import com.sitstand.timer.model.TimerMode
import com.sitstand.timer.model.TimerState
import com.sitstand.timer.model.clampMinutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerRepository {
    private const val PREFS_NAME = "sit_stand_timer_prefs"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_MODE = "mode"
    private const val KEY_SIT_MINUTES = "sit_minutes"
    private const val KEY_STAND_MINUTES = "stand_minutes"
    private const val KEY_END_TIME_MS = "end_time_epoch_ms"
    private const val KEY_AUTO_ADVANCE = "auto_advance"

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val p = prefs(context)
        val modeName = p.getString(KEY_MODE, TimerMode.SIT.name)
        val mode = runCatching { TimerMode.valueOf(modeName ?: TimerMode.SIT.name) }
            .getOrDefault(TimerMode.SIT)
        _state.value = TimerState(
            isRunning = p.getBoolean(KEY_IS_RUNNING, false),
            mode = mode,
            sitDurationMinutes = clampMinutes(p.getInt(KEY_SIT_MINUTES, 45)),
            standDurationMinutes = clampMinutes(p.getInt(KEY_STAND_MINUTES, 15)),
            endTimeEpochMs = p.getLong(KEY_END_TIME_MS, 0L),
            autoAdvance = p.getBoolean(KEY_AUTO_ADVANCE, true)
        )
        initialized = true
    }

    fun getState(context: Context): TimerState {
        init(context)
        return _state.value
    }

    @Synchronized
    private fun save(context: Context, newState: TimerState): TimerState {
        prefs(context).edit()
            .putBoolean(KEY_IS_RUNNING, newState.isRunning)
            .putString(KEY_MODE, newState.mode.name)
            .putInt(KEY_SIT_MINUTES, newState.sitDurationMinutes)
            .putInt(KEY_STAND_MINUTES, newState.standDurationMinutes)
            .putLong(KEY_END_TIME_MS, newState.endTimeEpochMs)
            .putBoolean(KEY_AUTO_ADVANCE, newState.autoAdvance)
            .apply()
        _state.value = newState
        return newState
    }

    fun updateSitDuration(context: Context, minutes: Int): TimerState {
        val current = getState(context)
        return save(context, current.copy(sitDurationMinutes = clampMinutes(minutes)))
    }

    fun updateStandDuration(context: Context, minutes: Int): TimerState {
        val current = getState(context)
        return save(context, current.copy(standDurationMinutes = clampMinutes(minutes)))
    }

    fun setAutoAdvance(context: Context, enabled: Boolean): TimerState {
        val current = getState(context)
        return save(context, current.copy(autoAdvance = enabled))
    }

    fun start(context: Context, nowMs: Long = System.currentTimeMillis()): TimerState {
        val current = getState(context)
        val endTime = nowMs + current.durationMsFor(current.mode)
        return save(context, current.copy(isRunning = true, endTimeEpochMs = endTime))
    }

    fun stop(context: Context): TimerState {
        val current = getState(context)
        return save(context, current.copy(isRunning = false, endTimeEpochMs = 0L))
    }

    fun switchMode(context: Context, nowMs: Long = System.currentTimeMillis()): TimerState {
        val current = getState(context)
        val nextMode = current.mode.next()
        val endTime = if (current.isRunning) nowMs + current.durationMsFor(nextMode) else 0L
        return save(context, current.copy(mode = nextMode, endTimeEpochMs = endTime))
    }

    fun setMode(
        context: Context,
        mode: TimerMode,
        nowMs: Long = System.currentTimeMillis()
    ): TimerState {
        val current = getState(context)
        if (current.mode == mode) return current
        val endTime = if (current.isRunning) nowMs + current.durationMsFor(mode) else 0L
        return save(context, current.copy(mode = mode, endTimeEpochMs = endTime))
    }

    fun completePhase(context: Context, nowMs: Long = System.currentTimeMillis()): TimerMode {
        val current = getState(context)
        val nextMode = current.mode.next()
        val nextState = if (current.autoAdvance) {
            current.copy(
                isRunning = true,
                mode = nextMode,
                endTimeEpochMs = nowMs + current.durationMsFor(nextMode)
            )
        } else {
            current.copy(
                isRunning = false,
                mode = nextMode,
                endTimeEpochMs = 0L
            )
        }
        save(context, nextState)
        return nextMode
    }
}
