package com.notificationlogger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.notificationlogger.data.AppPreferences
import com.notificationlogger.data.NotificationDatabase
import com.notificationlogger.data.TransactionEntry
import com.notificationlogger.sheets.SheetsService
import com.notificationlogger.ui.AppSelectionScreen
import com.notificationlogger.ui.HelpScreen
import com.notificationlogger.ui.MainScreen
import com.notificationlogger.ui.SettingsScreen
import com.notificationlogger.ui.TransactionEntrySheet
import com.notificationlogger.ui.theme.NotificationLoggerTheme
import kotlinx.coroutines.launch

/**
 * Main entry point for the app.
 * Handles navigation between screens and Google Sign-In.
 */
class MainActivity : ComponentActivity() {

    private lateinit var sheetsService: SheetsService
    private lateinit var database: NotificationDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sheetsService = SheetsService.getInstance(applicationContext)
        database = NotificationDatabase.getInstance(applicationContext)

        // Request POST_NOTIFICATIONS permission for Android 13+
        requestNotificationPermission()

        setContent {
            NotificationLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        sheetsService = sheetsService,
                        database = database
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted")
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied")
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}

@Composable
private fun AppNavigation(
    sheetsService: SheetsService,
    database: NotificationDatabase
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }

    // Observe both pending counts and combine them
    val notificationPendingCount by database.notificationDao().observePendingCount()
        .collectAsState(initial = 0)
    val transactionPendingCount by database.transactionDao().observePendingCount()
        .collectAsState(initial = 0)
    val totalPendingCount = notificationPendingCount + transactionPendingCount

    // Track sign-in state
    var isSignedIn by remember { mutableStateOf(sheetsService.isSignedIn()) }
    var userEmail by remember { mutableStateOf(sheetsService.getSignedInEmail()) }
    var isSigningIn by remember { mutableStateOf(false) }

    // Transaction sheet state
    var showTransactionSheet by remember { mutableStateOf(false) }

    // Sign-in launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        val signInResult = sheetsService.handleSignInResult(result.data)
        signInResult.onSuccess { email ->
            isSignedIn = true
            userEmail = email
            Log.d("MainActivity", "Sign-in successful: $email")
        }.onFailure { error ->
            Log.e("MainActivity", "Sign-in failed", error)
        }
    }

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToHelp = {
                    navController.navigate("help")
                },
                onRescanNotifications = {
                    scope.launch {
                        val listener = NotificationListener.getInstance()
                        val message = when {
                            listener == null -> "Notification service not running. Try toggling notification access off/on in Settings."
                            !listener.isServiceConnected() -> "Service not connected. Try toggling notification access off/on in Settings."
                            prefs.whitelistedApps.isEmpty() -> "No apps selected to track. Go to Settings → Select Apps to Track."
                            else -> {
                                val count = listener.scanActiveNotifications()
                                if (count == 0) "No new notifications from tracked apps" else "Logged $count new notifications"
                            }
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                    // Also trigger upload of any pending entries
                    val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>().build()
                    WorkManager.getInstance(context).enqueue(uploadWork)
                },
                onEnterTransaction = {
                    showTransactionSheet = true
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                pendingCount = totalPendingCount,
                isSignedIn = isSignedIn,
                userEmail = userEmail,
                isSigningIn = isSigningIn,
                onSignIn = {
                    isSigningIn = true
                    signInLauncher.launch(sheetsService.getSignInIntent())
                },
                onSignOut = {
                    scope.launch {
                        sheetsService.signOut()
                        isSignedIn = false
                        userEmail = null
                    }
                },
                onNavigateToAppSelection = {
                    navController.navigate("app_selection")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("app_selection") {
            AppSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("help") {
            HelpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }

    // Transaction entry sheet
    if (showTransactionSheet) {
        // Build accounts list: "Cash" first, then app names from whitelisted packages
        val accounts = buildList {
            add("Cash")
            addAll(getAppNamesFromPackages(context, prefs.whitelistedApps))
        }

        TransactionEntrySheet(
            accounts = accounts,
            currencies = prefs.recentCurrencies,
            categories = prefs.categories,
            onDismiss = {
                showTransactionSheet = false
            },
            onSubmit = { account, amount, currency, category, comment ->
                scope.launch {
                    try {
                        // Update recency lists
                        prefs.updateCurrencyRecency(currency)
                        prefs.updateCategoryRecency(category)

                        // Insert transaction to database
                        val transaction = TransactionEntry(
                            account = account,
                            amount = amount,
                            currency = currency,
                            category = category,
                            comment = comment,
                            uploaded = false
                        )
                        database.transactionDao().insert(transaction)

                        // Schedule upload work
                        val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>().build()
                        WorkManager.getInstance(context).enqueue(uploadWork)

                        // Show success feedback
                        Toast.makeText(context, "Transaction saved", Toast.LENGTH_SHORT).show()

                        // Dismiss sheet
                        showTransactionSheet = false
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to save transaction", e)
                        Toast.makeText(context, "Failed to save transaction", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

/**
 * Helper to get app display names from package names.
 */
private fun getAppNamesFromPackages(context: android.content.Context, packages: Set<String>): List<String> {
    val pm = context.packageManager
    return packages.mapNotNull { pkg ->
        try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }.sorted()
}
