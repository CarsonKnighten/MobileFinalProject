package com.example.mobilefinalproject.strength.timer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.mobilefinalproject.MainActivity
import com.example.mobilefinalproject.R

class RestTimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deadline = intent?.getLongExtra(EXTRA_DEADLINE, 0L) ?: 0L
        if (deadline <= 0L) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannels(listOf(
            NotificationChannel(RUNNING_CHANNEL, getString(R.string.rest_running_channel), NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(FINISHED_CHANNEL, getString(R.string.rest_finished_channel), NotificationManager.IMPORTANCE_HIGH)
        ))
        val running = notification(RUNNING_CHANNEL, R.string.rest_running_title)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
        val serviceType = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else 0
        ServiceCompat.startForeground(this, RUNNING_NOTIFICATION, running, serviceType)

        releaseTimer()
        val remaining = (deadline - SystemClock.elapsedRealtime()).coerceIn(0L, MAX_REST_MILLIS)
        if (remaining > 0) holdAwake(remaining)
        handler.postDelayed({ complete(startId, notifications) }, remaining)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseTimer()
        super.onDestroy()
    }

    private fun notification(channel: String, title: Int): NotificationCompat.Builder {
        val workout = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openWorkout = PendingIntent.getActivity(this, 0, workout, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(title))
            .setContentIntent(openWorkout)
    }

    @SuppressLint("MissingPermission") // areNotificationsEnabled covers the runtime permission and app settings.
    private fun complete(startId: Int, notifications: NotificationManager) {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            if (notifications.areNotificationsEnabled()) {
                notifications.notify(FINISHED_NOTIFICATION, notification(FINISHED_CHANNEL, R.string.rest_finished_title)
                    .setContentText(getString(R.string.rest_finished_text))
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .build())
            }
        } finally {
            stopSelf(startId)
        }
    }

    private fun holdAwake(remaining: Long) {
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:rest-timer")
            .apply { acquire(remaining + 2000L) }
    }

    private fun releaseTimer() {
        handler.removeCallbacksAndMessages(null)
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    internal companion object {
        private const val EXTRA_DEADLINE = "deadline"
        private const val MAX_REST_MILLIS = 90_000L
        private const val RUNNING_CHANNEL = "strength_rest_running"
        private const val FINISHED_CHANNEL = "strength_rest_finished"
        const val RUNNING_NOTIFICATION = 721
        const val FINISHED_NOTIFICATION = 722

        fun schedule(context: Context, deadline: Long) {
            val notifications = context.getSystemService(NotificationManager::class.java)
            if (!notifications.areNotificationsEnabled()) return
            notifications.cancel(FINISHED_NOTIFICATION)
            val intent = Intent(context, RestTimerService::class.java).putExtra(EXTRA_DEADLINE, deadline)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (error: IllegalStateException) {
                Log.w("RestTimer", "Unable to start the rest notification while the app is in the background.", error)
            } catch (error: SecurityException) {
                Log.w("RestTimer", "Unable to start the rest notification.", error)
            }
        }

        fun cancel(context: Context) {
            context.stopService(Intent(context, RestTimerService::class.java))
            context.getSystemService(NotificationManager::class.java).cancel(FINISHED_NOTIFICATION)
        }
    }
}
