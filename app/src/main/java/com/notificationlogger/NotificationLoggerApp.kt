package com.notificationlogger

import android.app.Application
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
    }
}
