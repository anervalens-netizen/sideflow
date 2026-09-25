# SideFlow Minimal

SideFlow is a compact right-edge Android sidebar for launching installed apps from two-column sections. Tap its thin handle to open; tap an app to launch it. The launcher activity provides Start/Stop, overlay setup, and searchable Add/Remove. A persistent notification also has Stop.

Version 0.2.0 (code 8) keeps application ID `eu.astancu.sideflow` for an in-place update. Existing section and shortcut IDs, labels, order, and package targets migrate on first load. Unsupported legacy shortcuts stop migration with a visible error. Legacy preferences remain available for rollback.

Build with JDK 17 and Android SDK 34:

```bash
./gradlew --no-daemon --max-workers=2 testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug lintRelease
```

Android instrumentation tests require a device or emulator. A release update requires the original signing certificate; local builds without signing properties produce an unsigned release APK. See [minimal architecture and migration](docs/SIDEFLOW_MINIMAL.md).

SideFlow derives from [Smart Edge](https://github.com/Imtiaz-Official/Smart-Edge) by Imtiaz. The original and derivative work retain the [MIT license](LICENSE).
