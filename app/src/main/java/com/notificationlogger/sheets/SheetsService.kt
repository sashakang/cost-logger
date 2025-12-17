package com.notificationlogger.sheets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.notificationlogger.data.NotificationEntry
import com.notificationlogger.data.UploadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Combined Google Auth + Sheets upload service.
 * Uses GoogleSignIn API for consistent auth state and access tokens.
 */
class SheetsService(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SHEETS_SCOPE))
            .requestIdToken(WEB_CLIENT_ID)
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    // ========================================================================
    // Authentication
    // ========================================================================

    /**
     * Check if user is signed in with Google
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(account, Scope(SHEETS_SCOPE))
    }

    /**
     * Get the signed-in user's email
     */
    fun getSignedInEmail(): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }

    /**
     * Get sign-in intent to launch with ActivityResultLauncher
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handle the result from sign-in activity
     */
    fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(Exception::class.java)
            val email = account?.email ?: "Unknown"
            Log.d(TAG, "Signed in as: $email")
            Result.success(email)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out and clear credentials
     */
    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                googleSignInClient.signOut()
                Log.d(TAG, "Signed out successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed", e)
            }
        }
    }

    /**
     * Get OAuth access token for API calls
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: run {
            Log.w(TAG, "No signed-in account found")
            return@withContext null
        }

        val googleAccount = account.account ?: run {
            Log.w(TAG, "No account object available")
            return@withContext null
        }

        try {
            val scope = "oauth2:$SHEETS_SCOPE"
            val token = GoogleAuthUtil.getToken(context, googleAccount, scope)
            Log.d(TAG, "Successfully obtained access token")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get access token", e)
            null
        }
    }

    // ========================================================================
    // Google Sheets Upload
    // ========================================================================

    /**
     * Upload a single notification entry to Google Sheets
     */
    suspend fun uploadEntry(entry: NotificationEntry, sheetId: String): UploadResult = withContext(Dispatchers.IO) {
        if (sheetId.isBlank()) {
            return@withContext UploadResult.Failure(
                Exception("Sheet ID not configured"),
                retryable = false
            )
        }

        val accessToken = getAccessToken()
        if (accessToken == null) {
            return@withContext UploadResult.Failure(
                Exception("Not authenticated"),
                retryable = false
            )
        }

        try {
            val result = appendRow(sheetId, accessToken, entry.toSheetRow())
            if (result.success) {
                UploadResult.Success(1)
            } else {
                UploadResult.Failure(Exception("Upload failed"), retryable = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            UploadResult.Failure(e, retryable = isRetryable(e))
        }
    }

    /**
     * Upload multiple entries in batch and return row information for reading categories.
     */
    suspend fun uploadBatchWithRowInfo(
        entries: List<NotificationEntry>,
        sheetId: String
    ): Pair<UploadResult, AppendResult?> = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) {
            return@withContext Pair(UploadResult.Success(0), null)
        }

        if (sheetId.isBlank()) {
            return@withContext Pair(
                UploadResult.Failure(Exception("Sheet ID not configured"), retryable = false),
                null
            )
        }

        val accessToken = getAccessToken()
        if (accessToken == null) {
            return@withContext Pair(
                UploadResult.Failure(Exception("Not authenticated"), retryable = false),
                null
            )
        }

        try {
            val rows = entries.map { it.toSheetRow() }
            val appendResult = appendRows(sheetId, accessToken, rows)
            if (appendResult.success) {
                Pair(UploadResult.Success(entries.size), appendResult)
            } else {
                Pair(UploadResult.Failure(Exception("Batch upload failed"), retryable = true), null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Batch upload failed", e)
            Pair(UploadResult.Failure(e, retryable = isRetryable(e)), null)
        }
    }

    /**
     * Upload multiple entries in batch (legacy method without row info)
     */
    suspend fun uploadBatch(entries: List<NotificationEntry>, sheetId: String): UploadResult {
        return uploadBatchWithRowInfo(entries, sheetId).first
    }

    /**
     * Append a single row to the sheet
     */
    private fun appendRow(sheetId: String, accessToken: String, row: List<String>): AppendResult {
        return appendRows(sheetId, accessToken, listOf(row))
    }

    /**
     * Append multiple rows to the sheet using Sheets API.
     * Returns AppendResult with the starting row number of appended data.
     */
    private fun appendRows(sheetId: String, accessToken: String, rows: List<List<String>>): AppendResult {
        val url = "$SHEETS_API_BASE/spreadsheets/$sheetId/values/Sheet1!A:F:append" +
                "?valueInputOption=USER_ENTERED" +
                "&insertDataOption=INSERT_ROWS"

        val values = JSONArray().apply {
            rows.forEach { row ->
                put(JSONArray(row))
            }
        }

        val requestBody = JSONObject().apply {
            put("values", values)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            val success = response.isSuccessful
            response.close()

            if (!success) {
                Log.e(TAG, "Sheets API error: ${response.code} - $responseBody")
                AppendResult(success = false)
            } else {
                Log.d(TAG, "Successfully appended ${rows.size} rows to sheet")
                // Parse updatedRange to get starting row number (e.g., "Sheet1!A5:F7" -> 5)
                val startRow = parseStartRowFromResponse(responseBody)
                AppendResult(success = true, startRow = startRow, rowCount = rows.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            AppendResult(success = false)
        }
    }

    /**
     * Parse the starting row number from Sheets API append response.
     * Response contains "updates": { "updatedRange": "Sheet1!A5:F7" }
     */
    private fun parseStartRowFromResponse(responseBody: String?): Int? {
        if (responseBody == null) return null
        return try {
            val json = JSONObject(responseBody)
            val updatedRange = json.optJSONObject("updates")?.optString("updatedRange")
            // Extract row number from range like "Sheet1!A5:F7" -> 5
            updatedRange?.let {
                val match = Regex("""!A(\d+):""").find(it)
                match?.groupValues?.get(1)?.toIntOrNull()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse start row from response", e)
            null
        }
    }

    // ========================================================================
    // Google Sheets Read
    // ========================================================================

    /**
     * Read a single cell value from the sheet.
     * @param sheetId The Google Sheet ID
     * @param cell Cell reference like "Sheet1!I5"
     * @return The cell value or null if empty/error
     */
    suspend fun readCell(sheetId: String, cell: String): String? = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext null

        val url = "$SHEETS_API_BASE/spreadsheets/$sheetId/values/$cell"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            response.close()

            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to read cell $cell: ${response.code}")
                return@withContext null
            }

            // Parse response: { "values": [["cell_value"]] }
            val json = JSONObject(responseBody ?: "{}")
            val values = json.optJSONArray("values")
            val firstRow = values?.optJSONArray(0)
            val cellValue = firstRow?.optString(0)

            if (cellValue.isNullOrBlank()) null else cellValue
        } catch (e: Exception) {
            Log.w(TAG, "Error reading cell $cell", e)
            null
        }
    }

    /**
     * Result of append operation with row information
     */
    data class AppendResult(
        val success: Boolean,
        val startRow: Int? = null,
        val rowCount: Int = 0
    )

    /**
     * Check if error is retryable (network issues, rate limits)
     */
    private fun isRetryable(e: Exception): Boolean {
        return when {
            e.message?.contains("timeout", ignoreCase = true) == true -> true
            e.message?.contains("network", ignoreCase = true) == true -> true
            e.message?.contains("429") == true -> true // Rate limited
            e.message?.contains("500") == true -> true // Server error
            e.message?.contains("503") == true -> true // Service unavailable
            else -> false
        }
    }

    companion object {
        private const val TAG = "SheetsService"
        private const val SHEETS_API_BASE = "https://sheets.googleapis.com/v4"
        private const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"

        const val WEB_CLIENT_ID = "956261646846-q08u1vq24id9pdspmhfdo47dc722qqbl.apps.googleusercontent.com"

        @Volatile
        private var instance: SheetsService? = null

        fun getInstance(context: Context): SheetsService {
            return instance ?: synchronized(this) {
                instance ?: SheetsService(context.applicationContext).also { instance = it }
            }
        }
    }
}
