package com.notificationlogger

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
