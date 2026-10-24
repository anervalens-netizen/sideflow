# In-place rollback, without losing the latest shortcuts

This is an emergency delivery path, not an additional feature in the minimal runtime.

## Current prepared recovery — qualified 12 October 2026

Private recovery **code14**, source `54cc0fbbc2360f29caa5fa20910271ad63ccdcae`, uses the original signing certificate. The disposable API35 rehearsal passed debug13 → non-debuggable R8 release13 → recovery14, preserving the latest shelf edits, both minimal copies and the original legacy preferences; a later legacy edit survived restart. The release13 APK was subsequently installed and accepted on the physical phone on 15 October. Recovery14 has **not** been installed on that phone. See the [dated delivery record](DELIVERY_20261015.md).

Before a future recovery, freshly verify the installed version and certificate and back up owner data. Code14 is valid only if the then-installed versionCode is lower than14; otherwise rebuild the private recovery above that version. Returning from recovery14 requires a minimal version above14. Never uninstall, clear data, replace the signing key or blindly downgrade. The 15 October observation is not a fresh phone readback.

## Historical recovery code9 — qualified 25 September 2026

The private recovery artifact is built from legacy `073ba4b` plus local recovery commit `1689dad`, with the same application ID and signing certificate, versionCode **9**, versionName `0.1.6-rollback`. The rehearsed minimal 0.2.0 used code **8**. **At the September delivery, owner-installed minimal 0.2.1 used code10: that recoverycode9 was not a compatible normal in-place rollback.** Before any recovery from code10, rebuild the recovery-only bridge with a code strictly greater than the version installed then, preserve the same signing certificate, and rerun the latest-edit preservation rehearsal. The compatible code14 follow-up was completed under issue #10; its current prerequisites are above. A subsequent minimal update must exceed the newly used recovery version. Do not uninstall or clear app data, and do not use a new signing key.

The recovery-only startup bridge strictly decodes the current minimal v2 shelf and converts every section/item to legacy v1, preserving IDs, order, names, package targets and edits made after the upgrade. It keeps the minimal files untouched, backs up the original legacy preferences, and commits the converted shelf and a source fingerprint together. The fingerprint prevents a subsequent legacy restart from overwriting edits made after rollback. If the primary is damaged, a last-good recovery is explicitly reported; if both copies are unreadable, the service stays disabled and the error is recorded instead of silently replacing data.

The bridge is present only in the emergency legacy artifact. It is not included in the minimal APK and does not add startup work or settings to the new product. Source patch, APK, checksums and the rehearsal report are retained privately with the implementation evidence.

## Historical code8 → code9 rehearsal

On a disposable Android API26 emulator, a same-signature in-place update from minimal code8 to recovery code9 succeeded. The test used 18 synthetic entries, removed an original entry and added a synthetic WebAPK before rollback. The complete edited shelf survived exactly. Both minimal copies and the original legacy backup remained intact. A further legacy edit survived another restart without being overwritten. The rollback build passed 70 JVM tests, including three new conversion regressions.

This emulator rehearsal does **not** validate a physical OnePlus rollback. The installed certificate was separately checked for the code7->8->10 physical updates, but before any future update or recovery capture a fresh phone configuration and installed APK, compare signing certificates and verify the resulting shelf. Never run synthetic fixture setup against a physical phone. No private configuration, package inventory or signing key belongs in this public repository.
