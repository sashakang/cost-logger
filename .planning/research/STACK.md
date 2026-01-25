# Stack Research

**Domain:** Android expense tracker with Google Sheets sync and hybrid categorization
**Researched:** 2026-01-25
**Confidence:** MEDIUM

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | 2.3.0 | Primary language | Latest stable Kotlin release with current tooling support; aligns with modern Android development and Compose-first codebases. Confidence: HIGH. |
| Jetpack Compose (BOM) | 2026.01.00 | UI toolkit and version alignment | Compose BOM keeps UI libraries compatible without pinning each artifact; current BOM is the safest way to stay aligned. Confidence: HIGH. |
| AndroidX Room | 2.8.4 | Local database for transactions, rules, and corrections | Battle-tested persistence layer with compile-time schema validation; ideal for offline-first capture before Sheets sync. Confidence: HIGH. |
| AndroidX WorkManager | 2.11.0 | Reliable background uploads to Sheets | WorkManager handles deferrable, retryable uploads and respects OS background limits. Confidence: HIGH. |
| Google Sheets API client | v4-rev20251110-2.0.0 | Sheets read/write access | Official Sheets v4 client versioned by API revision; supports append and batch updates needed for hybrid categorization workflows. Confidence: HIGH. |
| Google Play services auth | 21.5.0 | User sign-in and OAuth tokens | Standard Google Sign-In path for Android; integrates with Google APIs and reduces OAuth edge cases. Confidence: HIGH. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| androidx.datastore:datastore-preferences | 1.2.0 | Persist small rule settings and user overrides | Use for lightweight preferences and rule toggles; avoid alpha-only 1.3.x unless testing new APIs. Confidence: HIGH. |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.10.2 | Async work and background tasks | Use for all IO and rule evaluation pipelines; integrates cleanly with WorkManager and Room. Confidence: HIGH. |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.10.0 | Rule schema serialization | Use for storing rule definitions or export/import of category rules. Confidence: HIGH. |
| org.jetbrains.kotlinx:kotlinx-datetime | 0.7.1 | Time handling for transactions | Use to normalize transaction timestamps and timezone-safe reporting. Confidence: MEDIUM (latest stable; compat variant exists). |
| com.google.api-client:google-api-client-android | 2.8.1 | Google API client base | Required by Sheets client; provides Android-friendly transport and auth plumbing. Confidence: HIGH. |
| com.google.http-client:google-http-client-android | 2.1.0 | HTTP transport for Google APIs | Recommended transport for Android with the Google API client. Confidence: HIGH. |
| com.google.http-client:google-http-client-gson | 2.1.0 | JSON parsing for Google APIs | Use with Sheets client if you prefer Gson over Jackson; keeps dependencies minimal. Confidence: HIGH. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Android Studio (current stable) | IDE and Gradle integration | Align Android Studio and Android Gradle Plugin versions; use Kotlin K2 mode for improved diagnostics. |
| Gradle (via AGP) | Build and dependency management | Use the AGP version bundled with Android Studio stable to avoid compatibility issues. |

## Installation

```gradle
// Core
implementation(platform("androidx.compose:compose-bom:2026.01.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.room:room-runtime:2.8.4")
ksp("androidx.room:room-compiler:2.8.4")
implementation("androidx.work:work-runtime-ktx:2.11.0")
implementation("com.google.android.gms:play-services-auth:21.5.0")
implementation("com.google.apis:google-api-services-sheets:v4-rev20251110-2.0.0")

// Supporting
implementation("androidx.datastore:datastore-preferences:1.2.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
implementation("com.google.api-client:google-api-client-android:2.8.1")
implementation("com.google.http-client:google-http-client-android:2.1.0")
implementation("com.google.http-client:google-http-client-gson:2.1.0")
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Google Sheets API client | Apps Script Web App + HTTP | Use if you need custom server-side rule logic, but accept additional maintenance and deployment steps. |
| Room | SQLDelight | Use if you need Kotlin Multiplatform sharing or compile-time SQL on multiple platforms. |
| WorkManager | ForegroundService + manual retry | Use only for immediate, user-visible uploads that must run in the foreground. |
| DataStore | SharedPreferences | Use SharedPreferences only for very small, non-critical flags; avoid for rules or sync state. |
| Play services auth | AppAuth (OAuth via Custom Tabs) | Use if you must support devices without Google Play services. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| AsyncTask | Deprecated and unreliable for background work | Coroutines + WorkManager |
| SharedPreferences for rule sets | Easy to corrupt and lacks transactional updates | DataStore or Room |
| API keys or public tokens for Sheets | Not secure and often blocked by API scopes | OAuth via Google Sign-In |
| Direct Sheets formula writes from app | Hard to maintain and brittle across user edits | Use sheet-side rules and keep app sending clean raw data |

## Stack Patterns by Variant

**If you need robust hybrid categorization (app + sheet rules):**
- Use Room for canonical transaction rows and a local category override table
- Use DataStore for user-facing rule toggles and rule set metadata
- Because local overrides prevent sheet rule changes from breaking app-corrected categories

**If you must support devices without Google Play services:**
- Use AppAuth (OAuth via Custom Tabs) instead of Play services auth
- Because Google Sign-In requires Play services on-device

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| com.google.apis:google-api-services-sheets:v4-rev20251110-2.0.0 | com.google.api-client:google-api-client-android:2.8.1 | Client library expects Google API client 2.x; keep both on current 2.x line. Confidence: MEDIUM. |
| com.google.api-client:google-api-client-android:2.8.1 | com.google.http-client:google-http-client-android:2.1.0 | HTTP client 2.x aligns with API client 2.x; avoid mixing 1.x and 2.x. Confidence: MEDIUM. |

## Sources

- https://kotlinlang.org/docs/releases.html — Kotlin 2.3.0 release (HIGH)
- https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml — Compose BOM 2026.01.00 (HIGH)
- https://dl.google.com/dl/android/maven2/androidx/room/room-runtime/maven-metadata.xml — Room 2.8.4 (HIGH)
- https://dl.google.com/dl/android/maven2/androidx/work/work-runtime-ktx/maven-metadata.xml — WorkManager 2.11.0 (HIGH)
- https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml — DataStore 1.2.0 stable (HIGH)
- https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-auth/maven-metadata.xml — Play services auth 21.5.0 (HIGH)
- https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/maven-metadata.xml — Coroutines 1.10.2 (HIGH)
- https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml — Serialization 1.10.0 (HIGH)
- https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-datetime/maven-metadata.xml — Datetime 0.7.1 (MEDIUM)
- https://repo1.maven.org/maven2/com/google/apis/google-api-services-sheets/maven-metadata.xml — Sheets client v4-rev20251110-2.0.0 (HIGH)
- https://repo1.maven.org/maven2/com/google/api-client/google-api-client-android/maven-metadata.xml — Google API client 2.8.1 (HIGH)
- https://repo1.maven.org/maven2/com/google/http-client/google-http-client-android/maven-metadata.xml — Google HTTP client 2.1.0 (HIGH)
- https://repo1.maven.org/maven2/com/google/http-client/google-http-client-gson/maven-metadata.xml — HTTP client Gson 2.1.0 (HIGH)

---
*Stack research for: Android expense tracking with Google Sheets sync*
*Researched: 2026-01-25*
