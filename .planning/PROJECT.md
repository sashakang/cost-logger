# Expense Tracker (Android + Sheets)

## What This Is

An Android app that captures expense transactions from whitelisted app notifications and manual entries, then uploads them to Google Sheets. It is built for a personal budgeting workflow where the sheet is the source of truth and categories drive reporting.

## Core Value

I can produce a complete, categorized list of my expenses in Google Sheets so my budgeting system can generate reports.

## Requirements

### Validated

- ✓ Capture transactions from whitelisted app notifications — existing
- ✓ Manual transaction entry in-app — existing
- ✓ Background upload queue and retry to Sheets — existing
- ✓ Fixed sheet schema (A–F + category/comment columns) with template formulas — existing
- ✓ App-managed category list and in-app category assignment — existing
- ✓ Comment editing in-app with write-back to Sheets — existing
- ✓ Uncategorized review flow in-app — existing
- ✓ Sheet rules can auto-assign categories for new rows — existing
- ✓ Two-way sync for categories/comments between app and Sheets — existing
- ✓ Manual category overrides are preserved against sheet rules — existing
- ✓ Stable transaction IDs prevent duplicate rows — existing
- ✓ Google Sign-In + Sheets API integration — existing

### Active

(None — all current requirements implemented)

### Out of Scope

- SMS parsing — not in v1 and requires new permissions
- Multi-user or shared budgeting — personal use only
- Non-Android clients — Android-first workflow
- Split transactions — not in scope
- In-app rule builder — not in scope
- AI-assisted categorization — not in scope

## Context

- Existing Kotlin/Compose Android app with Room, WorkManager, and Google Sheets integration.
- Current sheet write flow appends columns A–F and writes category/comment into the sheet; formulas live in the sheet.
- Category list is managed in the app settings (not synchronized to Sheets).

## Constraints

- **Platform**: Android app only (minSdk 26), Compose UI, Room, WorkManager — existing codebase
- **Integration**: Google Sheets via OAuth; sheet remains the canonical data store
- **Data format**: Keep current A–F transaction columns and category/comment columns in Sheets

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Hybrid categorization with rules in Sheets | Sheets is the canonical source; rules are easier to maintain there | ✓ Good |
| Capture sources: whitelisted app notifications + manual entry | Matches existing code and user workflow | ✓ Good |
| Audience is personal-only | Keeps scope focused on one-user budgeting | ✓ Good |

---
*Last updated: 2026-01-25 after validation*
