# Requirements: Expense Tracker (Android + Sheets)

**Defined:** 2026-01-25
**Core Value:** I can produce a complete, categorized list of my expenses in Google Sheets so my budgeting system can generate reports.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Capture & Upload

- [x] **CAP-01**: User can capture transactions from whitelisted app notifications
- [x] **CAP-02**: User can add manual transactions in the app
- [x] **CAP-03**: User's transactions upload to Google Sheets automatically with retries on failure
- [x] **CAP-04**: User can manage the notification whitelist to control which apps are ingested

### Sheet Schema

- [x] **SHEET-01**: User's transactions are written to fixed columns (A–F + category/comment columns) for reporting
- [x] **SHEET-02**: New rows copy formulas and formatting from template range `G4:L4`
- [x] **SHEET-03**: User maintains a category list in the app settings (not synchronized to Sheets)

### Categorization

- [x] **CAT-01**: User can assign a category in the app using the category list from the app configuration screen
- [x] **CAT-02**: User can add or edit transaction comments in the app and have them written to Sheets
- [x] **CAT-03**: User can review an auto-categorized queue in the app and fix categories
- [x] **CAT-04**: User can rely on sheet rules to auto-assign categories for new rows

### Sync & Consistency

- [x] **SYNC-01**: User edits categories or comments in Sheets and the app reflects those updates
- [x] **SYNC-02**: User's manual category overrides are preserved even when sheet rules run
- [x] **SYNC-03**: User does not see duplicate rows created by retries (stable transaction IDs)

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Splits & Rules

- **RULE-02**: Rules can auto-run on new rows without manual action
- **HINT-01**: User can map category hints to their custom taxonomy

### Reporting & Assistance

- **REPORT-01**: User can generate prebuilt reporting templates in Sheets

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| SMS parsing | Requires new permissions; not in v1 scope |
| Multi-user/household support | Personal budgeting only |
| Non-Android clients | Android-first workflow |
| Fully automatic categorization without review | High risk of misclassification |
| Split transactions | Not in scope |
| In-app rule builder | Not in scope |
| AI-assisted categorization | Not in scope |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CAP-01 | Phase 1 | Complete |
| CAP-02 | Phase 1 | Complete |
| CAP-03 | Phase 1 | Complete |
| CAP-04 | Phase 1 | Complete |
| SHEET-01 | Phase 1 | Complete |
| SHEET-02 | Phase 1 | Complete |
| SHEET-03 | Phase 2 | Complete |
| CAT-01 | Phase 2 | Complete |
| CAT-02 | Phase 2 | Complete |
| CAT-03 | Phase 2 | Complete |
| CAT-04 | Phase 3 | Complete |
| SYNC-01 | Phase 3 | Complete |
| SYNC-02 | Phase 3 | Complete |
| SYNC-03 | Phase 1 | Complete |

**Coverage:**
- v1 requirements: 14 total
- Mapped to phases: 14
- Unmapped: 0

---
*Requirements defined: 2026-01-25*
*Last updated: 2026-01-25 after validation*
