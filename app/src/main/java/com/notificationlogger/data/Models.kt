package com.notificationlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a captured notification entry.
 * Used both as a data class and Room entity.
 */
@Entity(tableName = "notifications")
data class NotificationEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Unique key from StatusBarNotification.getKey() - unique per notification instance */
    val notificationKey: String = "",
    val appName: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val uploaded: Boolean = false
) {
    init {
        require(packageName.isNotBlank()) { "Package name cannot be blank" }
    }

    /**
     * Convert to list format for Google Sheets API
     */
    fun toSheetRow(): List<String> = listOf(
        appName,
        title,
        text,
        formatTimestamp(timestamp),
        notificationKey
    )

    companion object {
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }
}

/**
 * Sealed interface for upload operation results
 */
sealed interface UploadResult {
    data class Success(val rowsAdded: Int) : UploadResult
    data class Failure(val error: Throwable, val retryable: Boolean) : UploadResult
    data object Pending : UploadResult
}

/**
 * App info for whitelist selection
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSelected: Boolean = false
)
