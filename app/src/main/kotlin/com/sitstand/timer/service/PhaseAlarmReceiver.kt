package com.sitstand.timer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sitstand.timer.data.TimerRepository

class PhaseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val state = TimerRepository.getState(context)
        val now = System.currentTimeMillis()
        if (state.isRunning && now >= state.endTimeEpochMs - 1000L) {
            TimerService.phaseComplete(context)
        }
    }
}
