package com.notificationlogger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.notificationlogger.data.AppPreferences
import com.notificationlogger.sheets.SheetsService
import com.notificationlogger.ui.CategorySelectionScreen
import com.notificationlogger.ui.theme.NotificationLoggerTheme
import kotlinx.coroutines.launch

/**
 * Activity for selecting a category from a grid.
 * Launched when user clicks on a category notification.
 */
class CategorySelectionActivity : ComponentActivity() {

    private lateinit var sheetsService: SheetsService
    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sheetsService = SheetsService.getInstance(applicationContext)
        prefs = AppPreferences.getInstance(applicationContext)

        val rowNumber = intent.getIntExtra(EXTRA_ROW_NUMBER, -1)
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
        val transactionTitle = intent.getStringExtra(EXTRA_TRANSACTION_TITLE) ?: ""
        val transactionText = intent.getStringExtra(EXTRA_TRANSACTION_TEXT) ?: ""
        val tabName = intent.getStringExtra(EXTRA_TAB_NAME) ?: prefs.sheetTabName

        if (rowNumber == -1) {
            Toast.makeText(this, "Error: Missing row number", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sheetId = prefs.sheetId
        if (sheetId.isNullOrBlank()) {
            Toast.makeText(this, "Error: Sheet ID not configured", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            NotificationLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    var isLoading by remember { mutableStateOf(false) }
                    var selectedCategory by remember { mutableStateOf<String?>(null) }
                    var initialComment by remember { mutableStateOf("") }

                    val rangePrefix = if (tabName.isBlank()) "" else "$tabName!"

                    // PRE-SELECT CATEGORY & COMMENT: Fetch existing category and comment from sheet
                    LaunchedEffect(Unit) {
                        try {
                            // Read Category from Column I
                            val existingCategory = sheetsService.readCell(
                                sheetId,
                                "${rangePrefix}I$rowNumber"
                            )
                            if (!existingCategory.isNullOrBlank() && !existingCategory.equals("Uncategorized", ignoreCase = true)) {
                                selectedCategory = existingCategory
                            }

                            // Read Comment from Column M
                            val existingComment = sheetsService.readCell(
                                sheetId,
                                "${rangePrefix}M$rowNumber"
                            )
                            if (!existingComment.isNullOrBlank()) {
                                initialComment = existingComment
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching existing data", e)
                        }
                    }

                    CategorySelectionScreen(
                        appName = appName,
                        transactionTitle = transactionTitle,
                        transactionText = transactionText,
                        categories = prefs.categories,
                        isLoading = isLoading,
                        selectedCategory = selectedCategory,
                        initialComment = initialComment,
                        onCategorySelected = { category, comment ->
                            if (!isLoading) {
                                selectedCategory = category
                                isLoading = true
                                scope.launch {
                                    // Write Category to Column I
                                    val catSuccess = sheetsService.writeCell(
                                        sheetId,
                                        "${rangePrefix}I$rowNumber",
                                        category
                                    )
                                    // Write Comment to Column M
                                    val commentSuccess = sheetsService.writeCell(
                                        sheetId,
                                        "${rangePrefix}M$rowNumber",
                                        comment
                                    )
                                    
                                    if (!commentSuccess) {
                                        Log.w(TAG, "Failed to save comment for row $rowNumber")
                                    }

                                    if (catSuccess) { // Considering category write success as primary
                                        Toast.makeText(
                                            this@CategorySelectionActivity,
                                            "Saved",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Find and open next uncategorized row
                                        openNextNotification(rowNumber, appName)
                                    } else {
                                        Toast.makeText(
                                            this@CategorySelectionActivity,
                                            "Failed to save. Try again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        onApprove = { comment ->
                            if (!isLoading) {
                                isLoading = true
                                scope.launch {
                                    val commentSuccess = if (comment.isNotBlank()) {
                                        sheetsService.writeCell(
                                            sheetId,
                                            "${rangePrefix}M$rowNumber",
                                            comment
                                        )
                                    } else {
                                        true
                                    }

                                    if (!commentSuccess) {
                                        Log.w(TAG, "Failed to save comment for row $rowNumber")
                                        Toast.makeText(
                                            this@CategorySelectionActivity,
                                            "Failed to save comment. Try again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isLoading = false
                                        return@launch
                                    }

                                    openNextNotification(rowNumber, appName)
                                    finish()
                                }
                            }
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    /**
     * Find and open the next uncategorized notification from active notifications.
     */
    private suspend fun openNextNotification(currentRowNumber: Int, appName: String) {
        try {
            val nextNotification = findNextActiveNotification(currentRowNumber, appName)
            if (nextNotification != null) {
                val nextTabName = nextNotification.tabName.ifBlank { prefs.sheetTabName }
                val intent = Intent(this@CategorySelectionActivity, CategorySelectionActivity::class.java).apply {
                    putExtra(EXTRA_ROW_NUMBER, nextNotification.rowNumber)
                    putExtra(EXTRA_APP_NAME, nextNotification.appName)
                    putExtra(EXTRA_TRANSACTION_TITLE, nextNotification.title)
                    putExtra(EXTRA_TRANSACTION_TEXT, nextNotification.text)
                    putExtra(EXTRA_TAB_NAME, nextTabName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this@CategorySelectionActivity,
                    "No more notifications to categorize",
                    Toast.LENGTH_SHORT
                ).show()
                moveTaskToBack(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding next notification", e)
            Toast.makeText(
                this@CategorySelectionActivity,
                "Error finding next notification",
                Toast.LENGTH_SHORT
            ).show()
            moveTaskToBack(true)
        }
    }

    private fun findNextActiveNotification(currentRowNumber: Int, appName: String): ActiveNotification? {
        val listener = NotificationListener.getInstance()
        if (listener == null) {
            Log.w(TAG, "Notification listener not running; cannot find next notification")
            return null
        }

        val activeNotifications = listener.activeNotifications ?: emptyArray()
        return activeNotifications
            .mapNotNull { sbn ->
                val notification = sbn.notification
                if (sbn.packageName != applicationContext.packageName) return@mapNotNull null
                if (notification.channelId != NotificationLoggerApp.CHANNEL_ID) return@mapNotNull null

                val extras = notification.extras ?: return@mapNotNull null
                val rowNumber = extras.getInt(EXTRA_ROW_NUMBER, -1)
                val extraAppName = extras.getString(EXTRA_APP_NAME).orEmpty()
                if (rowNumber == -1 || rowNumber == currentRowNumber) return@mapNotNull null
                if (extraAppName != appName) return@mapNotNull null

                ActiveNotification(
                    rowNumber = rowNumber,
                    appName = extraAppName,
                    title = extras.getString(EXTRA_TRANSACTION_TITLE).orEmpty(),
                    text = extras.getString(EXTRA_TRANSACTION_TEXT).orEmpty(),
                    tabName = extras.getString(EXTRA_TAB_NAME).orEmpty(),
                    postTime = sbn.postTime
                )
            }
            .minByOrNull { it.postTime }
    }

    private data class ActiveNotification(
        val rowNumber: Int,
        val appName: String,
        val title: String,
        val text: String,
        val tabName: String,
        val postTime: Long
    )

    companion object {
        private const val TAG = "CategorySelectionActivity"
        const val EXTRA_ROW_NUMBER = "rowNumber"
        const val EXTRA_APP_NAME = "appName"
        const val EXTRA_TRANSACTION_TITLE = "transactionTitle"
        const val EXTRA_TRANSACTION_TEXT = "transactionText"
        const val EXTRA_TAB_NAME = "tabName"
    }
}
