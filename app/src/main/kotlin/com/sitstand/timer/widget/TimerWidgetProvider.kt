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
import com.sitstand.timer.model.formatDuration
import com.sitstand.timer.service.TimerService

class TimerWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_TOGGLE -> TimerService.toggle(context)
            ACTION_WIDGET_SWITCH -> TimerService.switchMode(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.sitstand.timer.ACTION_WIDGET_TOGGLE"
        const val ACTION_WIDGET_SWITCH = "com.sitstand.timer.ACTION_WIDGET_SWITCH"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TimerWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
            TimerWideWidgetProvider.updateAllWideWidgets(context)
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val state = TimerRepository.getState(context)
            val now = System.currentTimeMillis()
            val remainingMs = state.remainingMs(now)

            val views = RemoteViews(context.packageName, R.layout.widget_timer)
            views.setTextViewText(R.id.widget_mode, "${state.mode.label} ⇄")

            if (state.isRunning) {
                views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                views.setViewVisibility(R.id.widget_static_time, View.GONE)
                val base = SystemClock.elapsedRealtime() + remainingMs
                views.setChronometer(R.id.widget_chronometer, base, null, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    views.setChronometerCountDown(R.id.widget_chronometer, true)
                }
                views.setTextViewText(R.id.widget_toggle_button, "Stop")
                views.setInt(R.id.widget_toggle_button, "setBackgroundResource", R.drawable.widget_button_stop_bg)
                views.setTextColor(R.id.widget_toggle_button, Color.parseColor("#FAF8F4"))
            } else {
                views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                views.setViewVisibility(R.id.widget_static_time, View.VISIBLE)
                views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime(), null, false)
                views.setTextViewText(R.id.widget_static_time, formatDuration(remainingMs))
                views.setTextViewText(R.id.widget_toggle_button, "Start")
                views.setInt(R.id.widget_toggle_button, "setBackgroundResource", R.drawable.widget_button_bg)
                views.setTextColor(R.id.widget_toggle_button, Color.parseColor("#222B34"))
            }

            val toggleIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_toggle_button, togglePendingIntent)

            val switchIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_SWITCH
            }
            val switchPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                switchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_mode, switchPendingIntent)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
