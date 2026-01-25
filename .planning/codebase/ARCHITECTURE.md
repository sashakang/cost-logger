# Architecture

**Analysis Date:** 2026-01-25

## Pattern Overview

**Overall:** Single-module Android app with Compose UI + service-oriented background processing (`app/src/main/java/com/notificationlogger/MainActivity.kt`).

**Key Characteristics:**
- UI is stateful Compose screens with navigation hosted in a single activity (`app/src/main/java/com/notificationlogger/MainActivity.kt`).
- Background work is decoupled via WorkManager and a notification listener service (`app/src/main/java/com/notificationlogger/UploadWorker.kt`, `app/src/main/java/com/notificationlogger/NotificationListener.kt`).
- Local persistence + preferences are accessed directly by UI/services (no repository layer) (`app/src/main/java/com/notificationlogger/data/Storage.kt`).

## Layers

**UI & Navigation:**
- Purpose: Compose screens and in-app navigation.
- Location: `app/src/main/java/com/notificationlogger/ui/`
- Contains: Screens, dialogs, sheets, Compose UI utilities.
- Depends on: Preferences/database services, Sheets auth state, navigation callbacks (`app/src/main/java/com/notificationlogger/MainActivity.kt`).
- Used by: Activities (`app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/CategorySelectionActivity.kt`).

**Activity/Entry Layer:**
- Purpose: App entrypoints and screen host lifecycle.
- Location: `app/src/main/java/com/notificationlogger/`
- Contains: `MainActivity`, `CategorySelectionActivity`.
- Depends on: `SheetsService`, `NotificationDatabase`, `AppPreferences` (`app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`, `app/src/main/java/com/notificationlogger/data/Storage.kt`).
- Used by: Android launcher and notification intents (`app/src/main/AndroidManifest.xml`).

**Background Services:**
- Purpose: Notification interception and queued uploads.
- Location: `app/src/main/java/com/notificationlogger/`
- Contains: `NotificationListener`, `UploadWorker`.
- Depends on: Room database, preferences, Sheets integration (`app/src/main/java/com/notificationlogger/data/Storage.kt`, `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`).
- Used by: Android system (NotificationListenerService) and WorkManager (`app/src/main/AndroidManifest.xml`, `app/src/main/java/com/notificationlogger/NotificationListener.kt`).

**Data & Persistence:**
- Purpose: Local queueing and user settings.
- Location: `app/src/main/java/com/notificationlogger/data/`
- Contains: Room entities/DAOs, database, preferences, result types (`app/src/main/java/com/notificationlogger/data/Models.kt`, `app/src/main/java/com/notificationlogger/data/Storage.kt`).
- Depends on: Android Room/SharedPreferences APIs.
- Used by: UI, services, workers (`app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/NotificationListener.kt`).

**External Integration:**
- Purpose: Google Sign-In + Sheets API.
- Location: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
- Contains: OAuth, token retrieval, HTTP requests, Sheets read/write.
- Depends on: Google Play Services and OkHttp (`app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`).
- Used by: Activities and workers (`app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`).

## Data Flow

**Notification capture → sheet upload → category feedback:**
1. Android posts a notification; service intercepts it and converts to `NotificationEntry` (`app/src/main/java/com/notificationlogger/NotificationListener.kt`).
2. Entry is inserted into Room and upload work is enqueued (`app/src/main/java/com/notificationlogger/NotificationListener.kt`, `app/src/main/java/com/notificationlogger/data/Storage.kt`).
3. `UploadWorker` reads pending entries, uploads to Sheets, and marks them uploaded (`app/src/main/java/com/notificationlogger/UploadWorker.kt`, `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`).
4. Worker reads category column and posts a local notification with category action (`app/src/main/java/com/notificationlogger/UploadWorker.kt`).
5. Tapping the category notification launches category selection activity and writes category/comment back (`app/src/main/java/com/notificationlogger/CategorySelectionActivity.kt`).

**Manual transaction entry → sheet upload:**
1. User opens the transaction sheet from main screen (`app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/ui/TransactionEntrySheet.kt`).
2. Transaction is stored in Room and upload work is scheduled (`app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/data/Storage.kt`).
3. Worker appends transactions and writes category/comment to sheets (`app/src/main/java/com/notificationlogger/UploadWorker.kt`).

