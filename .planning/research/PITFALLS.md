# Pitfalls Research

**Domain:** Personal expense tracking Android app with Google Sheets backend
**Researched:** 2026-01-25
**Confidence:** MEDIUM

## Critical Pitfalls

### Pitfall 1: No stable transaction identity (idempotency failure)

**What goes wrong:**
Uploads are retried as `append`, producing duplicate rows or overwriting the wrong row when a device retries, edits, or backfills.

**Why it happens:**
Teams rely on row index, timestamp, or (merchant, amount) tuples instead of a stable transaction ID that survives retries and edits.

**How to avoid:**
Generate a UUID per transaction in the app, store it in a dedicated column, and make all writes idempotent by upserting based on that ID. Avoid row-index updates; search by ID or store developer metadata for row lookups.

**Warning signs:**
Duplicate transactions after reconnecting; totals drift from bank statements; manual deletes required to fix duplicates.

**Phase to address:**
Phase 1 (Data model + sync protocol)

---

### Pitfall 2: Category logic split between sheet formulas and app overrides

**What goes wrong:**
Categories “flip back” after sync or recalculation, and corrections never stick because formulas overwrite user edits or the app overwrites formula outputs.

**Why it happens:**
Rules live in Sheets while manual corrections live in the app; the system lacks a single source of truth or a clear precedence model.

**How to avoid:**
Separate columns for `raw_category`, `rule_category`, and `user_override`, and compute `final_category` with explicit precedence. Protect formula columns and only write to override columns from the app.

**Warning signs:**
User edits revert on the next sync; category counts differ between the app and the sheet; repeated “fix the same transaction” reports.

**Phase to address:**
Phase 2 (Categorization model + sheet schema)

---

### Pitfall 3: Sheet schema drift and row reordering break updates

**What goes wrong:**
Users insert columns, sort rows, or change headers; the app writes to the wrong columns or rows and formulas stop propagating.

**Why it happens:**
The sheet is treated as a free-form document, while the app assumes stable column positions and row ordering.

**How to avoid:**
Use a locked header row, named ranges, or developer metadata for column mapping. Prevent row-based updates; use ID-based lookups. Provide a template sheet and enforce schema versioning.

**Warning signs:**
App edits land in the wrong column; new rows lack formulas; reports show malformed values.

**Phase to address:**
Phase 1 (Schema + protections)

---

### Pitfall 4: Timezone/locale normalization is inconsistent

**What goes wrong:**
Day and month totals drift; transactions appear on the wrong day; currencies parse incorrectly due to locale formatting.

**Why it happens:**
Sheets uses spreadsheet locale/timezone while the app uses device settings. Dates and amounts are stored as formatted strings instead of normalized values.

**How to avoid:**
Store UTC timestamps and ISO date strings in dedicated columns; store numeric amounts in minor units (cents). Keep display formatting in the UI only.

**Warning signs:**
End-of-month totals differ between app and sheet; same transaction appears on adjacent dates; “1,234.56” vs “1.234,56” mismatches.

**Phase to address:**
Phase 1 (Data normalization)

---

### Pitfall 5: Quota limits and backoff are ignored

**What goes wrong:**
Uploads fail with 429 errors, partial writes, or long timeouts. Retrying without idempotency creates duplicates.

**Why it happens:**
Sync uses per-transaction writes, no batching, and no exponential backoff. Sheets API quotas are exceeded under bursty usage.

**How to avoid:**
Batch appends/updates, implement truncated exponential backoff, and throttle sync. Keep payloads under recommended sizes and queue retries via WorkManager.

**Warning signs:**
Frequent `429: Too many requests` errors; long upload times; users report “sync stuck”.

**Phase to address:**
Phase 2 (Sync reliability + backoff)

---

### Pitfall 6: Sheet edits create silent conflicts with app state

**What goes wrong:**
Users edit rows directly in Sheets, and the app later overwrites those edits with stale local data.

**Why it happens:**
There is no reconciliation mechanism (no last-modified column or conflict policy), so the app treats itself as authoritative.

**How to avoid:**
Track `last_modified_at` in both app and sheet, detect remote edits, and require explicit resolution rules (sheet-wins, app-wins, or manual merge).

**Warning signs:**
Users complain their sheet edits disappear after sync; repeated data corrections.

**Phase to address:**
Phase 3 (Bidirectional sync + conflict resolution)

---

### Pitfall 7: Notification parsing degrades silently

**What goes wrong:**
Bank notification formats change and the parser stops extracting amounts/merchants, leading to missing or misclassified transactions.

**Why it happens:**
Parsing relies on brittle regexes and assumes fixed notification formats or locales.

