> Historical pre-minimal document. Current scope and installed state: [SideFlow minimal delivery](../DELIVERY_20260925.md). Do not resume the old feature roadmap or review gates from this file.

# HANDOVER — SideFlow Codex Remediation + P4 — 2026-09-22

## Objective

Continue `anervalens-netizen/sideflow` safely from the current P4 branch, but **do not merge P4 before reconciling the Codex Connector review backlog** documented in `docs/reviews/CODEX_REVIEW_REMEDIATION_20260922.md`.

Use:
- **Remote Control MCP exclusively** for server/git/tests/GitHub CLI/builds.
- **ContextKeep project `SideFlow`** for continuity/checkpoints.
- Canonical GitHub tracker: **Issue #1**. Do not create another tracker.

## Current repository state

- Repo: `anervalens-netizen/sideflow`
- Working copy: server `/home/andrei/work/sideflow`
- Base main: `2252cd20d9c7de71bce3d17684ebe2c055729303`
- Current branch: `feat/sideflow-p4-appearance-presets`
- WIP checkpoint commit: `80eedf1ac7de5a3c76c07ae14e3fa26875052a63`
- P0–P3 are merged.
- PR #5 P3 exact-main CI: `35712153290` SUCCESS.

## Current P4 work in this branch

P4 is **WIP, not ready to merge**. The working branch contains:
- appearance preset model/catalog;
- Smoke Glass;
- Frosted Light;
- AMOLED;
- Material You using Material theme colors;
- Custom mode;
- light/dark content tinting for panel/picker/section labels;
- appearance preset persistence/export/import hooks;
- preset unit tests.

Last local validation before handover:
- `./gradlew --no-daemon testDebugUnitTest assembleDebug` — PASS.
- Unit tests: 29/29 PASS (`AppearancePresetTest` 6, `ShelfConfigTest` 9, `ShortcutPolicyTest` 9, `SideFlowPolicyTest` 5).
- P4 visual behavior is not device-validated yet.

Do not discard/reset/stash/clean the branch. Continue from it. The checkpoint is intentionally WIP and **must not be merged as-is** because R0 Codex remediation is still open.

## Critical discovery: Codex Connector backlog

All Codex Connector reviews on PRs #2–#5 were enumerated through GitHub. There are **21 findings total**:
- **1 already resolved:** PR #2 repo link now points to SideFlow.
- **20 currently open/actionable**.
- Highest-risk open findings include:
  - inherited upstream donation/payment accounts under SideFlow branding (P1);
  - Android Auto Backup can upload private shortcut URLs (P1);
  - configured empty shelf can be overwritten with default apps (P1);
  - legacy Smart Edge pseudo IDs not migrated;
  - freeform center-drop availability/size regressions;
  - duplicate shelf IDs and nested-folder identity bugs;
  - URL/deep-link drag loses target;
  - export-to-Downloads privacy warning missing;
  - URI normalization strips valid trailing characters;
  - layout width/columns/icon collision;
  - several editor/drop/migration correctness issues.

Canonical reconciliation document:
`docs/reviews/CODEX_REVIEW_REMEDIATION_20260922.md`

## Required next sequence

1. Read:
   - `docs/reviews/CODEX_REVIEW_REMEDIATION_20260922.md`
   - `docs/plans/SIDEFLOW_V1.md`
   - this handover
   - GitHub Issue #1
2. Preserve current P4 changes.
3. Implement the Codex remediation gate in the exact order in the review document.
4. Add regression tests for pure logic/model fixes.
5. Run `testDebugUnitTest assembleDebug`.
6. Push current branch and open a remediation/P4 PR only after code is coherent.
7. Trigger/wait for Codex Connector review and reconcile every new finding before merge.
8. Only after the review gate passes: finish P4, then P5.
9. Keep phone-only validation open until Remote Control MCP Android support is available.

## Phone-only work to defer

Do not block non-phone development on:
- OnePlus Nord 4/OxygenOS overlay/gesture behavior;
- real fullscreen launch behavior;
- ChatGPT conversation-link handoff into the installed ChatGPT app;
- battery/autostart behavior;
- visual pixel-level verification of appearance presets.

## Safety/privacy invariants

- Public Git must never contain owner conversation URLs, banking/account data, private exports, credentials, tokens, keystores or screenshots with private content.
- Shortcut targets are device-local unless explicitly exported.
- P5 must make plaintext export risk explicit.
- Auto Backup behavior must be corrected before claiming local-only privacy.
- Preserve MIT/upstream attribution, but SideFlow must not present upstream payment accounts as SideFlow donations.

## Do not

- Do not reset/stash/clean the current worktree.
- Do not create a competing tracker.
- Do not mark P4 complete based on local build alone.
- Do not merge while Codex findings remain unreconciled.
- Do not claim phone behavior verified without actual phone access.
