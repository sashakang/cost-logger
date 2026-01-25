# Android Notification to Google Sheets App

## Overview
A Kotlin Android app that intercepts device notifications, filters by whitelisted apps, and records them to Google Sheets in real-time via OAuth 2.0.

**Package name**: `com.notificationlogger`
**Data captured**: App name, title, text, timestamp
**minSdk**: 26, **targetSdk**: 34

---

## Consensus-Approved Architecture

### Project Structure (Simplified - 7 files)

```
app/src/main/java/com/notificationlogger/
├── MainActivity.kt                   # Entry point, permissions, navigation
├── NotificationListener.kt           # NotificationListenerService (core)
├── data/
│   ├── Models.kt                     # NotificationEntry + Room entity
│   └── Storage.kt                    # Preferences + DAO + Database
├── sheets/
│   └── SheetsService.kt              # Auth + Upload combined
└── ui/
    ├── MainScreen.kt                 # Status + Settings combined
    └── AppSelectionScreen.kt         # Whitelist management
```

**Rationale**: Single-purpose utility app → architecture matches simplicity.

---

## Phase 0: Development Environment Setup

### Install Android Studio
1. Download from https://developer.android.com/studio
2. Run installer, accept defaults
3. Setup Wizard downloads: Android SDK, Platform-Tools, Emulator

### Configure SDK
- **SDK Platforms**: Android 14 (API 34)
- **SDK Tools**: Build-Tools, Emulator, Platform-Tools

### Device for Testing
**Physical Device (Recommended)**:
1. Settings → About phone → Tap "Build number" 7x for Developer Options
2. Developer Options → Enable "USB debugging"
3. Connect via USB, accept prompt

**Emulator**: Device Manager → Create Device → Pixel 6 → API 34

---

## Implementation Order

### Phase 1: Project Setup
1. Create Android project (minSdk 26, targetSdk 34, Kotlin, Compose)
2. Add dependencies to build.gradle.kts
3. Configure AndroidManifest.xml

### Phase 2: Data Layer
4. Implement `Models.kt` (NotificationEntry data class + Room entity)
5. Implement `Storage.kt` (AppPreferences + NotificationDao + Database)

### Phase 3: Core Service
6. Implement `NotificationListener.kt` with whitelist filtering
7. Test notification interception with Logcat

### Phase 4: Google Sheets Integration
8. Set up Google Cloud Console (project, APIs, OAuth credentials)
9. Implement `SheetsService.kt` (auth + upload combined)
10. Test authentication and upload flow

### Phase 5: Background Upload Reliability
11. Add WorkManager for queue processing
12. Implement network connectivity observer
13. Test offline → online queue flush

### Phase 6: UI
14. Implement `MainScreen.kt` (status + settings + privacy consent)
15. Implement `AppSelectionScreen.kt` (whitelist)
16. Add navigation between screens

### Phase 7: Polish
17. Handle edge cases (service restart, token refresh)
18. Add proper error handling throughout
19. Testing and bug fixes

---

## AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Package visibility for app list (NO QUERY_ALL_PACKAGES) -->
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <application
        android:name=".NotificationLoggerApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.NotificationLogger">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- NotificationListenerService (system-bound, no foreground needed) -->
        <service
            android:name=".NotificationListener"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

    </application>
</manifest>
```

**Key Change**: Removed `QUERY_ALL_PACKAGES` (Play Store rejection risk) and `foregroundServiceType` (not needed for NotificationListenerService).

---

## Dependencies

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.notificationlogger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.notificationlogger"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "0.3.3.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room (offline queue)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager (reliable background uploads)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Google Authentication
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## Google Cloud Console Setup

1. Create project at console.cloud.google.com
2. Enable **Google Sheets API** and **Google Drive API**
3. Configure OAuth consent screen:
   - User type: External
   - Scopes: `spreadsheets`, `drive.file`
   - Add test users during development
4. Create OAuth 2.0 credentials (Android type):
   - Package: `com.notificationlogger`
   - SHA-1: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`
5. Also create **Web client** credentials (needed for Credential Manager)
6. Note the Web Client ID for use in code

---

## Data Flow

```
Notification Posted
       ↓
NotificationListener.onNotificationPosted()
       ↓
Check whitelist → Not whitelisted? → Ignore
       ↓
Extract: appName, title, text, timestamp
       ↓
Room DB insert (NotificationEntry)
       ↓
Trigger WorkManager upload job
       ↓
WorkManager checks network → No? → Wait with constraints
       ↓
SheetsService.getAccessToken()
       ↓
SheetsService.upload()
POST /v4/spreadsheets/{id}/values/Sheet1:append
       ↓
Success → Delete from Room
Failure → Retry with backoff
```

---

## Critical Implementation Notes

### NotificationListenerService
- System-bound service (no foreground notification needed)
- User must manually enable in Settings → Notification access
- Handle `onListenerDisconnected()` - user revoked permission
- Wrap processing in try-catch (service must remain stable)

### OAuth Token Management
- Access tokens expire ~1 hour
- Credential Manager handles refresh automatically
- Store sheet ID in SharedPreferences (not encrypted - user provides it)

### WorkManager for Uploads
- Guarantees execution despite Doze mode / battery optimization
- Use `NetworkType.CONNECTED` constraint
- Implement exponential backoff for failures

### Privacy Requirements
- Show consent dialog before enabling listener
- Warn: "This app reads notification content including private messages"
- Exclude sensitive apps by default (banking, authenticators)

### Play Store Compliance
- NO `QUERY_ALL_PACKAGES` - use package visibility queries instead
- Privacy policy required for notification access
- Rate limit: 100 Sheets API requests per 100 seconds per user

---

## Kotlin Patterns to Use

### Sealed Class for Results
```kotlin
sealed interface UploadResult {
    data class Success(val rowsAdded: Int) : UploadResult
    data class Failure(val error: Throwable, val retryable: Boolean) : UploadResult
}
```

### Extension Function for Notification Parsing
```kotlin
fun StatusBarNotification.toEntry(): NotificationEntry? {
    val extras = notification.extras ?: return null
    return NotificationEntry(
        appName = packageName,
        title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    )
}
```

### Flow for Reactive Queue Monitoring
```kotlin
@Query("SELECT COUNT(*) FROM notifications WHERE uploaded = 0")
fun observePendingCount(): Flow<Int>
```

---

## Files to Create

| File | LOC Est. | Purpose |
|------|----------|---------|
| `MainActivity.kt` | 80 | Entry, permissions, navigation |
| `NotificationListener.kt` | 120 | Core service |
| `data/Models.kt` | 40 | Data classes + Room entity |
| `data/Storage.kt` | 100 | Preferences + DAO + Database |
| `sheets/SheetsService.kt` | 150 | Auth + upload combined |
| `ui/MainScreen.kt` | 150 | Status + settings UI |
| `ui/AppSelectionScreen.kt` | 120 | Whitelist management |
| `UploadWorker.kt` | 60 | WorkManager job |
| **Total** | **~820** | |

---

## Testing Checklist

- [ ] NotificationListenerService receives notifications
- [ ] Whitelist filtering works correctly
- [ ] Privacy consent dialog shows on first launch
- [ ] Offline queue stores entries when offline
- [ ] Google Sign-In flow completes
- [ ] WorkManager processes queue when online
- [ ] Sheet append API succeeds
- [ ] Service survives app close
- [ ] Handle notification listener permission revocation
