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
            val result = appendRow(sheetId, accessToken, entry.toSheetRow(), "")
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
     * @param tabName Optional tab/worksheet name. Empty string uses the first sheet.
     */
    suspend fun uploadBatchWithRowInfo(
        entries: List<NotificationEntry>,
        sheetId: String,
        tabName: String = ""
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
            val appendResult = appendRows(sheetId, accessToken, rows, tabName)
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
    suspend fun uploadBatch(entries: List<NotificationEntry>, sheetId: String, tabName: String = ""): UploadResult {
        return uploadBatchWithRowInfo(entries, sheetId, tabName).first
    }

    /**
     * Generic method to append rows to Google Sheets.
     * Useful for uploading any data that can be converted to List<List<String>>.
     *
     * @param sheetId The Google Sheet ID
     * @param rows The rows to append (each row is a list of strings)
     * @param tabName Optional tab/worksheet name. Empty string uses the first sheet.
     * @return AppendResult containing success status and starting row number
     */
    suspend fun appendRowsGeneric(sheetId: String, rows: List<List<String>>, tabName: String = ""): AppendResult = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) {
            return@withContext AppendResult(success = true, startRow = null, rowCount = 0)
        }

        val accessToken = getAccessToken()
        if (accessToken == null) {
            return@withContext AppendResult(success = false)
        }

        appendRows(sheetId, accessToken, rows, tabName)
    }

    /**
     * Append a single row to the sheet
     */
    private fun appendRow(sheetId: String, accessToken: String, row: List<String>, tabName: String = ""): AppendResult {
        return appendRows(sheetId, accessToken, listOf(row), tabName)
    }

    /**
     * Append multiple rows to the sheet using Sheets API.
     * Returns AppendResult with the starting row number of appended data.
     * @param tabName Optional tab/worksheet name. Empty string uses the first sheet.
     */
    private fun appendRows(sheetId: String, accessToken: String, rows: List<List<String>>, tabName: String = ""): AppendResult {
        val rangePrefix = if (tabName.isBlank()) "" else "$tabName!"
        val url = "$SHEETS_API_BASE/spreadsheets/$sheetId/values/${rangePrefix}A:F:append" +
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
                // Parse updatedRange to get starting row number (e.g., "Sheet1!A5:L7" -> 5)
                val startRow = parseStartRowFromResponse(responseBody)
                if (startRow != null) {
                    val sheetTabId = getSheetTabId(accessToken, sheetId, tabName)
                    if (sheetTabId != null) {
                        val copySuccess = copyTemplateToRows(
                            accessToken = accessToken,
                            spreadsheetId = sheetId,
                            sheetTabId = sheetTabId,
                            startRow = startRow,
                            rowCount = rows.size
                        )
                        if (!copySuccess) {
                            Log.w(TAG, "Failed to copy template to rows $startRow-${startRow + rows.size - 1}")
                        }
                    } else {
                        Log.w(TAG, "Unable to resolve sheet tab ID for template copy")
                    }
                }
                AppendResult(success = true, startRow = startRow, rowCount = rows.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            AppendResult(success = false)
        }
    }

    /**
     * Parse the starting row number from Sheets API append response.
     * Response contains "updates": { "updatedRange": "Sheet1!A5:L7" } or "A5:L7"
     */
    private fun parseStartRowFromResponse(responseBody: String?): Int? {
        if (responseBody == null) return null
        return try {
            val json = JSONObject(responseBody)
            val updatedRange = json.optJSONObject("updates")?.optString("updatedRange")
            // Extract row number from range like "Sheet1!A5:L7" or "A5:L7" -> 5
            updatedRange?.let {
                val match = Regex("""A(\d+):""").find(it)
                match?.groupValues?.get(1)?.toIntOrNull()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse start row from response", e)
            null
        }
    }

    /**
     * Resolve the numeric sheet/tab ID for a given tab name.
     */
    private fun getSheetTabId(accessToken: String, spreadsheetId: String, tabName: String): Int? {
        val url = "$SHEETS_API_BASE/spreadsheets/$spreadsheetId?fields=sheets(properties(sheetId,title))"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            response.close()

            if (!response.isSuccessful || responseBody == null) {
                Log.w(TAG, "Failed to read sheet metadata: ${response.code}")
                return null
            }

            val json = JSONObject(responseBody)
            val sheets = json.optJSONArray("sheets") ?: return null
            for (i in 0 until sheets.length()) {
                val sheet = sheets.optJSONObject(i) ?: continue
                val properties = sheet.optJSONObject("properties") ?: continue
                val title = properties.optString("title")
                val id = properties.optInt("sheetId", -1)
                if (id != -1 && (tabName.isBlank() || title == tabName)) {
                    return id
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error reading sheet metadata", e)
            null
        }
    }

    /**
     * Copy formulas + formatting from template row (G4:L4) into appended rows.
     */
    private fun copyTemplateToRows(
        accessToken: String,
        spreadsheetId: String,
        sheetTabId: Int,
        startRow: Int,
        rowCount: Int
    ): Boolean {
        if (rowCount <= 0) return true

        val templateStartRowIndex = 3
        val templateEndRowIndex = 4
        val templateStartColumnIndex = 6
        val templateEndColumnIndex = 12

        val destinationStartRowIndex = startRow - 1
        val destinationEndRowIndex = destinationStartRowIndex + rowCount

        val requestBody = JSONObject().apply {
            put("requests", JSONArray().put(
                JSONObject().put("copyPaste", JSONObject().apply {
                    put("source", JSONObject().apply {
                        put("sheetId", sheetTabId)
                        put("startRowIndex", templateStartRowIndex)
                        put("endRowIndex", templateEndRowIndex)
                        put("startColumnIndex", templateStartColumnIndex)
                        put("endColumnIndex", templateEndColumnIndex)
                    })
                    put("destination", JSONObject().apply {
                        put("sheetId", sheetTabId)
                        put("startRowIndex", destinationStartRowIndex)
                        put("endRowIndex", destinationEndRowIndex)
                        put("startColumnIndex", templateStartColumnIndex)
                        put("endColumnIndex", templateEndColumnIndex)
                    })
                    put("pasteType", "PASTE_NORMAL")
                    put("pasteOrientation", "NORMAL")
                })
            ))
        }

        val url = "$SHEETS_API_BASE/spreadsheets/$spreadsheetId:batchUpdate"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                Log.w(TAG, "Template copy failed: ${response.code} - ${response.body?.string()}")
            }
            response.close()
            success
        } catch (e: Exception) {
            Log.w(TAG, "Template copy failed", e)
            false
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
     * Read a grid of cells.
     * @param sheetId The Google Sheet ID
     * @param range Range reference like "Sheet1!A1:L10"
     * @return List of rows, where each row is a List of Strings.
     */
    suspend fun readGrid(sheetId: String, range: String): List<List<String>> = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext emptyList()

        val url = "$SHEETS_API_BASE/spreadsheets/$sheetId/values/$range"

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
                Log.w(TAG, "Failed to read grid $range: ${response.code}")
                return@withContext emptyList()
            }

            // Parse response: { "values": [["val1", "val2"], ["val3", "val4"]] }
            val json = JSONObject(responseBody ?: "{}")
            val values = json.optJSONArray("values") ?: return@withContext emptyList()

            val result = mutableListOf<List<String>>()
            for (i in 0 until values.length()) {
                val rowJson = values.optJSONArray(i)
                val rowList = mutableListOf<String>()
                if (rowJson != null) {
                    for (j in 0 until rowJson.length()) {
                        rowList.add(rowJson.optString(j, ""))
                    }
                }
                result.add(rowList)
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Error reading grid $range", e)
            emptyList()
        }
    }

    /**
     * Read a range of cells from the sheet.
     * @param sheetId The Google Sheet ID
     * @param range Range reference like "Sheet1!G1:L1" or "G1:L1"
     * @return List of cell values in order, or empty list if error/empty
     */
    suspend fun readRange(sheetId: String, range: String): List<String> = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext emptyList()

        val url = "$SHEETS_API_BASE/spreadsheets/$sheetId/values/$range"

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
                Log.w(TAG, "Failed to read range $range: ${response.code}")
                return@withContext emptyList()
            }

            // Parse response: { "values": [["val1", "val2", "val3", ...]] }
            val json = JSONObject(responseBody ?: "{}")
            val values = json.optJSONArray("values")
            val firstRow = values?.optJSONArray(0)

            if (firstRow == null) {
                return@withContext emptyList()
            }

            val result = mutableListOf<String>()
            for (i in 0 until firstRow.length()) {
                val value = firstRow.optString(i, "")
                result.add(value)
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Error reading range $range", e)
            emptyList()
        }
    }

    /**
     * Read a full row from the sheet (columns A-L).
     * @param sheetId The Google Sheet ID
     * @param rowNumber The row number (1-based)
     * @param tabName Optional tab/worksheet name. Empty string uses the first sheet.
     * @return List of cell values in order [A, B, C, D, E, F, G, H, I, J, K, L], or empty list if error/empty
     */
    suspend fun readRow(sheetId: String, rowNumber: Int, tabName: String = ""): List<String> = withContext(Dispatchers.IO) {
        val rangePrefix = if (tabName.isBlank()) "" else "$tabName!"
        val range = "${rangePrefix}A$rowNumber:L$rowNumber"
        readRange(sheetId, range)
    }

    /**
     * Result of finding next uncategorized row
     */
    data class UncategorizedRowResult(
        val rowNumber: Int,
        val rowData: List<String> // Full row data [A, B, C, D, E, F, G, H, I, J, K, L]
    )

    /**
     * Find the next row that needs categorization (column I is empty, blank, or "Uncategorized").
     * @param sheetId The Google Sheet ID
     * @param startRow The row number to start searching from (will search from startRow + 1)
     * @param tabName Optional tab/worksheet name. Empty string uses the first sheet.
     * @param maxRows Maximum number of rows to search (default 100)
     * @return UncategorizedRowResult with row number and data, or null if not found
     */
    /**
     * Find the next row that needs categorization (column I is empty, blank, or "Uncategorized").
     * Uses optimized batch reading (A:L) to avoid 100-row limit and multiple API calls.
     *
     * @param sheetId The Google Sheet ID
     * @param startRow The row number to start searching from (will search from startRow + 1)
     * @param tabName Optional tab/worksheet name. Empty string uses the first sheet.
     * @return UncategorizedRowResult with row number and data, or null if not found
     */
    suspend fun findNextUncategorizedRow(
        sheetId: String,
        startRow: Int,
        tabName: String = ""
    ): UncategorizedRowResult? = withContext(Dispatchers.IO) {
        val rangePrefix = if (tabName.isBlank()) "" else "$tabName!"
        
        // Read from the next row to the end of the sheet, columns A to L
        // Optimized: Reading potentially large range in one go.
        // Google Sheets API by default returns data only for populated rows.
        val searchRange = "${rangePrefix}A${startRow + 1}:L"
        
        try {
            val rows = readGrid(sheetId, searchRange)
            
            rows.forEachIndexed { index, rowData ->
                // rowData is [A, B, C, D, E, F, G, H, I, J, K, L] (indices 0..11)
                // Actual sheet row number
                val currentRowNum = startRow + 1 + index

                // Check Column B (App Name) at index 1 to ensure it's a valid transaction row
                // and not just some stray data
                val appName = rowData.getOrNull(1)
                
                if (!appName.isNullOrBlank()) {
                    // Check Column I (Category) at index 8
                    val category = rowData.getOrNull(8)
                    
                    val isUncategorized = category.isNullOrBlank() || 
                                          category.trim().equals("Uncategorized", ignoreCase = true)
                    
                    if (isUncategorized) {
                        Log.d(TAG, "Found uncategorized row at $currentRowNum")
                        return@withContext UncategorizedRowResult(currentRowNum, rowData)
                    }
                }
            }
            
            Log.d(TAG, "No uncategorized row found after row $startRow")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding next uncategorized row", e)
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

    // ========================================================================
    // Google Sheets Write
    // ========================================================================

    /**
     * Write a value to a specific cell in the sheet.
     * @param sheetId The Google Sheet ID
     * @param cell Cell reference like "Sheet1!I5"
     * @param value The value to write
     * @return true if successful, false otherwise
     */
    suspend fun writeCell(sheetId: String, cell: String, value: String): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext false

        val url = "$SHEETS_API_BASE/spreadsheets/$sheetId/values/$cell?valueInputOption=USER_ENTERED"

        val requestBody = JSONObject().apply {
            put("values", JSONArray().put(JSONArray().put(value)))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .put(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                Log.e(TAG, "Failed to write cell $cell: ${response.code} - ${response.body?.string()}")
            } else {
                Log.d(TAG, "Successfully wrote '$value' to $cell")
            }
            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error writing cell $cell", e)
            false
        }
    }

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
