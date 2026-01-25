# Project Research Summary

**Project:** Expense Tracker (Android + Sheets)
**Domain:** Android personal expense tracking with Google Sheets as canonical store and hybrid categorization
**Researched:** 2026-01-25
**Confidence:** MEDIUM

## Executive Summary

This project is an Android-first expense tracker that captures notification and manual transactions, persists locally, and syncs into a Google Sheets workflow where categorization and reporting live. Expert approaches in this domain treat the app as an offline-first capture and review surface, with the sheet as the canonical store, and rely on idempotent sync plus clear category precedence to avoid duplicate rows and category drift.

The recommended approach is to standardize a stable transaction ID, a protected sheet schema, and a bidirectional categorization loop (rules in Sheets, overrides in app) before layering in automation. This means building on Room + WorkManager + Sheets API, using batch updates and conflict-aware reconciliation, and structuring the data model to separate raw inputs, rule outputs, and user overrides.

Key risks are duplicate rows from retries, category flips due to competing sources of truth, and schema drift from user edits in Sheets. Mitigations include UUID-based idempotent writes, explicit category precedence columns, and schema protections/named ranges so updates target the right columns regardless of row order.

## Key Findings

### Recommended Stack

The stack is a modern Android Kotlin/Compose setup with Room for local persistence, WorkManager for reliable background sync, and the official Sheets API client with Google Sign-In for OAuth. Versions are current and well-supported, and the stack aligns with existing project constraints. Use DataStore for lightweight preferences and coroutines for all IO and rule evaluation. See `.planning/research/STACK.md` for details.

**Core technologies:**
- Kotlin 2.3.0: primary language for Compose-first Android development.
- Jetpack Compose BOM 2026.01.00: UI toolkit with consistent dependency alignment.
- Room 2.8.4: offline-first persistence for transactions, mappings, and sync state.
- WorkManager 2.11.0: resilient background uploads with retries and constraints.
- Google Sheets API client v4-rev20251110-2.0.0: batch append/update support with OAuth.
- Play services auth 21.5.0: standard Android Google Sign-In flow.

### Expected Features

The MVP must cover category list management, dropdown enforcement in Sheets, manual categorization with an uncategorized queue, simple rule-based categorization, and split transactions. Differentiators focus on hybrid review UI, rules derived from recent edits, and auto-run rules on new rows. AI assistance and prebuilt reporting templates should wait for v2+. See `.planning/research/FEATURES.md`.

**Must have (table stakes):**
- Category assignment in sheet via dropdowns and a customizable category list.
- Manual categorization and correction, including an uncategorized queue.
- Rule-based auto-categorization (simple description-contains rules).
- Split transactions across multiple categories.
- Two-way edits respected by app and sheet (needs validation).

**Should have (competitive):**
- Hybrid review UI for bulk corrections and rule creation from selections.
- Auto-run rules on new rows and category hint mapping.

**Defer (v2+):**
- AI-assisted categorization.
- Prebuilt analytics templates.

### Architecture Approach

Use a layered architecture: Compose UI + ViewModels on top of a repository that writes to Room first and schedules sync via WorkManager. Sheets is the remote store accessed through an isolated client; the sync layer manages uploads, category feedback, retries, and reconciliation. Emphasize idempotent uploads and a category feedback loop with conflict rules. See `.planning/research/ARCHITECTURE.md`.

**Major components:**
1. Repository + Room: local source of truth, transaction IDs, and mapping tables.
2. Sync orchestrator + WorkManager: batching, retries, and background uploads/downloads.
3. Sheets client + OAuth: append/update operations and category pulls.
4. Review/categorization UI: conflict-aware corrections and rule creation.

### Critical Pitfalls

Top pitfalls to plan around are idempotency failures, category precedence conflicts, and schema drift. See `.planning/research/PITFALLS.md`.

1. **No stable transaction identity** — generate UUIDs and upsert by ID to prevent duplicates.
2. **Category logic split across app and sheet** — separate raw/rule/override columns and compute final category with explicit precedence.
3. **Schema drift and row reordering** — lock headers, use named ranges/metadata, and avoid row-index updates.
4. **Timezone/locale mismatch** — store UTC timestamps and numeric amounts, format in UI only.
5. **Quota limits/backoff ignored** — batch updates and use exponential backoff with WorkManager.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Data Model, Schema, and Ingestion
**Rationale:** All higher-level features depend on stable IDs, normalized data, and a protected sheet schema.
**Delivers:** UUID-based transactions, Room schema + mappings, notification/manual capture, template sheet with locked headers/named ranges.
**Addresses:** Category list + dropdowns, manual entry, structured columns.
**Avoids:** Idempotency failure, schema drift, timezone/locale mismatch, parsing drift.

