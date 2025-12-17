package com.notificationlogger

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notificationlogger.data.AppPreferences
import com.notificationlogger.data.NotificationDatabase
import com.notificationlogger.data.UploadResult
import com.notificationlogger.sheets.SheetsService

/**
 * WorkManager worker that uploads queued notifications to Google Sheets.
 * Handles batch uploads with proper error handling and retry logic.
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

        // Upload batch
        return when (val result = sheetsService.uploadBatch(pendingEntries, sheetId)) {
            is UploadResult.Success -> {
                // Mark entries as uploaded
                pendingEntries.forEach { entry ->
                    database.notificationDao().markAsUploaded(entry.id)
                }
                Log.d(TAG, "Successfully uploaded ${result.rowsAdded} entries")

                // Check if there are more entries to upload
                val remainingCount = database.notificationDao().getPendingCount()
                if (remainingCount > 0) {
                    Log.d(TAG, "$remainingCount entries remaining, scheduling another upload")
                }

                Result.success()
            }

            is UploadResult.Failure -> {
                Log.e(TAG, "Upload failed: ${result.error.message}")

                if (result.retryable && runAttemptCount < MAX_RETRIES) {
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

    companion object {
        private const val TAG = "UploadWorker"
        private const val BATCH_SIZE = 50
        private const val MAX_RETRIES = 3
    }
}
