# AGENTS.md

Purpose: guidance for agentic coding tools working in this repo.

Project summary
- Android app (Kotlin) that logs notifications and manual transactions to Google Sheets.
- UI is Jetpack Compose + Material 3; persistence via Room; background uploads via WorkManager.

Repository layout
- app/src/main/java/com/notificationlogger/: main app code (Compose UI, data, services).
- app/src/main/res/: Android resources.
- app/src/test/: local JVM unit tests (none yet).
- app/src/androidTest/: instrumented UI/device tests (none yet).

Build, lint, and test commands
- Build debug APK: `./gradlew assembleDebug`
- Install debug APK: `./gradlew installDebug`
- Run app unit tests: `./gradlew testDebugUnitTest`
- Run instrumented tests on device: `./gradlew connectedDebugAndroidTest`
- Run all lint checks: `./gradlew lint`
- Run debug lint only: `./gradlew lintDebug`

Single-test commands (Android)
- Single JVM unit test class:
  `./gradlew testDebugUnitTest --tests "com.notificationlogger.MyTest"`
- Single JVM unit test method:
  `./gradlew testDebugUnitTest --tests "com.notificationlogger.MyTest.myMethod"`
- Single instrumented test class:
  `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.notificationlogger.MyTest`
- Single instrumented test package:
  `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.notificationlogger.ui`

Gradle/SDK expectations
- Kotlin JVM target: 17.
- Android: minSdk 26, target/compile 34.
- Kotlin code style: official (`kotlin.code.style=official`).

Credentials and secrets
- OAuth credentials live in `.env` (see `.env.example`).
- Do not commit `.env` or client secrets.
- Update Web Client ID in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`.

Code style guidelines (observed in repo)
- Kotlin official formatting: 4-space indent, trailing commas where it improves diffs.
- Use KDoc for non-obvious public APIs, helpers, and data models.
- Favor `val` and immutable data; use `var` only for state or caches.
- Prefer data classes for models; sealed interfaces for result types.
- Keep functions focused; use small helpers when it simplifies complex UI or data work.

Imports
- Keep imports sorted and grouped by package; avoid unused imports.
- Wildcard imports are acceptable for Compose packages (see `MainScreen.kt`).
- Use explicit imports elsewhere to keep API use obvious.

Naming conventions
- Classes: PascalCase (e.g., `SheetsService`, `NotificationEntry`).
- Functions/vars: camelCase (`getAccessToken`, `uploadBatchWithRowInfo`).
- Constants: SCREAMING_SNAKE_CASE in companion objects.
- Compose functions: PascalCase and annotated with `@Composable`.
- Resource IDs: snake_case.

Types and nullability
- Use explicit nullable types (`String?`) for optional values from prefs or APIs.
- Prefer early returns on null checks to reduce nesting.
- Use `Result<T>` or sealed results (`UploadResult`) for operations with failures.

Error handling and logging
- Use `try/catch` around network and IO operations.
- Log with `Log.d/w/e` and include context; return graceful fallbacks on failure.
- Avoid throwing across coroutine boundaries; return result objects instead.

Coroutines and threading
- Use `Dispatchers.IO` for network and database work.
- Keep UI state in Compose `remember` state or via `collectAsState` from Flow.
- Use `rememberCoroutineScope` for UI-triggered async work.

Compose UI guidance
- Use Material 3 components (`Scaffold`, `TopAppBar`, `Button`, `Card`).
- Keep UI layout in composables under `app/src/main/java/com/notificationlogger/ui/`.
- When adding screens, update navigation in `MainActivity.kt`.

Room and data access
- DAOs return `Flow` for live data, `suspend` functions for queries.
- Room database uses `fallbackToDestructiveMigration` today; avoid schema changes without a plan.

WorkManager
- Use `OneTimeWorkRequestBuilder` for uploads; enqueue via `WorkManager.getInstance`.
- Keep work names and tags consistent if you add new workers.

Sheets API integration
- Use `SheetsService` for auth and networking; keep API calls on `Dispatchers.IO`.
- Append rows uses columns A-F and copies template formulas (G-L); respect this layout.

Documentation expectations
- Update `README.md` for feature changes that affect setup or usage.
- Add brief KDoc for new public classes or Compose screens.

Agent-specific notes
- No Cursor rules found in `.cursor/rules/` or `.cursorrules`.
- No Copilot instructions found in `.github/copilot-instructions.md`.

Quality checks before PR
- Build passes: `./gradlew assembleDebug`.
- Lint passes: `./gradlew lint` (or `lintDebug`).
- Add tests for new business logic when feasible.
