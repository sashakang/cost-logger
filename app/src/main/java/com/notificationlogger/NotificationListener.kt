package com.notificationlogger

import android.app.Notification
import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.*
import com.notificationlogger.data.AppPreferences
import com.notificationlogger.data.NotificationDatabase
import com.notificationlogger.data.NotificationEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Core service that intercepts all device notifications.
 * Filters by whitelist and queues for upload to Google Sheets.
 */
class NotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: AppPreferences
    private lateinit var database: NotificationDatabase
    private var isConnected = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = AppPreferences.getInstance(applicationContext)
        database = NotificationDatabase.getInstance(applicationContext)
        Log.d(TAG, "NotificationListener created")
    }

    override fun onDestroy() {
        instance = null
        isConnected = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d(TAG, "NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d(TAG, "NotificationListener disconnected - requesting rebind")
        // Try to reconnect if user didn't explicitly revoke
        requestRebind(ComponentName(this, NotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        try {
            processNotification(sbn)
        } catch (e: Exception) {
            // Service must remain stable - never crash
            Log.e(TAG, "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: track notification dismissals if needed
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Skip if not in whitelist
        if (!prefs.isWhitelisted(packageName)) {
            return
        }

        // Skip system UI notifications
        if (packageName == "android" || packageName == "com.android.systemui") {
            return
        }

        // Extract notification data
        val entry = sbn.toNotificationEntry() ?: return

        Log.d(TAG, "Queuing notification from ${entry.appName}: ${entry.title}")

        // Queue for upload
        serviceScope.launch {
            try {
                database.notificationDao().insert(entry)
                scheduleUpload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to queue notification", e)
            }
        }
    }

    /**
     * Convert StatusBarNotification to our data model
     */
    private fun StatusBarNotification.toNotificationEntry(): NotificationEntry? {
        val extras = notification?.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        // Skip empty notifications
        if (title.isBlank() && text.isBlank()) {
            return null
        }

        return NotificationEntry(
            notificationKey = key,
            appName = getAppName(packageName),
            packageName = packageName,
            title = title,
            text = text,
            timestamp = postTime
        )
    }

    /**
     * Get human-readable app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Schedule WorkManager job to upload queued notifications
     */
    private fun scheduleUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                UPLOAD_WORK_NAME,
                ExistingWorkPolicy.KEEP, // Don't duplicate if already scheduled
                uploadWork
            )
    }

    /**
     * Scan all currently active notifications and log those not yet in database.
     * Returns the count of newly logged notifications.
     */
    suspend fun scanActiveNotifications(): Int {
        val activeNotifs = activeNotifications ?: emptyArray()
        Log.d(TAG, "=== SCAN START ===")
        Log.d(TAG, "Total active notifications: ${activeNotifs.size}")
        Log.d(TAG, "Whitelisted apps: ${prefs.whitelistedApps}")

        activeNotifs.forEach { sbn ->
            val isWhitelisted = prefs.isWhitelisted(sbn.packageName)
            Log.d(TAG, "  - ${sbn.packageName}: whitelisted=$isWhitelisted")
        }

        val entries = activeNotifs.mapNotNull { sbn ->
            if (prefs.isWhitelisted(sbn.packageName)) {
                sbn.toNotificationEntry()
            } else null
        }

        Log.d(TAG, "Entries after whitelist filter: ${entries.size}")

        var newCount = 0
        entries.forEach { entry ->
            val result = database.notificationDao().insertIfNotExists(entry)
            Log.d(TAG, "  Insert ${entry.appName}: result=$result (new=${result != -1L})")
            if (result != -1L) newCount++
        }

        if (newCount > 0) {
            scheduleUpload()
        }

        Log.d(TAG, "=== SCAN END: $newCount new entries ===")
        return newCount
    }

    /**
     * Check if the service is connected to Android's notification system
     */
    fun isServiceConnected(): Boolean = isConnected

    companion object {
        private const val TAG = "NotificationListener"
        private const val UPLOAD_WORK_NAME = "notification_upload"

        @Volatile
        private var instance: NotificationListener? = null

        /**
         * Get the current service instance, or null if service is not running
         */
        fun getInstance(): NotificationListener? = instance

        /**
         * Check if notification listener permission is enabled
         */
        fun isEnabled(context: android.content.Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(context.packageName) == true
        }
    }
}
