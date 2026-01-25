# Requirements: Expense Tracker (Android + Sheets)

**Defined:** 2026-01-25
**Core Value:** I can produce a complete, categorized list of my expenses in Google Sheets so my budgeting system can generate reports.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Capture & Upload

- [ ] **CAP-01**: User can capture transactions from whitelisted app notifications
- [ ] **CAP-02**: User can add manual transactions in the app
- [ ] **CAP-03**: User's transactions upload to Google Sheets automatically with retries on failure
- [ ] **CAP-04**: User can manage the notification whitelist to control which apps are ingested

### Sheet Schema

- [ ] **SHEET-01**: User's transactions are written to fixed columns (A–F + category/comment columns) for reporting
- [ ] **SHEET-02**: New rows copy formulas and formatting from template range `G4:L4`
- [ ] **SHEET-03**: User maintains a category list in the app settings (not synchronized to Sheets)

### Categorization

- [ ] **CAT-01**: User can assign a category in the app using the category list from the app configuration screen
- [ ] **CAT-02**: User can add or edit transaction comments in the app and have them written to Sheets
- [ ] **CAT-03**: User can review an auto-categorized queue in the app and fix categories
- [ ] **CAT-04**: User can rely on sheet rules to auto-assign categories for new rows

### Sync & Consistency

- [ ] **SYNC-01**: User edits categories or comments in Sheets and the app reflects those updates
- [ ] **SYNC-02**: User's manual category overrides are preserved even when sheet rules run
- [ ] **SYNC-03**: User does not see duplicate rows created by retries (stable transaction IDs)

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
| CAP-01 | Phase [TBD] | Pending |
| CAP-02 | Phase [TBD] | Pending |
| CAP-03 | Phase [TBD] | Pending |
| CAP-04 | Phase [TBD] | Pending |
| SHEET-01 | Phase [TBD] | Pending |
| SHEET-02 | Phase [TBD] | Pending |
| SHEET-03 | Phase [TBD] | Pending |
| CAT-01 | Phase [TBD] | Pending |
| CAT-02 | Phase [TBD] | Pending |
| CAT-03 | Phase [TBD] | Pending |
| CAT-04 | Phase [TBD] | Pending |
| SYNC-01 | Phase [TBD] | Pending |
| SYNC-02 | Phase [TBD] | Pending |
| SYNC-03 | Phase [TBD] | Pending |

**Coverage:**
- v1 requirements: 14 total
- Mapped to phases: 0
- Unmapped: 14 ⚠️

---
*Requirements defined: 2026-01-25*
*Last updated: 2026-01-25 after initial definition*
