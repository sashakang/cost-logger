package com.notificationlogger.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ============================================================================
// App Preferences - SharedPreferences wrapper for whitelist and settings
// ============================================================================

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    var sheetId: String?
        get() = prefs.getString(KEY_SHEET_ID, null)
        set(value) = prefs.edit().putString(KEY_SHEET_ID, value).apply()

    var whitelistedApps: Set<String>
        get() = prefs.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_WHITELIST, value).apply()

    var hasAcceptedPrivacy: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVACY_ACCEPTED, value).apply()

    fun isWhitelisted(packageName: String): Boolean {
        return packageName in whitelistedApps
    }

    fun addToWhitelist(packageName: String) {
        whitelistedApps = whitelistedApps + packageName
    }

    fun removeFromWhitelist(packageName: String) {
        whitelistedApps = whitelistedApps - packageName
    }

    companion object {
        private const val PREFS_NAME = "notification_logger_prefs"
        private const val KEY_SHEET_ID = "google_sheet_id"
        private const val KEY_WHITELIST = "whitelisted_apps"
        private const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}

// ============================================================================
// Room Database - DAO and Database for offline queue
// ============================================================================

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: NotificationEntry): Long

    @Update
    suspend fun update(entry: NotificationEntry)

    @Delete
    suspend fun delete(entry: NotificationEntry)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM notifications WHERE uploaded = 0 ORDER BY timestamp ASC")
    suspend fun getPendingEntries(): List<NotificationEntry>

    @Query("SELECT * FROM notifications WHERE uploaded = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingEntries(limit: Int): List<NotificationEntry>

    @Query("SELECT COUNT(*) FROM notifications WHERE uploaded = 0")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE uploaded = 0")
    suspend fun getPendingCount(): Int

    @Query("UPDATE notifications SET uploaded = 1 WHERE id = :id")
    suspend fun markAsUploaded(id: Long)

    @Query("DELETE FROM notifications WHERE uploaded = 1")
    suspend fun deleteUploaded()

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int = 50): List<NotificationEntry>
}

@Database(
    entities = [NotificationEntry::class],
    version = 3,
    exportSchema = false
)
abstract class NotificationDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao

    companion object {
        private const val DATABASE_NAME = "notification_logger_db"

        @Volatile
        private var instance: NotificationDatabase? = null

        fun getInstance(context: Context): NotificationDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): NotificationDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NotificationDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
