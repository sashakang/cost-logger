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

    var sheetTabName: String
        get() = prefs.getString(KEY_SHEET_TAB_NAME, DEFAULT_SHEET_TAB_NAME) ?: DEFAULT_SHEET_TAB_NAME
        set(value) = prefs.edit().putString(KEY_SHEET_TAB_NAME, value.ifBlank { DEFAULT_SHEET_TAB_NAME }).apply()

    /**
     * Returns the range prefix for the sheet tab (e.g., "Sheet1!" or "" if using first sheet)
     */
    fun getSheetRangePrefix(): String {
        val tabName = sheetTabName
        return if (tabName.isBlank()) "" else "$tabName!"
    }

    var whitelistedApps: Set<String>
        get() = prefs.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_WHITELIST, value).apply()

    var hasAcceptedPrivacy: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVACY_ACCEPTED, value).apply()

    var categories: List<String>
        get() = prefs.getString(KEY_CATEGORIES, DEFAULT_CATEGORIES)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        set(value) = prefs.edit().putString(KEY_CATEGORIES, value.joinToString(",")).apply()

    var recentCurrencies: List<String>
        get() = prefs.getString(KEY_RECENT_CURRENCIES, DEFAULT_RECENT_CURRENCIES)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        set(value) = prefs.edit().putString(KEY_RECENT_CURRENCIES, value.joinToString(",")).apply()

    var recentCategories: List<String>
        get() {
            val stored = prefs.getString(KEY_RECENT_CATEGORIES, null)
            return if (stored != null) {
                stored.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            } else {
                // Default to first 5 categories
                categories.take(5)
            }
        }
        set(value) = prefs.edit().putString(KEY_RECENT_CATEGORIES, value.joinToString(",")).apply()

    fun isWhitelisted(packageName: String): Boolean {
        return packageName in whitelistedApps
    }

    fun addToWhitelist(packageName: String) {
        whitelistedApps = whitelistedApps + packageName
    }

    fun removeFromWhitelist(packageName: String) {
        whitelistedApps = whitelistedApps - packageName
    }

    fun updateCurrencyRecency(currency: String) {
        updateRecencyList(KEY_RECENT_CURRENCIES, currency)
    }

    fun updateCategoryRecency(category: String) {
        updateRecencyList(KEY_RECENT_CATEGORIES, category)
    }

    private fun updateRecencyList(key: String, item: String, maxSize: Int = 10) {
        val current = prefs.getString(key, "")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val updated = (listOf(item) + current.filter { it != item }).take(maxSize)
        prefs.edit().putString(key, updated.joinToString(",")).apply()
    }

    companion object {
        private const val PREFS_NAME = "notification_logger_prefs"
        private const val KEY_SHEET_ID = "google_sheet_id"
        private const val KEY_SHEET_TAB_NAME = "sheet_tab_name"
        private const val KEY_WHITELIST = "whitelisted_apps"
        private const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_RECENT_CURRENCIES = "recent_currencies"
        private const val KEY_RECENT_CATEGORIES = "recent_categories"
        private const val DEFAULT_CATEGORIES = "Food,Transport,Shopping,Bills,Entertainment,Health,Other"
        private const val DEFAULT_RECENT_CURRENCIES = "USD,EUR,GBP"
        private const val DEFAULT_SHEET_TAB_NAME = ""  // Empty means use first sheet

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(entry: NotificationEntry): Long

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

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TransactionEntry): Long

    @Query("SELECT * FROM transactions WHERE uploaded = 0 ORDER BY timestamp ASC")
    suspend fun getPendingEntries(): List<TransactionEntry>

    @Query("SELECT * FROM transactions WHERE uploaded = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingEntries(limit: Int): List<TransactionEntry>

    @Query("SELECT COUNT(*) FROM transactions WHERE uploaded = 0")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE transactions SET uploaded = 1 WHERE id = :id")
    suspend fun markAsUploaded(id: Long)
}

@Database(
    entities = [NotificationEntry::class, TransactionEntry::class],
    version = 6,
    exportSchema = false
)
abstract class NotificationDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun transactionDao(): TransactionDao

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