### Phase 2: Sync Reliability and Categorization Loop
**Rationale:** Hybrid categorization requires reliable sync and clear precedence before adding automation.
**Delivers:** WorkManager upload/download workers, batch updates with backoff, category feedback loop, raw/rule/override columns.
**Addresses:** Two-way category/comment sync, manual categorization queue, simple rules.
**Avoids:** Category flip-flops, quota/backoff issues.

### Phase 3: Conflict Resolution and Scale Guardrails
**Rationale:** Once bidirectional edits exist, conflict policies and scale boundaries become essential.
**Delivers:** Last-modified tracking, conflict resolution rules, incremental sync by range, data lifecycle/archiving plan.
**Addresses:** Split transactions representation and consistency at scale.
**Avoids:** Sheet edit conflicts, scale limits.

### Phase 4: Differentiators and Automation
**Rationale:** Only after core reliability is proven should automation and UX accelerators be added.
**Delivers:** Rule builder from selection, auto-run rules on new rows, category hint mapping, optional AI assist (behind review).
**Addresses:** Competitive features and workflow speedups.
**Avoids:** Premature automation and trust issues.

### Phase Ordering Rationale

- Data integrity (IDs, schema, normalization) must exist before any sync or rule automation.
- The architecture depends on a repository-first model and WorkManager sync; these underpin later review and automation UIs.
- Pitfalls show that category precedence and schema protection are early dependencies; conflict resolution and scale guardrails come after sync is stable.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2:** Conflict modeling and two-way sync semantics need validation with Sheets API behavior and current app schema.
- **Phase 3:** Scale thresholds and archiving strategy depend on expected row volumes and user workflows.

Phases with standard patterns (skip research-phase):
- **Phase 1:** Room + WorkManager + Compose capture flows are established patterns in Android.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Versions verified against official maven metadata and docs. |
| Features | MEDIUM | Strong references to Tiller and Sheets UX; some hybrid sync expectations need validation. |
| Architecture | MEDIUM | Standard Android patterns; domain-specific sync nuances need confirmation. |
| Pitfalls | MEDIUM | Common patterns + official API limits; some items include experiential guidance. |

**Overall confidence:** MEDIUM

### Gaps to Address

- Two-way sync semantics (sheet-edited rows vs app edits) need explicit conflict policy validation.
- Split-transaction representation choice (multi-row vs metadata) must be aligned with sheet formulas and reporting.
- Expected dataset size and sync cadence are unknown; define scale targets to pick archiving strategy.

## Sources

### Primary (HIGH confidence)
- https://kotlinlang.org/docs/releases.html — Kotlin 2.3.0 release
- https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml — Compose BOM 2026.01.00
- https://dl.google.com/dl/android/maven2/androidx/room/room-runtime/maven-metadata.xml — Room 2.8.4
- https://dl.google.com/dl/android/maven2/androidx/work/work-runtime-ktx/maven-metadata.xml — WorkManager 2.11.0
- https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml — DataStore 1.2.0
- https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-auth/maven-metadata.xml — Play services auth 21.5.0
- https://repo1.maven.org/maven2/com/google/apis/google-api-services-sheets/maven-metadata.xml — Sheets client v4-rev20251110-2.0.0
- https://developers.google.com/workspace/sheets — Sheets API overview
- https://developers.google.com/workspace/sheets/api/limits — Sheets API quota limits
- https://support.google.com/drive/answer/37603 — Sheets 10M cell limit
- https://support.google.com/docs/answer/186103 — Sheets data validation dropdowns
- https://support.google.com/docs/answer/1272900 — Sheets pivot tables

### Secondary (MEDIUM confidence)
- https://help.tiller.com/en/articles/1886250-categorizing-transactions — category workflow in Sheets
- https://help.tiller.com/en/articles/3250769-customizing-categories — taxonomy editing
- https://help.tiller.com/en/articles/581912-splitting-transactions-between-multiple-categories — split transactions
- https://help.tiller.com/en/articles/3792984-how-to-use-autocat-for-automatic-categorization — rule-based categorization
- https://help.tiller.com/en/articles/10343918-using-tiller-s-category-hints — category hints

### Tertiary (LOW confidence)
- https://help.tiller.com/en/articles/9916508-using-ai-suggest-beta-for-autocat-to-automatically-categorize-transactions — AI suggestions (beta)
- Personal experience with Sheets-backed expense trackers — operational pitfalls

---
*Research completed: 2026-01-25*
*Ready for roadmap: yes*
