package com.notificationlogger.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notificationlogger.NotificationListener
import com.notificationlogger.data.AppPreferences

/** App version - update this when releasing new versions */
const val APP_VERSION = "0.3.1.0"

/**
 * Main screen combining status, settings, and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    pendingCount: Int,
    isSignedIn: Boolean,
    userEmail: String?,
    isSigningIn: Boolean,
    onNavigateToAppSelection: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRescanNotifications: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }

    var sheetId by remember { mutableStateOf(prefs.sheetId ?: "") }
    var isNotificationAccessEnabled by remember { mutableStateOf(NotificationListener.isEnabled(context)) }
    var showPrivacyDialog by remember { mutableStateOf(!prefs.hasAcceptedPrivacy) }

    // Refresh notification access status when screen is visible
    LaunchedEffect(Unit) {
        isNotificationAccessEnabled = NotificationListener.isEnabled(context)
    }

    // Privacy consent dialog
    if (showPrivacyDialog) {
        PrivacyConsentDialog(
            onAccept = {
                prefs.hasAcceptedPrivacy = true
                showPrivacyDialog = false
            },
            onDecline = {
                showPrivacyDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Logger v$APP_VERSION") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(
                isNotificationAccessEnabled = isNotificationAccessEnabled,
                isSignedIn = isSignedIn,
                userEmail = userEmail,
                pendingCount = pendingCount,
                onEnableNotificationAccess = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onRescanNotifications = onRescanNotifications
            )

            // Google Sign In Card
            SignInCard(
                isSignedIn = isSignedIn,
                userEmail = userEmail,
                isSigningIn = isSigningIn,
                onSignIn = onSignIn,
                onSignOut = onSignOut
            )

            // Sheet ID Configuration
            SheetIdCard(
                sheetId = sheetId,
                onSheetIdChange = { newId ->
                    sheetId = newId
                    prefs.sheetId = newId.ifBlank { null }
                }
            )

            // Categories Configuration
            CategoriesCard(
                categories = prefs.categories,
                onCategoriesChange = { prefs.categories = it }
            )

            // App Selection Button
            OutlinedButton(
                onClick = onNavigateToAppSelection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Apps to Track (${prefs.whitelistedApps.size} selected)")
            }
        }
    }
}

@Composable
private fun CategoriesCard(
    categories: List<String>,
    onCategoriesChange: (List<String>) -> Unit
) {
    var categoriesText by remember { mutableStateOf(categories.joinToString(", ")) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = categoriesText,
                onValueChange = { newText ->
                    categoriesText = newText
                    val newCategories = newText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    onCategoriesChange(newCategories)
                },
                label = { Text("Categories (comma-separated)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Text(
                text = "${categories.size} categories configured",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusCard(
    isNotificationAccessEnabled: Boolean,
    isSignedIn: Boolean,
    userEmail: String?,
    pendingCount: Int,
    onEnableNotificationAccess: () -> Unit,
    onRescanNotifications: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Notification Access Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notification Access")
                if (isNotificationAccessEnabled) {
                    Text("Enabled", color = MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = onEnableNotificationAccess) {
                        Text("Enable")
                    }
                }
            }

            // Google Account Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Google Account")
                Text(
                    text = if (isSignedIn) userEmail ?: "Connected" else "Not signed in",
                    color = if (isSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            // Pending Uploads
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pending Uploads")
                Text(
                    text = pendingCount.toString(),
                    color = if (pendingCount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Re-scan Active Notifications Button
            Button(
                onClick = onRescanNotifications,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Re-scan Active Notifications")
            }
        }
    }
}

@Composable
private fun SignInCard(
    isSignedIn: Boolean,
    userEmail: String?,
    isSigningIn: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Google Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isSignedIn) {
                Text("Signed in as: ${userEmail ?: "Unknown"}")
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out")
                }
            } else {
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSigningIn
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Sign in with Google")
                }
            }
        }
    }
}

@Composable
private fun SheetIdCard(
    sheetId: String,
    onSheetIdChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Google Sheet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = sheetId,
                onValueChange = onSheetIdChange,
                label = { Text("Sheet ID") },
                placeholder = { Text("Enter your Google Sheet ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Find the ID in your sheet URL:\ndocs.google.com/spreadsheets/d/{SHEET_ID}/edit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Don't dismiss on outside click */ },
        title = { Text("Privacy Notice") },
        text = {
            Text(
                "This app reads notification content including private messages. " +
                "Data is sent to your Google Sheet. Only notifications from apps you " +
                "whitelist will be recorded.\n\n" +
                "Do you agree to proceed?"
            )
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("I Understand")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Cancel")
            }
        }
    )
}
