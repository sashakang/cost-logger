package com.notificationlogger

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.notificationlogger.data.TransactionEntry
import com.notificationlogger.data.UploadResult
import com.notificationlogger.sheets.SheetsService

/**
 * WorkManager worker that uploads queued notifications and transactions to Google Sheets.
 * Handles batch uploads with proper error handling and retry logic.
 *
 * For notifications: After upload, reads category from column I and shows notification to user.
 * For transactions: Writes the pre-selected category to column I immediately (no notification needed).
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

        // Get pending notification entries (limit to batch size)
        val pendingEntries = database.notificationDao().getPendingEntries(BATCH_SIZE)

        val tabName = prefs.sheetTabName

        // Handle notifications first, then transactions
        if (pendingEntries.isEmpty()) {
            Log.d(TAG, "No pending notification entries to upload")
            // Still try to upload transactions even if no notifications
            val transactionSuccess = uploadTransactions(sheetId, tabName)
            return if (transactionSuccess) Result.success() else Result.retry()
        }

        Log.d(TAG, "Uploading ${pendingEntries.size} notification entries to sheet: $sheetId (tab: ${tabName.ifBlank { "first" }})")

        // Upload batch with row info
        val (uploadResult, appendResult) = sheetsService.uploadBatchWithRowInfo(pendingEntries, sheetId, tabName)

        return when (uploadResult) {
            is UploadResult.Success -> {
                // Mark entries as uploaded
                pendingEntries.forEach { entry ->
                    database.notificationDao().markAsUploaded(entry.id)
                }
                Log.d(TAG, "Successfully uploaded ${uploadResult.rowsAdded} entries")

                // Read categories and show notifications
                if (appendResult?.startRow != null) {
                    showCategoryNotifications(pendingEntries, sheetId, tabName, appendResult.startRow)
                }

                // Upload transactions after notifications
                val transactionSuccess = uploadTransactions(sheetId, tabName)
                if (!transactionSuccess) {
                    Log.e(TAG, "Transaction upload failed but notification upload succeeded")
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
        tabName: String,
        startRow: Int
    ) {
        // Check notification permission
        if (!hasNotificationPermission()) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notifications")
            return
        }

        val rangePrefix = if (tabName.isBlank()) "" else "$tabName!"
        entries.forEachIndexed { index, entry ->
            val rowNum = startRow + index
            val category = sheetsService.readCell(sheetId, "${rangePrefix}I$rowNum") ?: "Uncategorized"
            showCategoryNotification(entry, category, rowNum, tabName)
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
     * Upload pending transactions to Google Sheets.
     * Transactions are uploaded with their category already set (no notification needed).
     *
     * @return true if upload succeeded, false otherwise
     */
    private suspend fun uploadTransactions(sheetId: String, tabName: String): Boolean {
        val pendingTransactions = database.transactionDao().getPendingEntries(BATCH_SIZE)

        if (pendingTransactions.isEmpty()) {
            Log.d(TAG, "No pending transactions to upload")
            return true
        }

        Log.d(TAG, "Uploading ${pendingTransactions.size} transactions to sheet: $sheetId (tab: ${tabName.ifBlank { "first" }})")

        val accessToken = sheetsService.getAccessToken()
        if (accessToken == null) {
            Log.w(TAG, "Cannot get access token for transaction upload")
            return false
        }

        val rangePrefix = if (tabName.isBlank()) "" else "$tabName!"

        return try {
            // Convert transactions to sheet rows (columns A-F)
            val rows = pendingTransactions.map { it.toSheetRow() }

            // Append rows to sheet
            val appendResult = sheetsService.appendRowsGeneric(sheetId, rows, tabName)

            if (appendResult.success && appendResult.startRow != null) {
                // Write categories to column I for each transaction
                // Note: We write categories individually rather than batch to keep the implementation
                // simple. This could be optimized with batch updates if API rate limits become an issue.
                pendingTransactions.forEachIndexed { index, transaction ->
                    val rowNum = appendResult.startRow + index
                    val writeSuccess = sheetsService.writeCell(sheetId, "${rangePrefix}I$rowNum", transaction.category)
                    if (writeSuccess) {
                        database.transactionDao().markAsUploaded(transaction.id)
                        Log.d(TAG, "Uploaded transaction ${transaction.id} to row $rowNum with category ${transaction.category}")
                    } else {
                        Log.w(TAG, "Failed to write category for transaction ${transaction.id}")
                    }
                }
                true
            } else {
                Log.e(TAG, "Failed to append transaction rows")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transaction upload failed", e)
            false
        }
    }

    /**
     * Show a notification with the transaction category.
     * Clicking the notification opens CategorySelectionActivity to change the category.
     */
    private fun showCategoryNotification(entry: NotificationEntry, category: String, rowNumber: Int, tabName: String) {
        // Create intent to open CategorySelectionActivity
        val intent = Intent(applicationContext, CategorySelectionActivity::class.java).apply {
            putExtra(CategorySelectionActivity.EXTRA_ROW_NUMBER, rowNumber)
            putExtra(CategorySelectionActivity.EXTRA_APP_NAME, entry.appName)
            putExtra(CategorySelectionActivity.EXTRA_TRANSACTION_TITLE, entry.title)
            putExtra(CategorySelectionActivity.EXTRA_TRANSACTION_TEXT, entry.text)
            putExtra(CategorySelectionActivity.EXTRA_TAB_NAME, tabName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            rowNumber, // unique request code per row
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationLoggerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Transaction Logged")
            .setContentText("${entry.title}: $category")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(entry.id.toInt(), notification)
            Log.d(TAG, "Showed notification for ${entry.title}: $category (row $rowNumber)")
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
