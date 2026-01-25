# Expense Tracker (Android + Sheets)

## What This Is

An Android app that captures expense transactions from whitelisted app notifications and manual entries, then uploads them to Google Sheets. It is built for a personal budgeting workflow where the sheet is the source of truth and categories drive reporting.

## Core Value

I can produce a complete, categorized list of my expenses in Google Sheets so my budgeting system can generate reports.

## Requirements

### Validated

- ✓ Capture Android app notifications from a user-managed whitelist and store them as expense entries — existing
- ✓ Manual transaction entry with upload to Google Sheets — existing
- ✓ Background upload queue and retry to Sheets — existing
- ✓ Category/comment write-back to Sheets from in-app selection — existing
- ✓ Google Sign-In + Sheets API integration — existing

### Active

- [ ] Maintain a complete list of expenses in Google Sheets using both manual entry and notification capture
- [ ] Support hybrid categorization: app edits + sheet-based rules
- [ ] Keep notification scope to whitelisted apps (no SMS ingestion in v1)

### Out of Scope

- SMS parsing — not in v1 and requires new permissions
- Multi-user or shared budgeting — personal use only
- Non-Android clients — Android-first workflow

## Context

- Existing Kotlin/Compose Android app with Room, WorkManager, and Google Sheets integration.
- Current sheet write flow appends columns A–F and writes category/comment into the sheet; formulas live in the sheet.
- Categorization is a pain point; sheet-based rules will drive auto-category with app corrections.

## Constraints

- **Platform**: Android app only (minSdk 26), Compose UI, Room, WorkManager — existing codebase
- **Integration**: Google Sheets via OAuth; sheet remains the canonical data store
- **Data format**: Keep current A–F transaction columns and category/comment columns in Sheets

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Hybrid categorization with rules in Sheets | Sheets is the canonical source; rules are easier to maintain there | — Pending |
| Capture sources: whitelisted app notifications + manual entry | Matches existing code and user workflow | — Pending |
| Audience is personal-only | Keeps scope focused on one-user budgeting | — Pending |

---
*Last updated: 2026-01-25 after initialization*
