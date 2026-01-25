# Technology Stack

**Analysis Date:** 2026-01-25

## Languages

**Primary:**
- Kotlin 1.9.22 - Android app source in `app/src/main/java/com/notificationlogger/` and build scripts in `app/build.gradle.kts`

**Secondary:**
- XML - Android manifest and resources in `app/src/main/AndroidManifest.xml` and `app/src/main/res/values/`
- Gradle Kotlin DSL - Build configuration in `build.gradle.kts` and `settings.gradle.kts`

## Runtime

**Environment:**
- Android (minSdk 26, target/compile 34) - `app/build.gradle.kts`

**Package Manager:**
- Gradle 8.13 (wrapper) - `gradle/wrapper/gradle-wrapper.properties`
- Lockfile: missing (no `gradle.lockfile` alongside `build.gradle.kts`)

## Frameworks

**Core:**
- Android SDK / AndroidX - App runtime and platform APIs in `app/build.gradle.kts`
- Jetpack Compose - UI framework in `app/build.gradle.kts`
- Material 3 - UI components in `app/build.gradle.kts`
- Navigation Compose - Screen navigation in `app/build.gradle.kts`

**Testing:**
- JUnit 4.13.2 - Unit tests in `app/build.gradle.kts`
- AndroidX Test JUnit / Espresso - Instrumentation tests in `app/build.gradle.kts`
- Compose UI Test JUnit4 - UI tests in `app/build.gradle.kts`

**Build/Dev:**
- Android Gradle Plugin 8.13.2 - Build tooling in `build.gradle.kts`
- Kotlin Android Plugin 1.9.22 - Kotlin build tooling in `build.gradle.kts`
- KSP 1.9.22-1.0.17 - Annotation processing for Room in `build.gradle.kts` and `app/build.gradle.kts`
- Compose Compiler Extension 1.5.10 - Compose compilation in `app/build.gradle.kts`

## Key Dependencies

**Critical:**
- Room 2.6.1 - Local persistence and offline queue in `app/build.gradle.kts` and `app/src/main/java/com/notificationlogger/data/Storage.kt`
- WorkManager 2.9.0 - Background uploads in `app/build.gradle.kts` and `app/src/main/java/com/notificationlogger/UploadWorker.kt`
- OkHttp 4.12.0 - Google Sheets REST calls in `app/build.gradle.kts` and `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
- Google Play Services Auth 21.0.0 - Google sign-in tokens in `app/build.gradle.kts` and `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`

**Infrastructure:**
- AndroidX Credentials 1.3.0 + Google Identity 1.1.0 - Google account auth flows in `app/build.gradle.kts`
- Kotlin Coroutines 1.7.3 - Async work in `app/build.gradle.kts` and `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
- AndroidX Lifecycle 2.7.0 - Lifecycle-aware UI in `app/build.gradle.kts`
- org.json 20231013 - JSON parsing for Sheets API in `app/build.gradle.kts` and `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`

## Configuration

**Environment:**
- OAuth credentials captured in `.env` (template in `.env.example`) and referenced in setup in `README.md`
- Web client ID configured in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`

**Build:**
- Root build config: `build.gradle.kts`
- App build config: `app/build.gradle.kts`
- Project settings: `settings.gradle.kts`
- Gradle properties: `gradle.properties`
- ProGuard rules: `app/proguard-rules.pro`
- Android manifest: `app/src/main/AndroidManifest.xml`

## Platform Requirements

**Development:**
- Android Studio + Android SDK 34 (compileSdk) - `app/build.gradle.kts`
- JDK 17 / Kotlin JVM target 17 - `app/build.gradle.kts`
- Gradle wrapper 8.13 - `gradle/wrapper/gradle-wrapper.properties`

**Production:**
- Android 8.0+ (minSdk 26) - `app/build.gradle.kts` and `README.md`

---

*Stack analysis: 2026-01-25*
