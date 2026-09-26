# SideFlow Minimal

SideFlow is a compact right-edge Android sidebar for launching installed apps from two-column sections. Tap its thin handle to open; tap an app to launch it. The launcher activity provides Start/Stop, overlay setup, compact Handle controls (height and vertical position), and searchable Add/Remove. A persistent notification also has Stop.

Version 0.2.2 (code 11) keeps application ID `eu.astancu.sideflow` for an in-place update. The edge touch depth is reduced from 30dp to 16dp to reduce accidental activation while typing. Existing section and shortcut IDs, labels, order, and package targets migrate on first load. Unsupported legacy shortcuts stop migration with a visible error. Legacy preferences remain preserved. The older recovery code9 is not an in-place downgrade from current code11; see [rollback prerequisites](docs/ROLLBACK.md).

The owner-installed baseline is the same-certificate **debug variant**. Core delivery and the close-flicker fix are accepted; the signed R8 release, recovery refresh and remaining physical/performance qualification are explicitly tracked in [issue #10](https://github.com/anervalens-netizen/sideflow/issues/10). See [delivery state](docs/DELIVERY_20260925.md). No RAM, battery or frame-time improvement is claimed without measurements.

Build with JDK 17 and Android SDK 34:

```bash
./gradlew --no-daemon --max-workers=2 testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug lintRelease
```

Android instrumentation tests require a device or emulator. A release update requires the original signing certificate; local builds without signing properties produce an unsigned release APK. See [minimal architecture and migration](docs/SIDEFLOW_MINIMAL.md).

SideFlow derives from [Smart Edge](https://github.com/Imtiaz-Official/Smart-Edge) by Imtiaz. The original and derivative work retain the [MIT license](LICENSE).
