# Codex Connector Review Remediation — 2026-09-22

Status: **IMPLEMENTED LOCALLY — fresh Codex review required before P4 merge**
Canonical tracker: GitHub Issue #1  
Reviewed PRs: #2, #3, #4, #5  
Source: `chatgpt-codex-connector[bot]` GitHub reviews/comments.

## Why this exists

The earlier implementation merged P0–P3 before all Codex Connector review comments had been reconciled. This document is the canonical reconciliation record. No later phase may be declared complete merely because CI is green; every applicable Codex finding below must be either fixed with evidence or explicitly documented as not applicable/superseded.

As of 2026-09-22, **21 findings were inspected**. The original 1 resolved finding plus all 20 actionable findings now have local remediation on feat/sideflow-p4-appearance-presets. The R0 gate remains open until the fresh Codex Connector review and exact-head CI are reconciled.

## PR #2 — bootstrap/rebrand

| ID | Severity | Finding | Current assessment | Required action |
| --- | --- | --- | --- | --- |
| r4069196518 | P2 | Migrate legacy `smartedge.*` pseudo-item IDs when importing old backups | **RESOLVED (local)** | Translate legacy shortcut/folder/tool IDs to `sideflow.*` during import/migration and test it. |
| r4069196526 | P1 | Inherited donation/payment accounts remain under SideFlow branding | **RESOLVED (local)** | Remove the inherited payment destinations from SideFlow or clearly separate them as upstream-only. Preferred owner-app action: remove donation UI entirely and keep upstream attribution only. |
| r4069196529 | P2 | In-app repository action still pointed upstream | **RESOLVED** | Current `SettingsMainActivity.openGithub()` opens `anervalens-netizen/sideflow`. Keep upstream attribution separately. |
| review 5275031311 | P2 | Generated `sideflow_backup_<timestamp>.json` is not ignored | **RESOLVED (local)** | Add `sideflow_backup_*.json` / equivalent generated export pattern to `.gitignore`. |

## PR #3 — launcher/grid

| ID | Severity | Finding | Current assessment | Required action |
| --- | --- | --- | --- | --- |
| r4069338335 | P2 | Narrow panel + many columns can make icons overlap/clip | **RESOLVED (local)** | Fit icon size to actual cell width or derive/enforce a width/columns minimum; add pure layout-policy tests. |
| r4069338343 | P2 | Center-drop freeform ignores configured freeform size mode | **RESOLVED (local)** | Reuse the existing Standard/Portrait/Maximized/Custom bounds policy in the center-drop path. |
| r4069338350 | P2 | Center-drop freeform is not gated on platform freeform availability | **RESOLVED (local)** | Use `isFreeformEnabled()`/policy and degrade to fullscreen when unavailable. |
| r4069338356 | P2 | Imported panel width can violate slider step (e.g. 185dp) | **RESOLVED (local)** | Quantize stored/imported width to the 10dp slider step or make slider continuous; add tests. |
| r4069338375 | P2 | Fullscreen launch in `SplitScreenHelper` sits outside error handling | **RESOLVED (local)** | Put fullscreen `startActivity` inside guarded/fallback launch handling. |

## PR #4 — sectioned shelf

| ID | Severity | Finding | Current assessment | Required action |
| --- | --- | --- | --- | --- |
| r4070244969 | P1 | Empty configured shelf is treated as uninitialized and overwritten with top apps | **RESOLVED (local)** | Seed default apps only if the shelf config has never existed; preserve intentionally empty sections/config. |
| r4070244975 | P2 | Downward drop on a later section header is off by one after source removal | **RESOLVED (local)** | Recompute/adjust target after source removal and add ordering regression test. |
| r4070244980 | P2 | Nested folders lose resolvable child identity | **RESOLVED (local)** | Make folder lookup recursive and preserve nested folder item IDs during resolution. |
| r4070244988 | P2 | Section headers collapse to height 0 in compact/edit mode, breaking empty-section drops | **RESOLVED (local)** | Keep a visible/nonzero drop zone while editing even when title text is hidden. |
| r4070244994 | P2 | Imported shelf can contain duplicate nonblank item IDs | **RESOLVED (local)** | Enforce globally unique IDs during normalization; deterministically regenerate collisions and test move/remove semantics. |
| r4070245000 | P2 | Legacy migration ignores previous global column count | **RESOLVED (local)** | Seed migrated first section from saved `panelColumns`, not hard-coded default. |
| r4070245009 | P2 | Appearance “Panel Columns” control no longer affects persisted per-section columns | **RESOLVED (local)** | Retarget it as a bulk “all sections” action or remove/hide it; per-section editor stays authoritative. |

## PR #5 — shortcuts/privacy

