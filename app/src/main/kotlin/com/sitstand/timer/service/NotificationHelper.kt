package com.sitstand.timer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sitstand.timer.MainActivity
import com.sitstand.timer.R
import com.sitstand.timer.model.TimerMode
import com.sitstand.timer.model.TimerState
import java.text.DateFormat
import java.util.Date

object NotificationHelper {
    const val CHANNEL_ONGOING = "sit_stand_ongoing"
    const val CHANNEL_ALERTS = "sit_stand_alerts"
    const val ONGOING_NOTIFICATION_ID = 1001
    const val ALERT_NOTIFICATION_ID = 1002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val ongoingChannel = NotificationChannel(
            CHANNEL_ONGOING,
            "Active Timer (Lock Screen)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows the current sit/stand mode and live countdown on your lock screen"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Switch Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when it is time to sit or stand"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(ongoingChannel)
        manager.createNotificationChannel(alertsChannel)
    }

    fun buildOngoingNotification(context: Context, state: TimerState): Notification {
        ensureChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val switchIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_SWITCH
        }
        val switchPendingIntent = PendingIntent.getService(
            context,
            1,
            switchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeTitle = if (state.mode == TimerMode.STAND) "Mode: STAND" else "Mode: SIT"
        val nextModeName = if (state.mode.next() == TimerMode.STAND) "Stand" else "Sit"
        val endTimeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.endTimeEpochMs))

        return NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(modeTitle)
            .setContentText("Switch to $nextModeName at $endTimeText")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(state.endTimeEpochMs)
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Switch Now", switchPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    fun postSwitchAlert(
        context: Context,
        newMode: TimerMode,
        durationMinutes: Int,
        autoStarted: Boolean
    ) {
        ensureChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (newMode == TimerMode.STAND) "Stand up!" else "Have a seat!"
        val modeVerb = if (newMode == TimerMode.STAND) "Standing" else "Sitting"
        val body = if (autoStarted) {
            "$modeVerb for $durationMinutes min"
        } else {
            "Tap to start your $durationMinutes min ${newMode.label.lowercase()} interval"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify
        }
    }
}
