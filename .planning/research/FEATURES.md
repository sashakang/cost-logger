# Feature Research

**Domain:** Personal expense tracking with Google Sheets-backed workflow (Android app + Sheets)
**Researched:** 2026-01-25
**Confidence:** MEDIUM

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Category assignment in the sheet (dropdown/select) | Spreadsheet workflows expect category selection directly in the Transactions sheet | LOW | Tiller categorization uses the Category column; Google Sheets supports dropdowns via data validation. Source: https://help.tiller.com/en/articles/1886250-categorizing-transactions, https://support.google.com/docs/answer/186103?hl=en |
| Customizable category list | Users need a personalized taxonomy for budgeting | MEDIUM | Tiller supports editing Categories sheet with groups/types; implies category list is user-owned. Source: https://help.tiller.com/en/articles/3250769-customizing-categories |
| Split a single transaction across multiple categories | Common for mixed purchases (e.g., groceries + household) | MEDIUM | Tiller provides manual and add-on assisted splitting. Source: https://help.tiller.com/en/articles/581912-splitting-transactions-between-multiple-categories |
| Rule-based auto-categorization | Users expect recurring merchants to auto-fill categories | MEDIUM | AutoCat rules with criteria and overrides for Categories and other columns. Source: https://help.tiller.com/en/articles/3792984-how-to-use-autocat-for-automatic-categorization |
| Category hints or prefilled suggestions | Users expect some guidance from merchant metadata | MEDIUM | Tiller exposes Category Hint and supports mapping to custom categories. Source: https://help.tiller.com/en/articles/10343918-using-tiller-s-category-hints |
| Structured columns to support analysis (pivot-friendly tables) | Sheets-based workflows rely on pivots/filters for reporting | MEDIUM | Google Sheets pivot tables depend on headered tables. Source: https://support.google.com/docs/answer/1272900?hl=en |
| Two-way edit workflow (app edits reflected in sheet, sheet edits respected) | Hybrid categorization requires both app and sheet to stay in sync | HIGH | Domain expectation for hybrid workflows; not directly sourced. Mark for validation. |
| Manual entry and correction of transactions | Any expense tracker must allow corrections and manual adds | MEDIUM | General expense-tracking expectation; not directly sourced. Mark for validation. |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Hybrid categorization UI (bulk review in app + rules in Sheets) | Faster clean-up while keeping Sheets as source of truth | HIGH | Aligns with project goal of hybrid categorization; requires conflict handling and sync state. |
| Rule builder tied to recent edits (create rule from selection) | Reduces friction to create recurring rules | MEDIUM | AutoCat includes rule builder from selection and rule order management. Source: https://help.tiller.com/en/articles/3792984-how-to-use-autocat-for-automatic-categorization |
| Auto-run rules on new rows (background categorize) | Minimizes manual touch on recurring items | MEDIUM | AutoCat supports Auto Run on Fill for new transactions. Source: https://help.tiller.com/en/articles/3792984-how-to-use-autocat-for-automatic-categorization |
| Category mapping from vendor hints to custom taxonomy | Faster onboarding of categories while staying consistent | MEDIUM | Tiller provides Category Hint and mapping via AutoCat rules. Source: https://help.tiller.com/en/articles/10343918-using-tiller-s-category-hints |
| AI-assisted suggestions (optional, reviewable) | Increases accuracy without fully automating | HIGH | Tiller's AI Suggest exists as an optional Beta; adopt cautiously. Source: https://help.tiller.com/en/articles/9916508-using-ai-suggest-beta-for-autocat-to-automatically-categorize-transactions |
| Template-ready reporting sheets (prebuilt pivots/charts) | Makes Sheet analytics "ready to use" without setup | MEDIUM | Pivot tables supported in Sheets; add templates for trend/ category views. Source: https://support.google.com/docs/answer/1272900?hl=en |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Fully automatic categorization with no review | "Set it and forget it" appeal | Even Tiller avoids default auto-categorization due to accuracy/intent issues | Provide rule-based automation + review queue | 
| Free-text categories in app that bypass sheet taxonomy | Quick entry in app | Breaks reporting consistency in Sheets; hard to reconcile | Enforce category list sync + allow "Uncategorized" bucket |
| Real-time, always-on reprocessing of the entire sheet | Users want everything up to date | Risk of overwriting manual corrections and high latency | Run rules on new rows + explicit reprocess option |

