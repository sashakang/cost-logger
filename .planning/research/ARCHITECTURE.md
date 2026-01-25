# Architecture Research

**Domain:** Personal expense tracking Android app with Google Sheets sync
**Researched:** 2026-01-25
**Confidence:** MEDIUM

## Standard Architecture

### System Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Presentation Layer                           │
├──────────────────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌─────────────────┐  ┌───────────────────────┐  │
│  │ Capture UI     │  │ Review/Categor. │  │ Settings/Account UI   │  │
│  └───────┬────────┘  └────────┬────────┘  └───────────┬───────────┘  │
│          │                    │                       │               │
├──────────┴────────────────────┴───────────────────────┴──────────────┤
│                           Domain / Sync Layer                          │
├──────────────────────────────────────────────────────────────────────┤
│  ┌───────────────┐  ┌────────────────┐  ┌─────────────────────────┐  │
│  │ Repository    │  │ Sync Orchestr. │  │ Category Feedback Loop  │  │
│  └───────┬───────┘  └───────┬────────┘  └──────────┬──────────────┘  │
│          │                  │                     │                 │
├──────────┴──────────────────┴─────────────────────┴─────────────────┤
│                           Data / Integration Layer                     │
├──────────────────────────────────────────────────────────────────────┤
│  ┌───────────────┐  ┌────────────────┐  ┌─────────────────────────┐  │
│  │ Room DB       │  │ WorkManager    │  │ Sheets API + OAuth      │  │
│  │ (Local Store) │  │ (Background)   │  │ (Remote Store)          │  │
│  └───────────────┘  └────────────────┘  └─────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| Notification capture | Ingest notification-derived transactions | `NotificationListenerService` + mapping layer |
| Manual capture UI | Create/edit transactions | Compose screens + ViewModel |
| Review/categorization UI | Display categories, corrections, and matches | Compose + Flow from Room |
| Repository | Single source of truth, orchestrate local/remote | Kotlin repo with DAO + Sheets client |
| Sync orchestrator | Schedule uploads/downloads, retry, backoff | WorkManager workers + constraints |
| Category feedback loop | Pull categories from Sheets, apply to local | Worker + reconciliation logic |
| Local store | Persist transactions, sync state, mappings | Room entities + DAOs |
| Remote store | Read/write rows, batch updates | Google Sheets API client |
| Auth/session | Manage OAuth tokens and scopes | Google Identity/Account manager |

## Recommended Project Structure

```
app/src/main/java/com/notificationlogger/
├── ui/                      # Compose screens and UI state
│   ├── capture/             # Manual entry, edit flows
│   ├── review/              # Categorization and corrections
│   └── settings/            # Account and sync settings
├── data/                    # Local persistence and models
│   ├── db/                  # Room database, entities, DAOs
│   ├── model/               # Domain models + mappers
│   └── repo/                # Repository interfaces/impls
├── sync/                    # Sync orchestration and workers
│   ├── worker/              # Upload/download workers
│   ├── queue/               # Pending ops, retry policies
│   └── reconcile/           # Category and row mapping logic
├── sheets/                  # Google Sheets API access
│   ├── auth/                # OAuth token handling
│   └── client/              # SheetsService calls
├── notifications/           # Notification listener + parsing
└── settings/                # Preference/DataStore access
```

### Structure Rationale

- **ui/:** Keeps Compose screens isolated from sync/network concerns.
- **data/:** Centralizes local source of truth and mapping logic.
- **sync/:** Makes background work explicit and testable; avoids hidden side effects.
- **sheets/:** Isolates remote API and auth for easier mocking and quota tuning.
- **notifications/:** Separates ingestion parsing from UI + storage.

## Architectural Patterns

### Pattern 1: Offline-first local source of truth

**What:** All writes go to Room first; sync is eventual.
**When to use:** Any workflow with background uploads and offline capture.
**Trade-offs:** Extra schema for sync state; prevents UI stalls.

**Example:**
```kotlin
// UI layer
viewModelScope.launch {
    repository.addTransaction(input)
    syncScheduler.enqueueUpload()
}
```

