# SideFlow

SideFlow is a personal-first Android edge shelf for fast access to apps, shortcuts and small groups of frequently used actions.

It is derived from [Smart Edge](https://github.com/Imtiaz-Official/Smart-Edge) by Imtiaz and keeps the original MIT license and project history. SideFlow has its own application ID, roadmap and release lifecycle.

## Direction

SideFlow v1 focuses on:

- normal full-screen app launching by default;
- configurable compact grids;
- grouped sections;
- app, shortcut/deep-link, URL, folder and system-action items;
- glass/AMOLED/Material You themes;
- device-local personal configuration;
- versioned import/export.

The active implementation plan is in [docs/plans/SIDEFLOW_V1.md](docs/plans/SIDEFLOW_V1.md). GitHub Issue #1 is the canonical progress tracker.

## Privacy boundary

This repository is public. Personal configuration is not source-controlled. Conversation URLs, account data, owner-specific exports, screenshots containing private content, credentials and signing material must stay outside Git. See [docs/PUBLIC_REPO_PRIVACY.md](docs/PUBLIC_REPO_PRIVACY.md).

## Build

Requirements:

- JDK 17+
- Android SDK
- Android API 34 SDK installed

Debug/test build:

    ./gradlew testDebugUnitTest assembleDebug

Release signing is optional at configuration time. A release keystore is supplied externally through keystore.properties or CI secrets and is never committed.

## Upstream

Upstream: Smart Edge by Imtiaz

License: MIT — see [LICENSE](LICENSE)

SideFlow preserves the original copyright notice as required by the MIT license.
