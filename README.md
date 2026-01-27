# Notification Logger

An Android app that captures device notifications from selected apps and logs them to Google Sheets in real-time.

## Features

- **Selective Notification Capture**: Whitelist specific apps to monitor
- **Google Sheets Integration**: Automatically logs notifications via OAuth 2.0
- **Offline Support**: Queues notifications locally when offline, uploads when connected
- **Category Review**: Approve auto-selected categories while optionally saving comments
- **Privacy-Focused**: No `QUERY_ALL_PACKAGES` permission, user controls which apps to monitor

## Requirements

- Android 8.0+ (API 26)
- Google account for Sheets integration

## Setup

### 1. Clone and Open

```bash
git clone <repository-url>
```

Open the project in Android Studio.

### 2. Google Cloud Console Setup

1. Create a project at [console.cloud.google.com](https://console.cloud.google.com)
2. Enable **Google Sheets API** and **Google Drive API**
3. Configure OAuth consent screen:
   - User type: External
   - Scopes: `spreadsheets`, `drive.file`
4. Create OAuth 2.0 credentials:
   - **Android client**: Use your debug keystore SHA-1
   - **Web client**: Required for Credential Manager

Get your debug SHA-1:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

### 3. Configure Credentials

Copy `.env.example` to `.env` and fill in your OAuth credentials:

```
SHA1: "YOUR_SHA1_FINGERPRINT"
Android_app.ID: "YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com"
Web_app_client.ID: "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
Web_app_client.secret: "YOUR_WEB_CLIENT_SECRET"
```

Update the Web Client ID in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`:

```kotlin
const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```

### 4. Build and Run

```bash
./gradlew assembleDebug
```

Or run directly from Android Studio.

## Usage

1. **Grant Permissions**: Enable notification access in Settings when prompted
2. **Sign In**: Authenticate with your Google account
3. **Configure Sheet**: Enter your Google Sheets ID
4. **Select Apps**: Choose which apps to monitor
5. **Done**: Notifications will be logged automatically

## Project Structure

```
app/src/main/java/com/notificationlogger/
├── MainActivity.kt              # Entry point, permissions, navigation
├── NotificationListener.kt      # NotificationListenerService
├── NotificationLoggerApp.kt     # Application class
├── UploadWorker.kt              # WorkManager background uploads
├── data/
│   ├── Models.kt                # Data classes + Room entity
│   └── Storage.kt               # Preferences + DAO + Database
├── sheets/
│   └── SheetsService.kt         # Google Auth + Sheets API
└── ui/
    ├── MainScreen.kt            # Status and settings UI
    ├── AppSelectionScreen.kt    # App whitelist management
    └── theme/                   # Material 3 theming
```

## Tech Stack

- **Kotlin** with Coroutines & Flow
- **Jetpack Compose** for UI
- **Room** for local persistence
- **WorkManager** for reliable background uploads
- **Credential Manager** for Google Sign-In
- **OkHttp** for networking

## Privacy

This app requires notification access permission, which allows reading notification content. Users must explicitly grant this permission and select which apps to monitor. No data is collected by the app developer - all data goes directly to the user's own Google Sheet.

## License

MIT
