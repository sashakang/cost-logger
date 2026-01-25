# Codebase Concerns

**Analysis Date:** 2026-01-25

## Tech Debt

**Room schema changes wipe user data:**
- Issue: `fallbackToDestructiveMigration()` drops local history on version bumps instead of migrating.
- Files: `app/src/main/java/com/notificationlogger/data/Storage.kt`
- Impact: Notification/transaction history can be lost on upgrades.
- Fix approach: Add explicit Room migrations and enable schema export; remove destructive fallback in `app/src/main/java/com/notificationlogger/data/Storage.kt`.

**Notification dedupe overwrites prior rows:**
- Issue: `@Insert(onConflict = OnConflictStrategy.REPLACE)` with a unique index on `notificationKey` can replace prior rows and reset `uploaded` state.
- Files: `app/src/main/java/com/notificationlogger/data/Storage.kt`
- Impact: Previously uploaded entries can be re-queued or lost, and IDs change unexpectedly.
- Fix approach: Use `OnConflictStrategy.IGNORE` + `@Update`, or query existing rows and update specific fields in `app/src/main/java/com/notificationlogger/data/Storage.kt`.

**Database growth has no cleanup path:**
- Issue: Uploaded entries are never purged; `deleteUploaded()` is unused.
- Files: `app/src/main/java/com/notificationlogger/data/Storage.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`
- Impact: Local database grows unbounded, slowing queries and increasing storage use.
- Fix approach: Add a retention policy or periodic cleanup after upload in `app/src/main/java/com/notificationlogger/UploadWorker.kt`.

**Hardcoded OAuth web client ID:**
- Issue: `WEB_CLIENT_ID` must be changed in code for each environment.
- Files: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`, `README.md`
- Impact: Builds ship with stale OAuth client IDs and sign-in fails if not manually updated.
- Fix approach: Move client ID into `local.properties`/`BuildConfig` or a string resource and read it in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`.

## Known Bugs

**Category flow skips “unopened” notifications:**
- Symptoms: After saving a category, the app advances to the next active notification rather than finding the next unopened one.
- Files: `app/src/main/java/com/notificationlogger/CategorySelectionActivity.kt`, `docs/TODO.md`
- Trigger: Assign a category when multiple earlier notifications remain in the tray.
- Workaround: Manually open remaining category notifications from the status bar.

**Transaction uploads can stall without retry:**
- Symptoms: Transaction rows fail to upload or update category/comment but WorkManager marks the work as success.
- Files: `app/src/main/java/com/notificationlogger/UploadWorker.kt`, `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
- Trigger: Any error while `uploadTransactions()` runs after a successful notification upload.
- Workaround: Manually trigger rescan/upload from the main screen in `app/src/main/java/com/notificationlogger/MainActivity.kt`.

## Security Considerations

**Sensitive data stored unencrypted at rest:**
- Risk: Notification contents and transaction details are stored in plain Room tables.
- Files: `app/src/main/java/com/notificationlogger/data/Storage.kt`
- Current mitigation: Android app sandbox only.
- Recommendations: Consider SQLCipher or encrypted Room and redact stored notification text when possible in `app/src/main/java/com/notificationlogger/data/Models.kt`.

**App data is eligible for device backups:**
- Risk: `android:allowBackup="true"` can copy notification content to cloud backups.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/notificationlogger/data/Storage.kt`
- Current mitigation: None.
- Recommendations: Disable backups or add a custom backup policy for sensitive data in `app/src/main/AndroidManifest.xml`.

**Local OAuth secrets file present:**
- Risk: `.env` contains client secrets; accidental commit would expose credentials.
- Files: `.env`, `.gitignore`, `README.md`
- Current mitigation: `.env` is gitignored.
- Recommendations: Keep `.env` out of VCS and prefer Gradle local properties or Android Studio secrets tooling.

## Performance Bottlenecks

**Full-sheet scans for categorization:**
- Problem: `findNextUncategorizedRow()` reads `A{startRow+1}:L` without paging.
- Files: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
- Cause: Single large range read for each scan.
- Improvement path: Page with bounded ranges or use filter queries to limit rows.

**Per-row API calls for categories/comments:**
- Problem: Each uploaded row triggers multiple `readCell()`/`writeCell()` calls.
- Files: `app/src/main/java/com/notificationlogger/UploadWorker.kt`, `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`
- Cause: No batch read/write for categories/comments.
- Improvement path: Use Sheets batchUpdate to read/write column I/M in a single call.

## Fragile Areas

**Start-row parsing assumes a specific response format:**
- Files: `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`
- Why fragile: `parseStartRowFromResponse()` only matches ranges like `A5:`; if Sheets returns a different range format, `startRow` becomes null.
- Safe modification: Broaden range parsing and handle missing start rows before relying on template copy/category writes.
- Test coverage: No tests cover append response parsing in `app/src/test/`.

**Upload scheduling depends on new events:**
- Files: `app/src/main/java/com/notificationlogger/NotificationListener.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`
- Why fragile: `ExistingWorkPolicy.KEEP` with one-time work means leftover rows may never upload without a new notification.
- Safe modification: Reschedule when `remainingCount > 0` in `app/src/main/java/com/notificationlogger/UploadWorker.kt`.
- Test coverage: No tests cover WorkManager re-queue logic in `app/src/androidTest/`.

## Scaling Limits

**Fixed batch size and no follow-up scheduling:**
- Current capacity: 50 rows per worker run (`BATCH_SIZE`).
- Limit: Queues larger than 50 can stall without new notifications.
- Scaling path: Chain work requests until `getPendingCount()` is zero in `app/src/main/java/com/notificationlogger/UploadWorker.kt`.

**Sheet-range scans scale poorly with large sheets:**
- Current capacity: Reads all populated rows on each search.
- Limit: Large sheets increase latency and hit API timeouts.
- Scaling path: Track last processed row in preferences and page reads in `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`.

## Dependencies at Risk

**Not detected:**
- Risk: No abandoned/deprecated dependencies identified.
- Impact: None observed.
- Migration plan: Not applicable.
- Files: `app/build.gradle.kts`

## Missing Critical Features

**No periodic retry/sync for pending uploads:**
- Problem: Uploads are only scheduled on new notifications or manual rescan.
- Blocks: Offline backlogs can persist indefinitely without user action.
- Files: `app/src/main/java/com/notificationlogger/NotificationListener.kt`, `app/src/main/java/com/notificationlogger/MainActivity.kt`, `app/src/main/java/com/notificationlogger/UploadWorker.kt`

## Test Coverage Gaps

**Core upload and parsing logic has no tests:**
- What's not tested: WorkManager flows, Sheets append response parsing, and error retries.
- Files: `app/src/main/java/com/notificationlogger/UploadWorker.kt`, `app/src/main/java/com/notificationlogger/sheets/SheetsService.kt`, `app/src/test/`, `app/src/androidTest/`
- Risk: Failures in upload/retry flows go unnoticed until production.
- Priority: High

---

*Concerns audit: 2026-01-25*