## Feature Dependencies

```
[Custom category list]
    └──requires──> [Category dropdowns in sheet]
                       └──requires──> [Two-way sync of category list]

[Rule-based auto-categorization]
    └──requires──> [Structured transaction schema + stable columns]

[Category hint mapping]
    └──requires──> [Category hints column] ──requires──> [Rules engine]

[Split transactions]
    └──requires──> [Multi-row representation or split metadata]

[Hybrid app review]
    └──requires──> [Sync status + conflict detection]
```

### Dependency Notes

- **Custom category list requires category dropdowns:** The sheet must enforce categories to keep reporting consistent.
- **Rules engine requires structured columns:** AutoCat-style rules target specific columns (e.g., Description, Amount).
- **Category hints require rules mapping:** Hints only help when mapped into the taxonomy.
- **Split transactions requires a representation standard:** Either multi-row splits or explicit split metadata must be supported.
- **Hybrid review requires conflict handling:** The app must detect if the sheet changed after a local edit.

## MVP Definition

### Launch With (v1)

Minimum viable product - what's needed to validate the concept.

- [ ] Two-way sync for category and comment edits - proves hybrid workflow viability
- [ ] Category list management in Sheets + in-app dropdown - avoids taxonomy drift
- [ ] Manual categorization and "uncategorized" queue - supports steady cleanup
- [ ] Basic rules support (simple "description contains" -> category) - reduces repetitive work
- [ ] Split transaction support (manual) - necessary for real-world purchases

### Add After Validation (v1.x)

Features to add once core is working.

- [ ] Rule builder from recent transactions - accelerate rule creation
- [ ] Auto-run rules on new rows - reduce manual effort
- [ ] Category hint mapping - faster onboarding for new merchants

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] AI-assisted categorization - high complexity and requires trust/controls
- [ ] Prebuilt analytics templates (trend, cashflow, budget) - nice-to-have once core is solid

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Two-way category/comment sync | HIGH | HIGH | P1 |
| Category list + dropdown enforcement | HIGH | MEDIUM | P1 |
| Manual categorization queue | HIGH | MEDIUM | P1 |
| Simple rules (description contains) | HIGH | MEDIUM | P1 |
| Split transactions | MEDIUM | MEDIUM | P2 |
| Rule builder from selection | MEDIUM | MEDIUM | P2 |
| Auto-run rules on new rows | MEDIUM | MEDIUM | P2 |
| Category hint mapping | MEDIUM | MEDIUM | P2 |
| AI-assisted categorization | MEDIUM | HIGH | P3 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | Competitor A | Competitor B | Our Approach |
|---------|--------------|--------------|--------------|
| Manual categorization in sheet | Tiller uses Category column dropdown | DIY Sheets trackers use dropdowns via data validation | Keep Categories sheet + dropdown validation synced with app |
| Rule-based auto-categorization | Tiller AutoCat rules + Auto Run | DIY Sheets templates often use formulas or scripts (LOW confidence) | Provide simple rules + optional automation |
| Split transactions | Tiller supports manual and add-on splits | DIY Sheets trackers often use multi-row splits (LOW confidence) | Support manual split representation in app + sheet |
| Category hints | Tiller exposes Category Hint and mapping | DIY Sheets trackers usually don't have external hints (LOW confidence) | Use hints when available, always reviewable |

## Sources

- https://help.tiller.com/en/articles/1886250-categorizing-transactions
- https://help.tiller.com/en/articles/3250769-customizing-categories
- https://help.tiller.com/en/articles/581912-splitting-transactions-between-multiple-categories
- https://help.tiller.com/en/articles/3792984-how-to-use-autocat-for-automatic-categorization
- https://help.tiller.com/en/articles/10343918-using-tiller-s-category-hints
- https://help.tiller.com/en/articles/9916508-using-ai-suggest-beta-for-autocat-to-automatically-categorize-transactions
- https://support.google.com/docs/answer/186103?hl=en
- https://support.google.com/docs/answer/1272900?hl=en

---
*Feature research for: personal expense tracking with Google Sheets workflows*
*Researched: 2026-01-25*
