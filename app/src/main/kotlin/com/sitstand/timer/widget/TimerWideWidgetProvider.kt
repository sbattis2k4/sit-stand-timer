package com.sitstand.timer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.sitstand.timer.MainActivity
import com.sitstand.timer.R
import com.sitstand.timer.data.TimerRepository
import com.sitstand.timer.model.TimerMode
import com.sitstand.timer.model.formatDuration
import com.sitstand.timer.service.TimerService

class TimerWideWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_TOGGLE -> TimerService.toggle(context)
            ACTION_WIDGET_SET_SIT -> TimerService.setMode(context, TimerMode.SIT)
            ACTION_WIDGET_SET_STAND -> TimerService.setMode(context, TimerMode.STAND)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWideWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.sitstand.timer.ACTION_WIDE_WIDGET_TOGGLE"
        const val ACTION_WIDGET_SET_SIT = "com.sitstand.timer.ACTION_WIDE_WIDGET_SET_SIT"
        const val ACTION_WIDGET_SET_STAND = "com.sitstand.timer.ACTION_WIDE_WIDGET_SET_STAND"

        fun updateAllWideWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TimerWideWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            for (id in ids) {
                updateWideWidget(context, manager, id)
            }
        }

        private fun updateWideWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val state = TimerRepository.getState(context)
            val now = System.currentTimeMillis()
            val remainingMs = state.remainingMs(now)

            val views = RemoteViews(context.packageName, R.layout.widget_timer_wide)

            val isSit = state.mode == TimerMode.SIT
            views.setInt(
                R.id.widget_wide_sit_button,
                "setBackgroundResource",
                if (isSit) R.drawable.widget_segment_active_bg else R.drawable.widget_segment_inactive_bg
            )
            views.setTextColor(
                R.id.widget_wide_sit_button,
                Color.parseColor(if (isSit) "#222B34" else "#D9C6A5")
            )

            views.setInt(
                R.id.widget_wide_stand_button,
                "setBackgroundResource",
                if (!isSit) R.drawable.widget_segment_active_bg else R.drawable.widget_segment_inactive_bg
            )
            views.setTextColor(
                R.id.widget_wide_stand_button,
                Color.parseColor(if (!isSit) "#222B34" else "#D9C6A5")
            )

            if (state.isRunning) {
                views.setViewVisibility(R.id.widget_wide_chronometer, View.VISIBLE)
                views.setViewVisibility(R.id.widget_wide_static_time, View.GONE)
                val base = SystemClock.elapsedRealtime() + remainingMs
                views.setChronometer(R.id.widget_wide_chronometer, base, null, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    views.setChronometerCountDown(R.id.widget_wide_chronometer, true)
                }
                views.setTextViewText(R.id.widget_wide_toggle_button, "Stop")
                views.setInt(R.id.widget_wide_toggle_button, "setBackgroundResource", R.drawable.widget_button_stop_bg)
                views.setTextColor(R.id.widget_wide_toggle_button, Color.parseColor("#FAF8F4"))
            } else {
                views.setViewVisibility(R.id.widget_wide_chronometer, View.GONE)
                views.setViewVisibility(R.id.widget_wide_static_time, View.VISIBLE)
                views.setChronometer(R.id.widget_wide_chronometer, SystemClock.elapsedRealtime(), null, false)
                views.setTextViewText(R.id.widget_wide_static_time, formatDuration(remainingMs))
                views.setTextViewText(R.id.widget_wide_toggle_button, "Start")
                views.setInt(R.id.widget_wide_toggle_button, "setBackgroundResource", R.drawable.widget_button_bg)
                views.setTextColor(R.id.widget_wide_toggle_button, Color.parseColor("#222B34"))
            }

            views.setOnClickPendingIntent(
                R.id.widget_wide_toggle_button,
                broadcastPendingIntent(context, 10, ACTION_WIDGET_TOGGLE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_wide_sit_button,
                broadcastPendingIntent(context, 11, ACTION_WIDGET_SET_SIT)
            )
            views.setOnClickPendingIntent(
                R.id.widget_wide_stand_button,
                broadcastPendingIntent(context, 12, ACTION_WIDGET_SET_STAND)
            )

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                13,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_wide_root, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun broadcastPendingIntent(
            context: Context,
            requestCode: Int,
            action: String
        ): PendingIntent {
            val intent = Intent(context, TimerWideWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
