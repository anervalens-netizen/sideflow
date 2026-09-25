# SideFlow Minimal 0.2.1

## Architecture

One Android module uses Kotlin Views and RecyclerView. `MainActivity` is the small manager. `FloatingPanelService` owns the persistent notification, thin right handle, and reusable panel. `PanelGrid` draws one subtle grey rounded card per section behind a two-column grid. The panel uses the accepted dark grey 90% opacity and 28dp radius. The handle has a thin visible stripe in a transparent 30dp touch area, with bounded 115dp height and 214dp placement. On API 29+, only that touch area is excluded from system edge gestures. `SidebarStyle` centralizes the accepted 180dp by at most 700dp panel, section spacing, handle placement, and short transition. All window sizes clamp to the available viewport; API 26–29 use the display-size fallback.

`LauncherRepository` queries launchable apps only when Add opens. Normal sidebar opening attaches the existing panel; it does not rebind the grid, scan packages, or read shelf files. Icon work can finish while the panel is hidden. Selected icons render off the main thread at display size into an 8MiB byte-weighted cache. Package broadcasts invalidate only selected targets. App launch uses the package's launcher intent, including WebAPK packages. Missing apps stay in the shelf with a placeholder and a tap message. No notification listener, polling, alarms, or broad package query is used.

`RuntimeState` records durable user enablement and a boot marker. Stop saves false before removing the service. Same-boot recovery can use the retained legacy-named accessibility component, which does not retrieve window content or act on events. A new boot requires legacy `auto_start` (default true) and enabled state. The manager exposes a Retry button when enabled but the service is absent. A successful overlay creation records the current boot marker so subsequent same-boot process recovery is possible. The old `toggle_sidebar` shortcut is disabled once because its target activity was removed. Accessibility retention was observed during the actual in-place update. The complete OxygenOS Clear-All/reboot matrix remains open in issue #10.

## Shelf migration and recovery

`ShelfStore` reads `side_panel_prefs/shelf_config_v1` once when no minimal store exists. The legacy format is version 1; the minimal format is version 2. The codec validates the entire document and rejects unsupported versions/types, missing or duplicate IDs, malformed arrays/objects, invalid package references, wrong JSON types, custom icon references, and oversized input. It preserves section/item IDs, titles/labels, order, and app package targets. A valid empty shelf or empty section stays empty. Unsupported legacy data blocks migration and leaves the source untouched.

The minimal store uses `AtomicFile` for current and last-known-good copies. The good copy is written before the first primary write. Later edits save the prior validated snapshot as good before replacing current. Pending AtomicFile `.bak` files count as recoverable state; migration never reseeds over damaged minimal data. File operations and edits share one serialized worker. Editor callbacks ignore destroyed or superseded activities. The legacy preference file is never rewritten by the minimal implementation. Local copies do not survive uninstall or data wipe.

Rollback requires a previous code build with a higher versionCode and the same signing certificate; the existing recovery code9 cannot normally replace installed 0.2.1/code10 in place. The version 1 preferences remain for that path. Back up device data before changing installed builds.

## Removed capabilities

The old product code for Shizuku, hidden APIs, notification tracking, Quick Settings tile, dashboard tools, screenshot, flashlight, RAM/temperature/audio controls, windowing, nested folders, URL/deep-link shortcuts, icon packs/themes/presets, all-activities discovery, broad import/export, and duplicate settings screens is removed. The public repository contains synthetic test fixtures only.

## Verification and gates

Run `testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug lintRelease` with JDK 17 and SDK 34. Unit tests cover strict migration, order, empty and bounded inputs, recovery decisions, geometry, and icon generation. Android tests use isolated synthetic storage for store migration, atomic recovery, persistence, and failure behavior; grid and queue tests check spans, section decoration, and delivery of committed edits after an editor closes. Twenty concurrent synthetic edits exercise serialization. Instrumentation APK build does not execute Android tests. CI uploads APKs, lint text, and unit XML for seven days. Local unsigned release output is not update-installable.

Current lint warnings are triaged as retained target 34 compatibility, inherited launcher icon assets, intentional fixed right edge, code-only custom view construction, and full RecyclerView refresh on actual shelf replacement. No error category is suppressed.

Owner delivery on 25 September: matching installed/candidate certificate verified, in-place 0.1.6/code7 -> 0.2.0/code8 -> 0.2.1/code10 installed, four sections/18 targets preserved, Accessibility and overlay retained, foreground runtime active. Actual opening and outside-close were inspected; after the close-flicker fix the owner responded "ok" and authorized closeout. The installed artifact is debug-signed with the original certificate, not a signed R8 release.

Issue #10 retains unfinished qualification: every target launch, the complete rapid-cycle/edit/Back/rotation/IME/font/revocation/recovery/Clear-All/reboot matrix on OnePlus, signed R8 delivery and new recovery artifact compatible with code10. Measure frame timing, memory and battery under matched conditions. Tests on emulators and source/APK-size reductions are not substitutes for those measurements. No further phone action was required for this documentation closeout.
