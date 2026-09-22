# Codex Connector Review Remediation — 2026-09-22

Status: **THIRD REVIEW ROUND PENDING — round-2 findings remediated locally; fresh Codex review required before R0 closes**
Canonical tracker: GitHub Issue #1  
Reviewed PRs: #2, #3, #4, #5  
Source: `chatgpt-codex-connector[bot]` GitHub reviews/comments.

## Why this exists

The earlier implementation merged P0–P3 before all Codex Connector review comments had been reconciled. This document is the canonical reconciliation record. No later phase may be declared complete merely because CI is green; every applicable Codex finding below must be either fixed with evidence or explicitly documented as not applicable/superseded.

As of 2026-09-22, **31 Codex findings were inspected** across the historical PRs and two PR #6 review rounds. All are now remediated locally on feat/sideflow-p4-appearance-presets, but the R0 gate remains open until a fresh review of the new exact HEAD and exact-head CI are both clean.

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

## PR #6 — fresh R0/P4 review round 1

Fresh Codex review of c6350c66 completed on 2026-09-22 and produced five new actionable findings. All five are remediated locally in the next commit; none authorizes P4 merge until a subsequent Codex review is clean.

| ID | Severity | Finding | Current assessment | Remediation evidence |
| --- | --- | --- | --- | --- |
| r4070841420 | P2 | Reset All Settings can leave the shelf empty instead of restoring default apps | **RESOLVED (local)** | Reset now removes both shelf-presence keys and both reset entry points (SettingsMainActivity and legacy SettingsActivity) explicitly reseed AppRepository.getTop5Apps() before refresh. |
| r4070841425 | P2 | Narrow rich-item grid fitting ignores RecyclerView/inner horizontal padding | **RESOLVED (local)** | Fitting now uses usable grid width after 16dp RecyclerView margins and accepts layout-specific inner padding; rich 180dp/6-column regression added. |
| r4070841433 | P2 | Material You resolves static theme attributes instead of dynamic wallpaper colors | **RESOLVED (local)** | Material surface/accent resolution now uses DynamicColors.wrapContextIfAvailable(appContext) before resolving Material attributes, including service-side preference reads. |
| r4070841436 | P2 | Reset-radius mutates appearance while leaving named preset active | **RESOLVED (local)** | Radius reset now calls markCustomPreset() before changing the value. |
| r4070841439 | P1 | AMOLED stores blur 0 below Slider minimum 5 and can crash Appearance | **RESOLVED (local)** | AMOLED keeps blur disabled but stores the valid inactive minimum (5); blur persistence/readback is clamped to 5–50; regression added. |

### PR #6 round-1 local validation

- Gradle testDebugUnitTest + assembleDebug with server Android SDK: **PASS**.
- Unit tests: **44/44 PASS**.
- git diff --check: **PASS**.
- P4 remains draft/WIP; another Codex review is mandatory on the new exact HEAD.

## P4 adversarial hardening before review round 2

A focused static pass over the appearance implementation found three adjacent defects before Codex could report them on the next head. They are fixed in the same R0/P4 branch and must be included in the next exact-head review.

| Area | Finding | Remediation evidence |
| --- | --- | --- |
| Panel opacity | The Appearance “Panel Opacity” preference changed the edge handle but was not applied to the panel/picker background, so the advertised P4 custom opacity control was ineffective for the panel itself. | Resolved background colors now apply the persisted opacity multiplier; named presets set deterministic opacity values. |
| Appearance import bounds | Imported opacity, radius, icon scale and max-height values could lie outside Material Slider ranges, creating the same class of reopen crash as the AMOLED blur finding. | Getters/setters/import now sanitize every Appearance slider-bound value: opacity, blur, radius, width, gap, icon scale, panel max height and picker max height. |
| Reset Blur | Appearance exposed a Reset Blur control but the activity had no click handler. | Reset Blur now restores 15, marks a named preset Custom and refreshes the panel. Opacity edits/resets likewise mark named presets Custom because opacity is preset-defined. |

### P4 hardening validation

- Gradle testDebugUnitTest + assembleDebug: **PASS**.
- Unit tests: **46/46 PASS**.
- git diff --check: **PASS**.
- A new Codex review is required on the resulting exact HEAD before R0 can close.

## PR #6 — fresh R0/P4 review round 2

Codex reviewed c68ab48 and completed on 2026-09-22 with five additional P2 findings. All five are remediated locally in the next commit; round 3 review remains mandatory.

| ID | Severity | Finding | Current assessment | Remediation evidence |
| --- | --- | --- | --- | --- |
| r4071088694 | P2 | Legacy appearance imports can leave a stale named preset active | **RESOLVED (local)** | Imports without an appearance-preset key now mark appearance Custom when legacy appearance fields are present; pure policy regression added. |
| r4071088700 | P2 | Hidden-background mode derives dark content from an invisible light surface | **RESOLVED (local)** | Hidden background now has an explicit light-content policy; app labels/pseudo icons plus panel buttons/tool text are retinted consistently and stale clipping is cleared. |
| r4071088705 | P2 | Named preset -> Custom transition loses the preset surface color | **RESOLVED (local)** | Transition materializes the current effective preset base surface (including Material You dynamic surface) into the stored custom color before switching the marker to Custom; custom rendering uses the stored surface. |
| r4071088714 | P2 | Back from a nested folder jumps to root instead of parent | **RESOLVED (local)** | SidePanelView now reports the remaining parent folder ID after stack pop; FloatingPanelService refreshes that parent instead of unconditionally clearing currentFolderId. |
| r4071088717 | P2 | Unselected picker edit icons stay near-white on Frosted Light | **RESOLVED (local)** | PickerAdapter centralizes inactive edit-icon tint and uses a dark slate tint in light mode across full bind, payload bind and toggle paths. |

