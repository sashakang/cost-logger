# Implementation Plan: Transaction Form UI Fix

## Problem
Current `TransactionEntrySheet` uses `ModalBottomSheet` which:
- Places form at bottom of screen
- Category dropdown menu extends below screen boundary
- Cannot select categories due to overflow

## Proposed Solution
Convert from ModalBottomSheet to full-screen composable:

1. **UI Structure Change**
   - Replace `ModalBottomSheet` with full-screen `Box` overlay
   - Add `Scaffold` with `TopAppBar` for navigation
   - Back button in TopAppBar calls `onDismiss`

2. **Scrollability**
   - Add `rememberScrollState()`
   - Apply `verticalScroll()` modifier to Column
   - Ensures all dropdowns are accessible

3. **Amount Field Fix**
   - Add `singleLine = true` to amount OutlinedTextField

4. **Integration**
   - Keep existing function signature with callbacks
   - No MainActivity changes needed
   - Works with existing state-based triggering (`showTransactionSheet`)

## Architecture Analysis

**Complexity**: Medium (20-30 line changes in 1 file)

**Alternatives Considered**:
1. Fix ModalBottomSheet with skipPartiallyExpanded - Doesn't address "form at top" requirement
2. Convert to navigation destination - Over-engineering; requires MainActivity changes
3. Full-screen overlay with callbacks - CHOSEN: Solves problem with minimal changes

**Why This Approach**:
- Solves all stated problems
- Preserves existing integration
- No MainActivity business logic changes
- Can be converted to navigation later if needed
- Avoids premature abstraction