**Settings & preferences:**
1. Settings UI reads/writes sheet configuration, categories, currencies, and whitelisted apps (`app/src/main/java/com/notificationlogger/ui/SettingsScreen.kt`).
2. Preferences are persisted via `AppPreferences` (`app/src/main/java/com/notificationlogger/data/Storage.kt`).

**State Management:**
- Compose `remember` state + `collectAsState` for DB counts (`app/src/main/java/com/notificationlogger/MainActivity.kt`).
- SharedPreferences for settings and recency lists (`app/src/main/java/com/notificationlogger/data/Storage.kt`).

## Key Abstractions

**SheetsService:**
- Purpose: Auth, token handling, and Sheets API requests.
- Examples: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
- Pattern: Singleton service with coroutine-based IO.

**NotificationDatabase + DAOs:**
- Purpose: Local queue for notifications/transactions.
- Examples: `app/src/main/java/com/notificationlogger/data/Storage.kt`
- Pattern: Room database with DAO methods for pending uploads.

**NotificationListener:**
- Purpose: System-level capture of notifications.
- Examples: `app/src/main/java/com/notificationlogger/NotificationListener.kt`
- Pattern: Android `NotificationListenerService` with WorkManager enqueue.

**UploadWorker:**
- Purpose: Background upload + category notifications.
- Examples: `app/src/main/java/com/notificationlogger/UploadWorker.kt`
- Pattern: WorkManager CoroutineWorker with retry logic.

**AppPreferences:**
- Purpose: Settings and whitelists.
- Examples: `app/src/main/java/com/notificationlogger/data/Storage.kt`
- Pattern: SharedPreferences wrapper with singleton access.

## Entry Points

**Application:**
- Location: `app/src/main/java/com/notificationlogger/NotificationLoggerApp.kt`
- Triggers: Android app process start (`app/src/main/AndroidManifest.xml`).
- Responsibilities: Initialize singletons, create notification channel.

**MainActivity:**
- Location: `app/src/main/java/com/notificationlogger/MainActivity.kt`
- Triggers: App launcher (`app/src/main/AndroidManifest.xml`).
- Responsibilities: Navigation host, sign-in flow, transaction entry, rescan notifications.

**CategorySelectionActivity:**
- Location: `app/src/main/java/com/notificationlogger/CategorySelectionActivity.kt`
- Triggers: Tapping category notification (`app/src/main/java/com/notificationlogger/UploadWorker.kt`).
- Responsibilities: Category/comment writeback to Sheets and advancing to next notification.

**NotificationListenerService:**
- Location: `app/src/main/java/com/notificationlogger/NotificationListener.kt`
- Triggers: Android notification listener binding (`app/src/main/AndroidManifest.xml`).
- Responsibilities: Capture notifications, persist, schedule uploads.

**UploadWorker:**
- Location: `app/src/main/java/com/notificationlogger/UploadWorker.kt`
- Triggers: WorkManager enqueue from listener and UI actions (`app/src/main/java/com/notificationlogger/NotificationListener.kt`, `app/src/main/java/com/notificationlogger/MainActivity.kt`).
- Responsibilities: Upload pending entries/transactions, notify user.

## Error Handling

**Strategy:** Guarded IO with try/catch and non-fatal logging (`app/src/main/java/com/notificationlogger/UploadWorker.kt`).

**Patterns:**
- Service-level try/catch to prevent crashes (`app/src/main/java/com/notificationlogger/NotificationListener.kt`).
- Worker retry/failure decisions based on upload results (`app/src/main/java/com/notificationlogger/UploadWorker.kt`).

## Cross-Cutting Concerns

**Logging:** Android `Log.d/w/e` across UI, services, and integration (`app/src/main/java/com/notificationlogger/MainActivity.kt`).
**Validation:** Entry validation via guards and `require` in models (`app/src/main/java/com/notificationlogger/data/Models.kt`).
**Authentication:** Google Sign-In checks and token access in Sheets integration (`app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`).

---

*Architecture analysis: 2026-01-25*