### PR #6 round-2 local validation

- Forced unit test execution after fixes: **47/47 PASS**.
- Gradle testDebugUnitTest + assembleDebug: **PASS**.
- git diff --check: **PASS**.
- A new exact-head Codex review is required; P4 remains draft/unmerged.

## PR #6 — fresh R0/P4 review round 3

Codex reviewed 9c36fad and published five additional P2 findings. All five are remediated locally in the next commit; a clean exact-head review remains mandatory before R0 can close.

| ID | Severity | Finding | Current assessment | Remediation evidence |
| --- | --- | --- | --- | --- |
| r4071312972 | P2 | Imported/persisted icon scale can be in-range but off the Slider 0.1 step | **RESOLVED (local)** | Icon scale sanitizer now clamps and quantizes to 0.1; regressions cover 1.04 -> 1.0 and 1.05 -> 1.1. |
| r4071312991 | P2 | Explicitly selecting Custom from Material You bypasses color preservation | **RESOLVED (local)** | applyAppearancePreset(Custom) now routes through the preservation helper; the helper materializes both effective surface and effective accent before switching the preset marker. |
| r4071313004 | P2 | Reset can clear shelf before lifecycle-scoped default-app query completes | **RESOLVED (local)** | Both reset entry points now query default apps first, then synchronously reset and seed with no suspension point between those mutations. Cancellation before the query completes leaves existing settings intact. |
| r4071313012 | P2 | Legacy smartedge/sideflow tools-folder alias opens as an empty persisted folder | **RESOLVED (local)** | Both smartedge.folder.tools and old sideflow.folder.tools normalize to sideflow.tool.tools as SYSTEM_ACTION, routing through the synthetic built-in Tools behavior; flat and structured migration regressions cover it. |
| r4071313019 | P2 | Nested-folder children appear draggable although their order cannot persist | **RESOLVED (local)** | Stable child IDs remain for recursive navigation, but reorder policy rejects items whose sectionId is folder:* until recursive order persistence exists; pure policy regression added. |

### PR #6 round-3 local validation

- Gradle testDebugUnitTest + assembleDebug: **PASS**.
- Unit tests: **49/49 PASS**.
- git diff --check: **PASS**.
- P4 remains draft/unmerged; another Codex review is required on the resulting exact HEAD.

## PR #6 — fresh R0/P4 review round 4

Codex reviewed 71b84f49 and completed with three additional P2 findings. All three are remediated locally in the next commit; another exact-head review is mandatory.

| ID | Severity | Finding | Current assessment | Remediation evidence |
| --- | --- | --- | --- | --- |
| r4071607776 | P2 | Material You resolves a dynamic accent but picker controls still use fixed colors | **RESOLVED (local)** | Picker edit/selection/type-toggle accent resolution now consumes resolvedPanelAccentColor() for Material You (and custom accent) instead of fixed hex tints. |
| r4071607783 | P2 | Light but highly translucent panel chooses dark content despite unknown backdrop | **RESOLVED (local)** | Contrast policy now requires both a light surface and effective alpha >= 192 before choosing dark content; hidden/low-alpha surfaces use light content. Pure policy regression added. |
| r4071607785 | P2 | Preset changing Rich -> Origin does not rebuild picker layout/holders | **RESOLVED (local)** | Picker tracks the applied layout theme; applyTheme() swaps layout manager, clears recycled holders, reattaches the adapter and recomputes height when the UI theme changes. |

### PR #6 round-4 local validation

- Gradle testDebugUnitTest + assembleDebug: **PASS**.
- Unit tests: **50/50 PASS**.
- git diff --check: **PASS**.
- P4 remains draft/unmerged; another Codex review is required on the resulting exact HEAD.

## PR #6 — fresh R0/P4 review round 5

Codex reviewed b9f3eb2 and completed with one P1 and two P2 findings. All three are remediated locally in the next commit; another exact-head review is mandatory.

| ID | Severity | Finding | Current assessment | Remediation evidence |
| --- | --- | --- | --- | --- |
| r4071774272 | P2 | Legacy-only empty reset marker suppresses default seeding after upgrade | **RESOLVED (local)** | Shelf-presence policy now treats an empty legacy-only panel-apps key as uninitialized while preserving intentional structured empty shelves; regression covers all combinations. |
| r4071774280 | P1 | Picker adapters initialize before panelPrefs and can crash service startup | **RESOLVED (local)** | AppPickerPanelView initializes PanelPreferences and repository before constructing adapters, so adapter accent initialization cannot dereference an uninitialized preference field. |
| r4071774283 | P2 | Realme renders a dark gradient but content tint can be derived from a light stored custom color | **RESOLVED (local)** | Central contrast policy now supports forced-dark rendered surfaces; Custom + Realme always chooses light content while preserving hidden/alpha rules for other themes. |

### PR #6 round-5 local validation

- Gradle testDebugUnitTest + assembleDebug: **PASS**.
- Unit tests: **51/51 PASS**.
- git diff --check: **PASS**.
- P4 remains draft/unmerged; another Codex review is required on the resulting exact HEAD.

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
