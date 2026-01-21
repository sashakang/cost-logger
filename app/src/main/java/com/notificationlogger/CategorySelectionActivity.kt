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

                    val rangePrefix = prefs.getSheetRangePrefix()

                    CategorySelectionScreen(
                        appName = appName,
                        transactionTitle = transactionTitle,
                        transactionText = transactionText,
                        categories = prefs.categories,
                        isLoading = isLoading,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            if (!isLoading) {
                                selectedCategory = category
                                isLoading = true
                                scope.launch {
                                    val success = sheetsService.writeCell(
                                        sheetId,
                                        "${rangePrefix}I$rowNumber",
                                        category
                                    )
                                    if (success) {
                                        Toast.makeText(
                                            this@CategorySelectionActivity,
                                            "Saved",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Find and open next uncategorized row
                                        openNextNotification(sheetId, rowNumber, tabName)
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
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    /**
     * Find and open the next uncategorized notification.
     */
    private suspend fun openNextNotification(sheetId: String, currentRowNumber: Int, tabName: String) {
        try {
            val nextRow = sheetsService.findNextUncategorizedRow(sheetId, currentRowNumber, tabName)
            
            if (nextRow != null && nextRow.rowData.size >= 6) {
                // Parse row data: [UTC Timestamp, App Name, Title, Text, Local Timestamp, Notification Key, ...]
                val nextAppName = if (nextRow.rowData.size > 1) nextRow.rowData[1] else ""
                val nextTitle = if (nextRow.rowData.size > 2) nextRow.rowData[2] else ""
                val nextText = if (nextRow.rowData.size > 3) nextRow.rowData[3] else ""
                
                // Open CategorySelectionActivity for the next row
                val intent = Intent(this@CategorySelectionActivity, CategorySelectionActivity::class.java).apply {
                    putExtra(EXTRA_ROW_NUMBER, nextRow.rowNumber)
                    putExtra(EXTRA_APP_NAME, nextAppName)
                    putExtra(EXTRA_TRANSACTION_TITLE, nextTitle)
                    putExtra(EXTRA_TRANSACTION_TEXT, nextText)
                    putExtra(EXTRA_TAB_NAME, tabName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            } else {
                // No more uncategorized rows found
                Toast.makeText(
                    this@CategorySelectionActivity,
                    "All transactions categorized",
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

    companion object {
        private const val TAG = "CategorySelectionActivity"
        const val EXTRA_ROW_NUMBER = "rowNumber"
        const val EXTRA_APP_NAME = "appName"
        const val EXTRA_TRANSACTION_TITLE = "transactionTitle"
        const val EXTRA_TRANSACTION_TEXT = "transactionText"
        const val EXTRA_TAB_NAME = "tabName"
    }
}
