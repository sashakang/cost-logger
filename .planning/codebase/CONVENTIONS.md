# Coding Conventions

**Analysis Date:** 2026-01-25

## Naming Patterns

**Files:**
- Use PascalCase file names for classes and screens (examples: `app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/CategorySelectionActivity.kt`, `app/src/main/java/com/notificationlogger/ui/MainScreen.kt`).

**Functions:**
- Use camelCase for functions and parameters (examples: `app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`, `app/src/main/java/com/notificationlogger/NotificationListener.kt`).

**Variables:**
- Use camelCase for locals and properties, prefer `val` unless mutation is needed (examples: `app/src/main/java/com/notificationlogger/ui/TransactionEntrySheet.kt`, `app/src/main/java/com/notificationlogger/data/Storage.kt`).

**Types:**
- Use PascalCase for classes/data classes and sealed interfaces (examples: `app/src/main/java/com/notificationlogger/data/Models.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`).

## Code Style

**Formatting:**
- Kotlin official style is enforced via `kotlin.code.style=official` in `gradle.properties`.
- 4-space indentation and trailing commas are used where they improve diffs (examples: `app/src/main/java/com/notificationlogger/data/Storage.kt`, `app/src/main/java/com/notificationlogger/ui/SettingsScreen.kt`).

**Linting:**
- Android Gradle lint tasks are used for quality checks (`./gradlew lint`, `./gradlew lintDebug`) documented in `AGENTS.md`.

## Import Organization

**Order:**
1. Android/Java/Kotlin standard and platform imports (examples: `app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/NotificationListener.kt`).
2. Third-party libraries (examples: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`).
3. Project-local imports (examples: `app/src/main/java/com/notificationlogger/UploadWorker.kt`, `app/src/main/java/com/notificationlogger/ui/MainScreen.kt`).

**Path Aliases:**
- Not detected; use full package imports (examples: `app/src/main/java/com/notificationlogger/ui/MainScreen.kt`).

**Wildcard Imports:**
- Allowed for Compose packages (examples: `app/src/main/java/com/notificationlogger/ui/MainScreen.kt`, `app/src/main/java/com/notificationlogger/ui/TransactionEntrySheet.kt`).

## Error Handling

**Patterns:**
- Use guard clauses and early returns for invalid state (examples: `app/src/main/java/com/notificationlogger/NotificationListener.kt`, `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`).
- Wrap network/IO and system interactions in `try/catch` and return a safe fallback (`null`, `false`, or result object) while logging context (examples: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`).

## Logging

**Framework:** Android `Log` (`Log.d`, `Log.w`, `Log.e`) used across the app (examples: `app/src/main/java/com/notificationlogger/NotificationListener.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`).

**Patterns:**
- Include context in message strings and attach exceptions for failures (examples: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`, `app/src/main/java/com/notificationlogger/CategorySelectionActivity.kt`).

## Comments

**When to Comment:**
- Use KDoc for public classes, composables, and non-obvious helpers (examples: `app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/data/Models.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`).

**JSDoc/TSDoc:**
- Not applicable; Kotlin KDoc is used instead (examples: `app/src/main/java/com/notificationlogger/data/Storage.kt`).

## Function Design

**Size:**
- Keep functions focused and extract helpers for clarity (examples: `app/src/main/java/com/notificationlogger/NotificationListener.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`).

**Parameters:**
- Prefer explicit parameters over implicit state; pass dependencies into composables and helpers (examples: `app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/ui/MainScreen.kt`).

**Return Values:**
- Use nullable returns for optional values and result objects for success/failure (examples: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`, `app/src/main/java/com/notificationlogger/data/Models.kt`).

## Module Design

**Exports:**
- Use Kotlin top-level functions, data classes, and companion objects for constants/singletons (examples: `app/src/main/java/com/notificationlogger/data/Models.kt`, `app/src/main/java/com/notificationlogger/data/Storage.kt`, `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`).

**Barrel Files:**
- Not used; import Kotlin packages directly (examples: `app/src/main/java/com/notificationlogger/MainActivity.kt`).

---

*Convention analysis: 2026-01-25*
