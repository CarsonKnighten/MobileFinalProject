package com.example.mobilefinalproject.strength

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mobilefinalproject.MainActivity
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.shell.AppSession
import com.example.mobilefinalproject.shell.testAccount
import com.example.mobilefinalproject.strength.timer.RestTimerService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestNotificationTest {
    @Test
    fun postsRestCompleteWhileTheAppIsInTheBackground() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        AppSession.signIn(testAccount)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { activity ->
                activity.onActivity { RestTimerService.schedule(it, SystemClock.elapsedRealtime() + 2500) }
                awaitNotification(manager, RestTimerService.RUNNING_NOTIFICATION)
                instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
                val finished = awaitNotification(manager, RestTimerService.FINISHED_NOTIFICATION)
                assertEquals(context.getString(R.string.rest_finished_title),
                    finished.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
            }
        } finally {
            RestTimerService.cancel(context)
        }
    }

    private fun awaitNotification(manager: NotificationManager, id: Int): Notification {
        val timeout = SystemClock.elapsedRealtime() + 7000
        while (SystemClock.elapsedRealtime() < timeout) {
            manager.activeNotifications.firstOrNull { it.id == id }?.let { return it.notification }
            SystemClock.sleep(50)
        }
        error("Notification $id did not appear.")
    }
}