### Pattern 2: Idempotent uploads with stable client IDs

**What:** Every transaction has a stable UUID and optional remote row mapping.
**When to use:** To avoid duplicates from retries or network loss.
**Trade-offs:** Requires mapping table and merge rules.

**Example:**
```kotlin
val tx = Transaction(id = UUID.randomUUID().toString(), status = PENDING)
dao.insert(tx)
// Worker uploads tx.id as a column, then stores rowId mapping locally.
```

### Pattern 3: Categorization feedback loop

**What:** Pull category updates from Sheets and apply to local records.
**When to use:** Remote sheets are the final categorization system.
**Trade-offs:** Needs conflict rules when local edits happen offline.

**Example:**
```kotlin
val updates = sheetsClient.fetchCategoryUpdates(sinceRow)
repository.applyRemoteCategories(updates)
```

## Data Flow

### Request Flow

```
[Notification or Manual Entry]
    ↓
[Capture/Review UI] → [Repository] → [Room DB]
    ↓                    ↓
[UI renders]       [Sync Scheduler]
                         ↓
                   [WorkManager Worker] → [Sheets API]
                         ↓                      ↓
                 [Upload Ack + Row Mapping] ← [Sheet Row]
                         ↓
                   [Room DB update]
```

### State Management

```
[Room/Flow]
    ↓ (collect)
[ViewModels] ←→ [UI Events] → [Repository] → [DAO]
```

### Key Data Flows

1. **Notification ingestion:** Listener parses notification → repository writes draft transaction → UI shows pending list.
2. **Manual entry:** User adds entry → stored locally → sync worker enqueued.
3. **Upload + mapping:** Worker batches pending rows → Sheets API append/update → store rowId/etag.
4. **Category feedback:** Worker pulls category column changes → reconcile local categories → UI updates.
5. **Correction loop:** User corrects category → mark dirty → worker updates specific row.

## Build Order Implications

1. **Local data model + Room schema** (transactions, sync state, row mapping).
2. **Ingestion sources** (NotificationListener + manual entry UI) writing to Room.
3. **Repository + sync queue** to centralize writes and enqueue work.
4. **WorkManager workers + Sheets client** (upload first, then pull categories).
5. **Reconciliation rules** (conflicts, offline edits, category corrections).

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-10k rows | Single-sheet append with periodic batch updates. |
| 10k-100k rows | Use batch updates, paging reads, and index on rowId in local DB. |
| 100k+ rows | Split by month/year sheets and use background partial sync windows. |

### Scaling Priorities

1. **First bottleneck:** Sheets API quota/latency → batch updates + fewer reads.
2. **Second bottleneck:** Local query speed → indices on date/category/status.

## Anti-Patterns

### Anti-Pattern 1: UI writes directly to Sheets

**What people do:** Call Sheets API from UI on every entry.
**Why it's wrong:** UI freezes, failures cause data loss, no retries.
**Do this instead:** Write locally first; sync via WorkManager.

### Anti-Pattern 2: No idempotency or row mapping

**What people do:** Append on every retry with no unique ID.
**Why it's wrong:** Duplicate rows and broken reconciliation.
**Do this instead:** Store stable IDs and map to row IDs after upload.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Google Sheets API | REST client with OAuth scopes | Batch updates, A1 ranges, quotas. |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| UI ↔ Repository | Kotlin calls + Flow | UI never talks to Sheets directly. |
| Repository ↔ Room | DAO/Flow | Local source of truth. |
| Repository ↔ Sheets | Client interface | Hide auth + retry logic. |
| Worker ↔ Repository | Direct calls | Workers reuse repo for consistency. |

## Sources

- https://developers.google.com/workspace/sheets
- https://github.com/androidx/androidx/tree/androidx-main/work
- https://github.com/androidx/androidx/tree/androidx-main/room
- https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/service/notification/NotificationListenerService.java

---
*Architecture research for: Android expense tracking with Sheets sync*
*Researched: 2026-01-25*
