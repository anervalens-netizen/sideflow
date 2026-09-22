# SideFlow v1 Plan

Status: Active
Canonical tracker: GitHub Issue #1 (created/maintained from this plan)
Base: Smart Edge by Imtiaz, MIT licensed

## Product goal

SideFlow is a personal-first Android edge shelf: swipe from an edge, get a compact configurable panel, and launch apps or saved shortcuts quickly. The primary interaction is normal full-screen launch. Freeform/split-screen is optional secondary functionality.

The public repository contains generic source code only. Personal app selections, conversation URLs, account data, screenshots containing private content, signing material, and device-specific runtime configuration stay outside Git.

## Product shape

The v1 panel is intentionally simple:

- compact configurable panel width/height;
- 2–6 columns;
- configurable icon size and spacing;
- grouped sections with optional headers;
- item types: app, shortcut/deep-link, URL, folder/group, system action;
- normal tap launches full-screen;
- long-press/edit mode handles secondary actions and reordering;
- glass/AMOLED/Material You appearance presets;
- configuration stored locally on-device;
- import/export with a versioned schema.

The intended organization supports a small number of practical sections such as “My Apps”, “Daily”, and “Chats”, without hard-coding any owner-specific apps or links.

## Current execution status — 2026-09-22

- P0 bootstrap: merged.
- P1 core launcher/grid: merged; physical OnePlus validation remains pending.
- P2 sectioned shelf: merged.
- P3 fast shortcuts/share target: merged.
- P4 appearance: code-complete locally on `feat/sideflow-p4-appearance-presets`; R0 cleared on `bdb931ee`; final exact-head review pending, not merged.
- P5 backup/portability: not started as a phase.
- P6 device/release validation: pending Android Remote Control MCP / physical device access.
- **Mandatory Codex review remediation gate R0 is CLEARED:** reviewed head `bdb931ee` had zero unresolved threads, exact-head CI `35760943007` green, and the fresh Codex review reported no major issues. The final P4 code batch still requires its own exact-head review before merge.

## Mandatory gate R0 — Codex Connector remediation

Before P4 can merge:

1. Resolve every applicable finding in `docs/reviews/CODEX_REVIEW_REMEDIATION_20260922.md`.
2. Prioritize P1 privacy/data-loss findings, then P2 correctness.
3. Add focused regression tests where the behavior is pure/model-driven.
4. Run `testDebugUnitTest assembleDebug`.
5. Request a fresh Codex Connector review on the remediation PR.
6. Reconcile every new actionable finding before merge.
7. Update GitHub Issue #1 and ContextKeep with evidence.

This gate exists because green CI alone did not catch several valid cross-feature/privacy regressions in P0–P3.

## Phase P0 — Bootstrap / ownership

### Work
- Keep Smart Edge history and MIT license.
- Maintain upstream remote for optional cherry-picks/reference.
- Rebrand application to SideFlow.
- Use an independent application ID.
- Make release signing optional for debug/CI builds.
- Add public GitHub Actions CI for unit tests + debug APK.
- Add privacy/source-control guardrails.
- Establish a clean reproducible baseline build.

### Definition of done
- Fresh checkout builds testDebugUnitTest and assembleDebug with no secrets.
- CI passes on exact commit.
- MIT notice remains intact.
- No owner-private data exists in Git history added by SideFlow.

## Phase P1 — Core launcher behavior

### Work
- Tap path uses normal Android launch behavior and does not request freeform/windowing mode.
- Freeform/split-screen becomes explicit secondary behavior only.
- Add 2–6 column selection.
- Make icon size, spacing, panel max width and panel max height independently configurable.
- Preserve configurable edge trigger location/size.
- Validate Android 15/16 background activity launch behavior while overlay is visibly open.

### Definition of done
- Standard app tap opens the target app as a normal full-screen task on the target OnePlus device.
- Layout settings persist and are bounded.
- Unit tests cover launch-mode decisions and preference bounds.
- No hidden API is required for ordinary launch.

## Phase P2 — Sectioned shelf

### Work
- Introduce a versioned local shelf schema.
- Support ordered sections.
- Per-section title visibility and grid columns.
- Drag/reorder items within and between sections.
- Generic item model for app / deep-link / URL / folder / system action.
- Migrate legacy flat pinned-app data without loss.

### Definition of done
- Section state survives process death/restart.
- Migration from legacy preferences is deterministic and tested.
- Edit mode supports section and item reordering.
- Source contains only generic sample/default data.

## Phase P3 — Fast shortcut flow

### Work
- Add generic link/deep-link items.
- Editable display title and icon.
- Add a fast “Add to SideFlow” path using Android share/intent mechanisms where practical.
- Validate common app deep-links, including conversation URLs, without relying on undocumented account data.

### Definition of done
- A saved link can be added, renamed, reordered and launched.
- Link values are stored only in local app data unless the owner explicitly exports them.
- No conversation URL is committed to the repository.

## Phase P4 — Appearance

### Work
- Smoke Glass preset.
- Frosted Light preset.
- AMOLED preset.
- Material You preset.
- Custom opacity, blur, corner radius, icon size and spacing.
- Subtle section cards and optional section titles.

### Definition of done
- Presets render coherently in light/dark modes. **Code complete; device visual validation pending.**
- Custom values survive restart. **Implemented via persistent preferences/import sanitization.**
- Appearance changes do not materially regress panel-open latency or scrolling. **Hot-path contrast reads were reduced and section cards use a RecyclerView decoration rather than nested lists; device validation pending.**

## Phase P5 — Backup / portability

### Work
- Versioned export/import format.
- Validation and migration.
- Explicit handling of user-private shortcut data.
- Safe failure for corrupt/incompatible files.

### Definition of done
- Export/import round-trip is automated/tested.
- Corrupt or unsupported input fails without destroying current state.
- Export UI makes clear that exported shortcut URLs may be private.

## Phase P6 — Device validation / release

### Work
- Validate on OnePlus Nord 4 / current OxygenOS.
- Validate overlay permission, service lifecycle, autostart and battery handling.
- Keep release keystore outside Git.
- Produce signed v1 APK and release notes.

### Definition of done
- Target-device smoke test passes.
- Exact-head CI passes.
- APK installs/upgrades cleanly.
- Release notes identify upstream attribution and SideFlow-specific changes.

## Engineering constraints

1. MIT license and upstream attribution remain.
2. No personal URLs, account identifiers, screenshots of private content, local settings, tokens, credentials or signing material in public Git.
3. Standard Android APIs first. Shizuku/hidden APIs may remain optional but are not dependencies for ordinary launch.
4. Avoid rewrites purely for novelty. Modernize where it improves Android 15/16 correctness, maintainability or UX.
5. GitHub Issue #1 is the canonical tracker; this document is the implementation plan.
