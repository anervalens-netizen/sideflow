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
- Presets render coherently in light/dark modes.
- Custom values survive restart.
- Appearance changes do not materially regress panel-open latency or scrolling.

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
