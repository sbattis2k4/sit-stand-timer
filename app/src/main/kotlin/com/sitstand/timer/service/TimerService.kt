package com.sitstand.timer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sitstand.timer.data.TimerRepository
import com.sitstand.timer.model.TimerMode
import com.sitstand.timer.model.TimerState
import com.sitstand.timer.widget.TimerWidgetProvider

class TimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val phaseRunnable = Runnable { handlePhaseComplete() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TimerRepository.init(this)
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
            ACTION_TOGGLE -> handleToggle()
            ACTION_SWITCH -> handleSwitch()
            ACTION_SET_MODE -> handleSetMode(intent)
            ACTION_PHASE_COMPLETE -> handlePhaseComplete()
            else -> {
                val state = TimerRepository.getState(this)
                if (state.isRunning) {
                    applyRunningState(state)
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun handleStart() {
        val state = TimerRepository.start(this)
        applyRunningState(state)
    }

    private fun handleToggle() {
        val current = TimerRepository.getState(this)
        if (current.isRunning) {
            handleStop()
        } else {
            handleStart()
        }
    }

    private fun handleSwitch() {
        val state = TimerRepository.switchMode(this)
        if (state.isRunning) {
            applyRunningState(state)
        } else {
            TimerWidgetProvider.updateAllWidgets(this)
            stopSelf()
        }
    }

    private fun handleSetMode(intent: Intent) {
        val modeName = intent.getStringExtra(EXTRA_MODE)
        val mode = runCatching { TimerMode.valueOf(modeName ?: "") }.getOrNull() ?: return
        val state = TimerRepository.setMode(this, mode)
        if (state.isRunning) {
            applyRunningState(state)
        } else {
            TimerWidgetProvider.updateAllWidgets(this)
            stopSelf()
        }
    }

    private fun handlePhaseComplete() {
        val current = TimerRepository.getState(this)
        if (!current.isRunning) return

        val now = System.currentTimeMillis()
        val newMode = TimerRepository.completePhase(this, now)
        val updated = TimerRepository.getState(this)
        val durationMinutes = if (newMode.name == "SIT") {
            updated.sitDurationMinutes
        } else {
            updated.standDurationMinutes
        }

        NotificationHelper.postSwitchAlert(
            context = this,
            newMode = newMode,
            durationMinutes = durationMinutes,
            autoStarted = updated.isRunning
        )

        if (updated.isRunning) {
            applyRunningState(updated)
        } else {
            cancelScheduledPhase()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            TimerWidgetProvider.updateAllWidgets(this)
            stopSelf()
        }
    }

    private fun handleStop() {
        cancelScheduledPhase()
        TimerRepository.stop(this)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        TimerWidgetProvider.updateAllWidgets(this)
        stopSelf()
    }

    private fun applyRunningState(state: TimerState) {
        val notification = NotificationHelper.buildOngoingNotification(this, state)
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NotificationHelper.ONGOING_NOTIFICATION_ID,
            notification,
            fgsType
        )
        schedulePhaseEnd(state.endTimeEpochMs)
        TimerWidgetProvider.updateAllWidgets(this)
    }

    private fun schedulePhaseEnd(endTimeEpochMs: Long) {
        cancelScheduledPhase()
        val delayMs = (endTimeEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
        handler.postDelayed(phaseRunnable, delayMs)

        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = alarmPendingIntent()
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                endTimeEpochMs,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                endTimeEpochMs,
                pendingIntent
            )
        }
    }

    private fun cancelScheduledPhase() {
        handler.removeCallbacks(phaseRunnable)
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(this, PhaseAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(phaseRunnable)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.sitstand.timer.action.START"
        const val ACTION_STOP = "com.sitstand.timer.action.STOP"
        const val ACTION_TOGGLE = "com.sitstand.timer.action.TOGGLE"
        const val ACTION_SWITCH = "com.sitstand.timer.action.SWITCH"
        const val ACTION_SET_MODE = "com.sitstand.timer.action.SET_MODE"
        const val ACTION_PHASE_COMPLETE = "com.sitstand.timer.action.PHASE_COMPLETE"
        const val EXTRA_MODE = "extra_mode"
        private const val ALARM_REQUEST_CODE = 2001

        fun start(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_START }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val state = TimerRepository.getState(context)
            if (!state.isRunning) {
                TimerRepository.stop(context)
                TimerWidgetProvider.updateAllWidgets(context)
                return
            }
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_STOP }
            try {
                context.startService(intent)
            } catch (_: IllegalStateException) {
                TimerRepository.stop(context)
                TimerWidgetProvider.updateAllWidgets(context)
            }
        }

        fun toggle(context: Context) {
            val state = TimerRepository.getState(context)
            if (state.isRunning) {
                stop(context)
            } else {
                start(context)
            }
        }

        fun switchMode(context: Context) {
            val state = TimerRepository.getState(context)
            if (state.isRunning) {
                val intent = Intent(context, TimerService::class.java).apply { action = ACTION_SWITCH }
                ContextCompat.startForegroundService(context, intent)
            } else {
                TimerRepository.switchMode(context)
                TimerWidgetProvider.updateAllWidgets(context)
            }
        }

        fun setMode(context: Context, mode: TimerMode) {
            val state = TimerRepository.getState(context)
            if (state.isRunning) {
                val intent = Intent(context, TimerService::class.java).apply {
                    action = ACTION_SET_MODE
                    putExtra(EXTRA_MODE, mode.name)
                }
                ContextCompat.startForegroundService(context, intent)
            } else {
                TimerRepository.setMode(context, mode)
                TimerWidgetProvider.updateAllWidgets(context)
            }
        }

        fun phaseComplete(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_PHASE_COMPLETE
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
