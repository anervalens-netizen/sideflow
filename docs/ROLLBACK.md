# In-place rollback, without losing the latest shortcuts

This is an emergency delivery path, not an additional feature in the minimal runtime.

The private recovery artifact is built from legacy `073ba4b` plus local recovery commit `1689dad`, with the same application ID and signing certificate, versionCode **9**, versionName `0.1.6-rollback`. The rehearsed minimal 0.2.0 used code **8**. **Current owner-installed minimal 0.2.1 uses code10: this existing recoverycode9 is not a compatible normal in-place rollback.** Before any recovery from code10, rebuild the recovery-only bridge with a code strictly greater than the version installed then, preserve the same signing certificate, and rerun the latest-edit preservation rehearsal. This follow-up is tracked in issue #10. A subsequent minimal update must exceed the newly used recovery version. Do not uninstall or clear app data, and do not use a new signing key.

The recovery-only startup bridge strictly decodes the current minimal v2 shelf and converts every section/item to legacy v1, preserving IDs, order, names, package targets and edits made after the upgrade. It keeps the minimal files untouched, backs up the original legacy preferences, and commits the converted shelf and a source fingerprint together. The fingerprint prevents a subsequent legacy restart from overwriting edits made after rollback. If the primary is damaged, a last-good recovery is explicitly reported; if both copies are unreadable, the service stays disabled and the error is recorded instead of silently replacing data.

The bridge is present only in the emergency legacy artifact. It is not included in the minimal APK and does not add startup work or settings to the new product. Source patch, APK, checksums and the rehearsal report are retained privately with the implementation evidence.

## Executed rehearsal

On a disposable Android API26 emulator, a same-signature in-place update from minimal code8 to recovery code9 succeeded. The test used 18 synthetic entries, removed an original entry and added a synthetic WebAPK before rollback. The complete edited shelf survived exactly. Both minimal copies and the original legacy backup remained intact. A further legacy edit survived another restart without being overwritten. The rollback build passed 70 JVM tests, including three new conversion regressions.

This emulator rehearsal does **not** validate a physical OnePlus rollback. The installed certificate was separately checked for the code7->8->10 physical updates, but before any future update or recovery capture a fresh phone configuration and installed APK, compare signing certificates and verify the resulting shelf. Never run synthetic fixture setup against a physical phone. No private configuration, package inventory or signing key belongs in this public repository.
