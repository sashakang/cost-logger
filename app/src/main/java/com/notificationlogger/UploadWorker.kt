package com.notificationlogger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notificationlogger.data.AppPreferences
import com.notificationlogger.data.NotificationDatabase
import com.notificationlogger.data.NotificationEntry
import com.notificationlogger.data.UploadResult
import com.notificationlogger.sheets.SheetsService

/**
 * WorkManager worker that uploads queued notifications to Google Sheets.
 * Handles batch uploads with proper error handling and retry logic.
 * After upload, reads category from column I and shows notification to user.
 */
class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = NotificationDatabase.getInstance(context)
    private val sheetsService = SheetsService.getInstance(context)
    private val prefs = AppPreferences.getInstance(context)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting upload work")

        // Check if user is authenticated
        if (!sheetsService.isSignedIn()) {
            Log.w(TAG, "User not signed in, skipping upload")
            return Result.success() // Don't retry if not signed in
        }

        // Check if sheet ID is configured
        val sheetId = prefs.sheetId
        if (sheetId.isNullOrBlank()) {
            Log.w(TAG, "Sheet ID not configured, skipping upload")
            return Result.success() // Don't retry if no sheet ID
        }

        // Get pending entries (limit to batch size)
        val pendingEntries = database.notificationDao().getPendingEntries(BATCH_SIZE)

        if (pendingEntries.isEmpty()) {
            Log.d(TAG, "No pending entries to upload")
            return Result.success()
        }

        Log.d(TAG, "Uploading ${pendingEntries.size} entries to sheet: $sheetId")

        // Upload batch with row info
        val (uploadResult, appendResult) = sheetsService.uploadBatchWithRowInfo(pendingEntries, sheetId)

        return when (uploadResult) {
            is UploadResult.Success -> {
                // Mark entries as uploaded
                pendingEntries.forEach { entry ->
                    database.notificationDao().markAsUploaded(entry.id)
                }
                Log.d(TAG, "Successfully uploaded ${uploadResult.rowsAdded} entries")

                // Read categories and show notifications
                if (appendResult?.startRow != null) {
                    showCategoryNotifications(pendingEntries, sheetId, appendResult.startRow)
                }

                // Check if there are more entries to upload
                val remainingCount = database.notificationDao().getPendingCount()
                if (remainingCount > 0) {
                    Log.d(TAG, "$remainingCount entries remaining, scheduling another upload")
                }

                Result.success()
            }

            is UploadResult.Failure -> {
                Log.e(TAG, "Upload failed: ${uploadResult.error.message}")

                if (uploadResult.retryable && runAttemptCount < MAX_RETRIES) {
                    Log.d(TAG, "Retrying upload (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
                    Result.retry()
                } else {
                    Log.e(TAG, "Upload failed permanently after $runAttemptCount attempts")
                    Result.failure()
                }
            }

            UploadResult.Pending -> {
                // Shouldn't happen, but handle gracefully
                Result.retry()
            }
        }
    }

    /**
     * Read category from column I for each uploaded entry and show notification.
     */
    private suspend fun showCategoryNotifications(
        entries: List<NotificationEntry>,
        sheetId: String,
        startRow: Int
    ) {
        // Check notification permission
        if (!hasNotificationPermission()) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notifications")
            return
        }

        entries.forEachIndexed { index, entry ->
            val rowNum = startRow + index
            val category = sheetsService.readCell(sheetId, "Sheet1!I$rowNum") ?: "Uncategorized"
            showCategoryNotification(entry, category)
        }
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (required for Android 13+).
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required before Android 13
        }
    }

    /**
     * Show a notification with the transaction category.
     */
    private fun showCategoryNotification(entry: NotificationEntry, category: String) {
        val notification = NotificationCompat.Builder(applicationContext, NotificationLoggerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Transaction Logged")
            .setContentText("${entry.title}: $category")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(entry.id.toInt(), notification)
            Log.d(TAG, "Showed notification for ${entry.title}: $category")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot show notification - permission denied", e)
        }
    }

    companion object {
        private const val TAG = "UploadWorker"
        private const val BATCH_SIZE = 50
        private const val MAX_RETRIES = 3
    }
}
