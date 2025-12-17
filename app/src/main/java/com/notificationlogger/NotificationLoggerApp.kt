package com.notificationlogger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.notificationlogger.data.AppPreferences
import com.notificationlogger.data.NotificationDatabase

/**
 * Application class for global initialization.
 */
class NotificationLoggerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize singletons
        AppPreferences.getInstance(this)
        NotificationDatabase.getInstance(this)

        // Create notification channel
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transaction Categories",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows transaction category after logging"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "category_notifications"
    }
}
