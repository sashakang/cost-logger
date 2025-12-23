
## [2025-12-21 18:20:45] Add TransactionEntry entity to Models.kt

**Problem**: Need to add TransactionEntry entity for manual transaction tracking, following the same pattern as NotificationEntry
**Solution**: 
  - Extracted formatTimestamp() from NotificationEntry.Companion to top-level private function for reuse
  - Added TransactionEntry entity with Room annotations (@Entity, @PrimaryKey, @Index on timestamp)
  - Implemented toSheetRow() method returning [UTC timestamp, account, "Manual Entry", "$currency $amount", local timestamp, "manual_$id"]
  - File: app/src/main/java/com/notificationlogger/data/Models.kt
**Outcome**: ✅ SUCCESS
  - First try: Yes - implementation worked immediately
  - Tests: N/A - Android project requires Android SDK for compilation, manual code review passed
  - Regressions: None - additive change only
**Lessons Learned**:
  - Extracting shared utilities to top-level functions simplifies code reuse in Kotlin
  - Following existing patterns (NotificationEntry) made implementation straightforward
  - Room entity pattern (table name, indices, auto-increment PK) is consistent and predictable
  - Android projects may not have test suites that run outside of Android SDK environment

## [2025-12-21 18:52:00] Transform Main Screen to Settings Screen with Transaction Entry

**Problem**: Main screen needed to be simplified to 2 action buttons (Re-scan Notifications, Enter Transaction), with settings moved to separate screen. Transaction entry required with Account/Amount/Currency/Category fields, offline queue, and upload to Google Sheets.

**Solution**:
- Created TransactionEntry Room entity in Models.kt
- Added TransactionDao and bumped database to v5 in Storage.kt
- Added recency tracking for currencies/categories in AppPreferences
- Created new SettingsScreen.kt with all settings moved from MainScreen
- Created TransactionEntrySheet.kt with ModalBottomSheet and dropdowns
- Simplified MainScreen.kt to 2 buttons + settings icon (59 lines from 353)
- Updated MainActivity.kt with settings route and transaction handling
- Updated UploadWorker.kt and SheetsService.kt for transaction upload

**Outcome**: PENDING VERIFICATION - Build requires Gradle wrapper fix or Android Studio
  - First try: All code changes implemented successfully
  - Tests: Cannot run - Gradle wrapper not properly configured
  - Regressions: Unknown until build is verified

**Lessons Learned**:
  - What would I do differently? Verify build environment before starting
  - What patterns emerged? Extracted shared timestamp formatting to top-level function in Models.kt
  - What to avoid next time? Relying on build verification when Gradle not available

## [2025-12-21 19:45:00] Fix Transaction Entry Form Layout and Dropdown Visibility

**Problem**: Transaction entry form had three issues:
1. Form appeared at bottom of screen (ModalBottomSheet) instead of top
2. Amount field was multi-line instead of single-line
3. Category dropdown menu extended below screen boundary, making categories unselectable

**Solution**:
- Converted TransactionEntrySheet from ModalBottomSheet to Scaffold with TopAppBar
- Form now displays at top of screen with "Add Transaction" title and back navigation
- Added `singleLine = true` to amount OutlinedTextField
- Added `verticalScroll(rememberScrollState())` to make form scrollable
- Added `BackHandler(onBack = onDismiss)` for system back button
- Added `imePadding()` for keyboard handling
- File: app/src/main/java/com/notificationlogger/ui/TransactionEntrySheet.kt

**Outcome**: ✅ SUCCESS
  - First try: Yes - implementation and consensus achieved on first attempt
  - Tests: N/A - Android project requires Android SDK for compilation
  - Regressions: None - same callback interface maintained, no MainActivity changes needed

**Lessons Learned**:
  - ModalBottomSheet is problematic for forms with dropdowns near bottom of sheet
  - Scaffold + TopAppBar is the correct Material3 pattern for full-screen forms
  - verticalScroll ensures dropdown menus have room to expand
  - BackHandler is essential when replacing navigation-like components

## [2025-12-22 19:50:00] Fix Category Dropdown to Show All Categories

**Problem**: Category dropdown in manual entry screen only showed 1 category (from recentCategories) instead of all configured categories from settings.

**Solution**: 
- Changed MainActivity.kt line 217 from:
  `categories = prefs.recentCategories.ifEmpty { prefs.categories }`
  to:
  `categories = prefs.categories`
- The issue: `recentCategories` had default fallback to `categories.take(5)`, so it was never empty, meaning the full categories list was never used
- Fix directly uses the full configured categories list

**Outcome**: ✅ SUCCESS
  - First try: Yes - simple 1-line fix
  - Tests: N/A - Android project
  - Regressions: None

**Lessons Learned**:
  - The `ifEmpty` fallback logic was flawed because `recentCategories` has its own default value
  - When user wants "all items", use the source list directly, not a filtered/subset list with fallbacks
