# Codebase Structure

**Analysis Date:** 2026-01-25

## Directory Layout

```
[project-root]/
├── app/                       # Android app module (`app/build.gradle.kts`)
├── docs/                      # Project docs (`docs/DEV_LOG.md`)
├── gradle/                    # Gradle wrapper files (`gradle/wrapper/gradle-wrapper.properties`)
├── build.gradle.kts           # Root build config (`build.gradle.kts`)
├── settings.gradle.kts        # Module settings (`settings.gradle.kts`)
└── README.md                  # Setup/usage docs (`README.md`)
```

## Directory Purposes

**app/**
- Purpose: Main Android application module.
- Contains: Sources, resources, manifest, assets.
- Key files: `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`

**app/src/main/java/com/notificationlogger/**
- Purpose: Kotlin application code.
- Contains: Activities, services, workers, data, UI, integrations.
- Key files: `app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/NotificationListener.kt`

**app/src/main/java/com/notificationlogger/data/**
- Purpose: Persistence and preferences.
- Contains: Room entities, DAOs, database, SharedPreferences wrapper.
- Key files: `app/src/main/java/com/notificationlogger/data/Storage.kt`, `app/src/main/java/com/notificationlogger/data/Models.kt`

**app/src/main/java/com/notificationlogger/ui/**
- Purpose: Compose UI screens.
- Contains: Screen composables, sheets, UI helpers.
- Key files: `app/src/main/java/com/notificationlogger/ui/MainScreen.kt`, `app/src/main/java/com/notificationlogger/ui/SettingsScreen.kt`

**app/src/main/java/com/notificationlogger/sheets/**
- Purpose: Google Sheets auth and API access.
- Contains: Sheets client implementation.
- Key files: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`

**app/src/main/res/**
- Purpose: Android resources (drawables, strings, themes).
- Contains: XML resources for UI and app metadata.
- Key files: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`

**app/src/main/assets/**
- Purpose: Static bundled assets.
- Contains: Help/guide markdown.
- Key files: `app/src/main/assets/help_content.md`

**docs/**
- Purpose: Project notes and planning docs.
- Contains: Developer logs and plans.
- Key files: `docs/DEV_LOG.md`, `docs/PLAN.md`

## Key File Locations

**Entry Points:**
- `app/src/main/AndroidManifest.xml`: Declares app, activities, and services.
- `app/src/main/java/com/notificationlogger/MainActivity.kt`: Compose navigation host.
- `app/src/main/java/com/notificationlogger/NotificationLoggerApp.kt`: Application initialization.
- `app/src/main/java/com/notificationlogger/NotificationListener.kt`: System notification listener service.

**Configuration:**
- `app/build.gradle.kts`: Android module build settings.
- `build.gradle.kts`: Root Gradle config.
- `settings.gradle.kts`: Module inclusion and project name.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle wrapper version.

**Core Logic:**
- `app/src/main/java/com/notificationlogger/UploadWorker.kt`: Background upload orchestration.
- `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`: Sheets API + auth.
- `app/src/main/java/com/notificationlogger/data/Storage.kt`: Room database and preferences.
- `app/src/main/java/com/notificationlogger/data/Models.kt`: Entities and value types.

**Testing:**
- Not detected; no files under `app/src/test/` or `app/src/androidTest/`.

## Naming Conventions

**Files:**
- PascalCase Kotlin files named after primary class/composable (e.g., `app/src/main/java/com/notificationlogger/MainActivity.kt`).

**Directories:**
- Lowercase package paths (e.g., `app/src/main/java/com/notificationlogger/ui/`).

## Where to Add New Code

**New Feature:**
- Primary code: Add new screen or service under `app/src/main/java/com/notificationlogger/`.
- Tests: Add under `app/src/test/java/com/notificationlogger/` or `app/src/androidTest/java/com/notificationlogger/`.

**New Component/Module:**
- Implementation: Create new composable in `app/src/main/java/com/notificationlogger/ui/`.

**Utilities:**
- Shared helpers: Add to `app/src/main/java/com/notificationlogger/data/` for data utilities or a new package under `app/src/main/java/com/notificationlogger/`.

## Special Directories

**app/src/main/assets/**
- Purpose: Bundled static documents and images.
- Generated: No.
- Committed: Yes (`app/src/main/assets/help_content.md`).

**app/src/main/res/**
- Purpose: Android resources for UI and app configuration.
- Generated: No.
- Committed: Yes (`app/src/main/res/values/strings.xml`).

---

*Structure analysis: 2026-01-25*