| ID | Severity | Finding | Current assessment | Required action |
| --- | --- | --- | --- | --- |
| r4070389396 | P2 | Shortcut section/icon selection is lost on Activity recreation | **RESOLVED (local)** | Save/restore pending selection in `savedInstanceState`; test pure state where practical. |
| r4070389408 | P2 | URI normalization strips valid trailing URI characters | **RESOLVED (local)** | Stop mutating manually entered valid URIs; trim punctuation only in shared-text extraction using context-aware rules. Include Wikipedia-parenthesis regression. |
| r4070389414 | P2 | Export does not warn that shortcut targets are written to plaintext Downloads | **RESOLVED (local)** | Add explicit confirmation/warning before export when shelf contains URL/deep-link targets. |
| r4070389421 | P1 | Android Auto Backup can upload private shortcut targets | **RESOLVED (local)** | Disable app Auto Backup or explicitly exclude the preference storage containing shelf targets. For this owner-first app, `allowBackup=false` is the simplest acceptable policy unless P5 replaces it with a scoped rule. |
| r4070389428 | P2 | Drag-to-split for URL/deep-link loses the actual target and may launch icon-source app | **RESOLVED (local)** | Preserve the full shortcut intent through drag, or disable split/freeform drag for items with `intentUri`. Preferred v1: restrict drag-windowing to plain APP items. |


## R0 local remediation evidence — 2026-09-22

The fixes below are implemented on the P4 branch but **do not authorize merge**. Fresh Codex Connector review remains mandatory.

| Finding(s) | Evidence |
| --- | --- |
| r4069196518 | ShelfConfigOps migrates legacy smartedge folder/tool/shortcut IDs; legacy migration preserves saved global columns; regression in ShelfConfigTest. |
| r4069196526 | SideFlow payment/donation UI and inherited destination-bearing SupportActivity/layout removed; unused legacy settings donation layout removed; upstream attribution/license left intact. |
| review 5275031311 | Git ignore now covers generated SideFlow backup JSON files. |
| r4069338335 | SideFlowPolicy fits icons to actual panel/column cell width; regression covers 180dp / 6-column case. |
| r4069338343, r4069338350 | All freeform launch paths share one bounds policy; center-drop requires actual freeform availability and otherwise falls back fullscreen. |
| r4069338356 | Panel width sanitization now clamps and quantizes to the 10dp slider step; regression covers off-step imports. |
| r4069338375 | SplitScreenHelper routes fullscreen and windowed failures through guarded launch/fallback logic. |
| r4070244969 | Shelf configuration presence is distinct from emptiness; plain reads no longer persist a fresh empty shelf before initial seeding. |
| r4070244975 | Header-drop insertion accounts for source removal; upward/downward regression added. |
| r4070244980 | Shelf item lookup is recursive and nested folder children retain stable persisted IDs during resolution. |
| r4070244988 | Hidden-title section headers retain a visible/nonzero edit-mode drop zone. |
| r4070244994 | Normalization enforces globally unique, nonblank deterministic item IDs including nested children; idempotence/uniqueness regression added. |
| r4070245000 | Legacy first section inherits saved panel column count. |
| r4070245009 | Appearance control is now All section columns and updates every persisted section. |
| r4070389396 | Shortcut editor saves/restores pending item, section, icon-source, and autofill state across Activity recreation. |
| r4070389408 | Manual URI normalization no longer strips valid trailing URI characters; shared-text cleanup balances delimiters; Wikipedia-parenthesis regression added. |
| r4070389414 | Export with URL/deep-link targets now requires explicit plaintext Downloads disclosure/confirmation. |
| r4070389421 | Android Auto Backup is disabled for the app, preventing backup of private shortcut preferences. |
| r4070389428 | Windowing drag is restricted to plain app entries without structured intent targets. |

### Local validation

- Forced unit-test run: **42/42 tests PASS**.
- Debug APK assembly: **PASS**.
- Git diff whitespace check: **PASS**.
- Project-wide lint remains historically non-clean; the touched-code API-level finding in SplitScreenHelper was guarded after the first lint pass. Existing unrelated manifest/translations/layout/service lint debt is outside R0.

## Mandatory remediation order

1. **Privacy / misleading UI first:** PR #2 donation UI, backup filename ignore, PR #5 Auto Backup and export warning.
2. **Data integrity:** empty shelf preservation, globally unique IDs, legacy ID/column migration.
3. **Launch correctness:** freeform availability/size, fullscreen exception handling, shortcut drag semantics.
4. **Edit correctness:** header drop index, nested folders, visible drop zones, Activity recreation.
5. **Layout correctness:** width/columns/icon fitting and width quantization.
6. **Control semantics:** fix/remove the disconnected global Panel Columns control.
7. Re-run all unit tests + `assembleDebug`, create focused regression tests for every pure-policy/data-model fix.
8. Open a remediation PR, trigger a fresh Codex Connector review, and do not merge until new findings are reconciled.
9. Only then continue/merge P4 appearance work.

## Definition of done for this review gate

- Every row above is marked RESOLVED or NOT APPLICABLE with code/test evidence.
- No P1/P2 Codex finding remains silently open.
- Public-repo privacy boundary matches actual Android backup/export behavior.
- Fresh PR Codex review has no unresolved actionable finding.
- Exact-head CI passes.
- GitHub Issue #1 and ContextKeep checkpoint are updated.
