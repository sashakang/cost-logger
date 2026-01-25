# External Integrations

**Analysis Date:** 2026-01-25

## APIs & External Services

**Google APIs:**
- Google Sheets API (v4) - Read/write spreadsheet data for uploads in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
  - SDK/Client: OkHttp in `app/build.gradle.kts` and `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
  - Auth: OAuth 2.0 via Google Sign-In (web client ID + Android client ID in `.env.example` and `README.md`)

## Data Storage

**Databases:**
- Room (SQLite, local only) - Offline queue and entities in `app/src/main/java/com/notificationlogger/data/Storage.kt`
  - Connection: Local app storage (no external connection) in `app/src/main/java/com/notificationlogger/data/Storage.kt`
  - Client: AndroidX Room in `app/build.gradle.kts`

**File Storage:**
- Local app storage only (SharedPreferences) in `app/src/main/java/com/notificationlogger/data/Storage.kt`

**Caching:**
- None detected (no cache providers in `app/build.gradle.kts`)

## Authentication & Identity

**Auth Provider:**
- Google Sign-In / Credential Manager - OAuth sign-in and tokens in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
  - Implementation: GoogleSignInOptions with Sheets scope in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`

## Monitoring & Observability

**Error Tracking:**
- None detected (no SDKs in `app/build.gradle.kts`)

**Logs:**
- Android Logcat via `Log.d/w/e` in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`

## CI/CD & Deployment

**Hosting:**
- Android APK on-device (no deployment service config in `README.md`)

**CI Pipeline:**
- None detected (no CI config referenced in `README.md`)

## Environment Configuration

**Required env vars:**
- `SHA1` - Debug keystore fingerprint in `.env.example` and `README.md`
- `Android_app.ID` - Android OAuth client ID in `.env.example` and `README.md`
- `Web_app_client.ID` - Web OAuth client ID in `.env.example` and `README.md`
- `Web_app_client.secret` - Web OAuth client secret in `.env.example` and `README.md`

**Secrets location:**
- Local `.env` (not committed) as described in `README.md`
- Web client ID constant in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`

## Webhooks & Callbacks

**Incoming:**
- None detected (no webhook endpoints in `app/src/main/AndroidManifest.xml`)

**Outgoing:**
- Google Sheets API HTTPS calls in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`

---

*Integration audit: 2026-01-25*