**How to avoid:**
Store raw notification text, track parse success rates, and allow manual correction flows. Maintain per-source parsing rules with versioning.

**Warning signs:**
Drop in parsed transaction counts; surge in “unknown merchant” entries; user reports of missed transactions.

**Phase to address:**
Phase 1 (Ingestion + parsing) and Phase 4 (Monitoring)

---

### Pitfall 8: Sheets scale limits ignored

**What goes wrong:**
The sheet hits cell limits and becomes slow or refuses new data, halting sync and analysis.

**Why it happens:**
All historical data stays in one sheet without archiving or partitioning by month/year.

**How to avoid:**
Partition by month or year, and archive old rows into separate sheets. Build rollups rather than recomputing over the entire dataset.

**Warning signs:**
Slow edits, formula recalculation delays, or inability to append new rows.

**Phase to address:**
Phase 3 (Data lifecycle + archiving)

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Use row index as transaction ID | Easy updates | Breaks on sorting, duplicates on retries | Never |
| Store amounts as formatted strings | Easy display | Locale parsing bugs, broken aggregation | MVP only, but replace quickly |
| One monolithic sheet forever | Simple setup | Hits size limits, slow formulas | MVP only, if monthly volume is low |

## Integration Gotchas

Common mistakes when connecting to external services.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Google Sheets API | No batching or backoff; retries append duplicates | Batch updates + exponential backoff + idempotent IDs |
| OAuth consent | Requesting overly broad scopes triggers review delays | Use narrow scopes; avoid sensitive/restricted scopes when possible |
| Google Drive permissions | Sheet not shared with the correct account/service | Enforce share/ownership checks during setup |

## Performance Traps

Patterns that work at small scale but fail as usage grows.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Per-transaction API calls | Slow sync, quota errors | Batch updates and queue work | ~100+ transactions/day bursty usage |
| Full sheet read on every sync | Long sync, high data usage | Incremental sync by last_modified | ~5k+ rows |
| Sheet-wide formulas on every row | Sheet becomes sluggish | Limit ranges, use rollups | ~50k+ rows |

## Security Mistakes

Domain-specific security issues beyond general mobile security.

| Mistake | Risk | Prevention |
|---------|------|------------|
| Storing OAuth tokens unencrypted | Account takeover, data exposure | Use encrypted storage; rotate tokens |
| Using broad scopes like full Drive access | Excessive data access if compromised | Request least-privilege Sheets scope only |
| Sharing sheets publicly for convenience | PII leakage | Require explicit owner-only sharing, warn on public links |

## UX Pitfalls

Common user experience mistakes in this domain.

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| No visible sync status | Users assume data is saved | Show last sync time + error state |
| Category overrides are hidden | Users can’t trust corrections | Expose override state and allow undo |
| Too many categories early | Users abandon setup | Start with a small default taxonomy, grow over time |

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces.

- [ ] **Sync:** Often missing idempotent IDs — verify duplicate-proof retries
- [ ] **Categories:** Often missing override precedence — verify rule + override flow
- [ ] **Sheet template:** Often missing locked headers — verify schema protection
- [ ] **Analytics:** Often missing timezone normalization — verify daily totals match

## Recovery Strategies

When pitfalls occur despite prevention, how to recover.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Duplicate rows from retries | MEDIUM | Deduplicate by transaction ID; re-run category formulas |
| Schema drift | HIGH | Migrate to a new template sheet; map and backfill columns |
| Category divergence | MEDIUM | Recompute final category from raw + override columns |

## Pitfall-to-Phase Mapping

How roadmap phases should address these pitfalls.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| No stable transaction identity | Phase 1 | Append retry test does not create duplicates |
| Category logic split | Phase 2 | Overrides persist after rule recalculation |
| Schema drift | Phase 1 | Insert column/sort row does not break updates |
| Timezone/locale mismatch | Phase 1 | Daily totals match across app and sheet |
| Quota/backoff ignored | Phase 2 | Sustained sync under quota without 429 errors |
| Sheet edits conflict | Phase 3 | Remote edits detected and resolved |
| Parsing drift | Phase 1/4 | Parse failure rate monitored and alertable |
| Sheets scale limits | Phase 3 | Archive/partition verified on large dataset |

## Sources

- https://developers.google.com/workspace/sheets/api/limits (Sheets API quotas, backoff guidance)
- https://support.google.com/drive/answer/37603 (Google Sheets 10M cell limit)
- https://developers.google.com/workspace/guides/configure-oauth-consent (OAuth scopes and consent requirements)
- Personal experience with Sheets-backed expense trackers (LOW confidence)

---
*Pitfalls research for: personal expense tracking Android app with Google Sheets backend*
*Researched: 2026-01-25*
