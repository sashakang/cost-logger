package com.notificationlogger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.notificationlogger.data.NotificationDatabase
import com.notificationlogger.sheets.SheetsService
import com.notificationlogger.ui.AppSelectionScreen
import com.notificationlogger.ui.MainScreen
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

    // Observe pending count
    val pendingCount by database.notificationDao().observePendingCount()
        .collectAsState(initial = 0)

    // Track sign-in state
    var isSignedIn by remember { mutableStateOf(sheetsService.isSignedIn()) }
    var userEmail by remember { mutableStateOf(sheetsService.getSignedInEmail()) }
    var isSigningIn by remember { mutableStateOf(false) }

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
                pendingCount = pendingCount,
                isSignedIn = isSignedIn,
                userEmail = userEmail,
                isSigningIn = isSigningIn,
                onNavigateToAppSelection = {
                    navController.navigate("app_selection")
                },
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
    }
}
