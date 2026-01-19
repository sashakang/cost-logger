package com.notificationlogger.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.notificationlogger.NotificationListener

/**
 * Main screen with primary action buttons.
 * Simple launcher screen that delegates to specific features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onRescanNotifications: () -> Unit,
    onEnterTransaction: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val context = LocalContext.current
    var isNotificationAccessEnabled by remember { mutableStateOf(NotificationListener.isEnabled(context)) }

    // Refresh notification access status when screen resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isNotificationAccessEnabled = NotificationListener.isEnabled(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Logger") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Help"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning banner when notification access is disabled
            if (!isNotificationAccessEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notification access disabled",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "If you see 'Restricted setting', enable it manually:\n" +
                                    "Settings → Apps → Notification Logger → Notification access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    try {
                                        // Try to open notification listener settings
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    } catch (e: Exception) {
                                        // Fallback: open app settings
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Settings")
                            }
                            TextButton(
                                onClick = {
                                    // Open app's own settings page
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("App Settings")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onRescanNotifications,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Re-scan Notifications")
            }

            Button(
                onClick = onEnterTransaction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enter Transaction")
            }
        }
    }
}
