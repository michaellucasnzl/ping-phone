# Task Complete: PingPhone alarm fixes, hardening, and dependency modernization

**Started:** 2026-11-08
**Completed:** 2026-11-08
**Phases:** 3
**Files changed:** 21 unique files (Phase 1: 5; Phase 2: 8; Phase 3: 10 — `AndroidManifest.xml` and `strings.xml` were also changed in Phase 2)
**Review verdict:** APPROVED (1 revision round)
**Security:** 0 Critical / 0 High / 0 Medium / 1 Low — Low finding (dead `VIBRATE` permission) fixed before approval

## Phase Summary

1. **Build toolchain modernization** — AGP 8.13.2 → 9.5.0, Kotlin 2.0.21 → 2.4.21, Compose BOM 2024.12.01 → 2026.09.00, Gradle 8.13 → 9.6.0, foojay 0.10.0 → 1.0.0. Removed the `kotlin-android` plugin (AGP 9 has built-in Kotlin) and replaced `kotlinOptions` with `kotlin { compilerOptions { jvmTarget = JVM_17 } }`. compileSdk 35 → 37, targetSdk 35 → 36, minSdk 26 unchanged, Java 11 → 17, JDK toolchain stays 21. versionCode 2, versionName 1.1.
2. **Alarm audio fix + dismiss UX** — Notification channel is now soundless and vibration-free. Root cause of duplicate/overlapping alarms: the channel's `setSound(TYPE_ALARM)` and `enableVibration(true)` layered a second audio source over the looping `MediaPlayer`. New versioned channel `alarm_channel_v2` with create-then-delete migration of legacy `alarm_channel` (Android persists channel settings across upgrades). `setSilent(true)` added as defence in depth. `MediaPlayer.prepare()` → `prepareAsync()` with prepared/error listeners. `ACTION_STOP` made idempotent so a stale stop cannot zero the volume. `AlarmDismissActivity` now launches immediately after `startForeground`, so the dismiss screen appears without pulling down the notification shade. Back/gesture navigation routed through `dismissAlarm()`. New `Theme.PingPhone.AlarmDismiss` removes the pre-Compose white flash. Keyguard intentionally stays locked after dismiss (AOSP clock-alarm behaviour).
3. **Hardening + tests** — Removed `READ_SMS` (`RECEIVE_SMS` supplies body and sender). Added battery-optimization permission and a 4th onboarding step (Android 12+), plus a full-screen-intent step (Android 14+, only when not granted). Dropped `LOCKED_BOOT_COMPLETED` (app is not `directBootAware`). `allowBackup=false`. Removed dead `VIBRATE` permission. Extracted pure-Kotlin `TriggerMatcher` (word-boundary regex, `Regex.escape`, non-blank, 50-char limit) with 13 JVM JUnit 4 tests. `SmsReceiver` gained structured error handling that logs only exception class/message — never SMS contents. `SettingsViewModel.PermissionState` gained `batteryOptimization` and `fullScreenIntent`. `MainScreen` gained inline trigger-code validation and debounced volume persistence.

**Revision after review:** added the missing `import kotlinx.coroutines.cancel` in `SmsReceiver.kt`; removed the dead `VIBRATE` permission.

## Gate Results

| Gate | Result |
|---|---|
| Reviewer | APPROVED after revision delta |
| Security Auditor | 0 Critical / 0 High / 0 Medium / 1 Low (fixed) |
| Build (`assembleDebug`) | NOT RUN — no JDK or Android SDK in the working environment |
| Lint (`lint`) | NOT RUN — same reason |
| Tests (`test`) | NOT RUN — same reason; `TriggerMatcherTest` is written but unexecuted |

The build, lint, and test gates were skipped for environmental reasons only. They are not reported as passing. Nothing in this task was compiled or executed.

## Follow-ups

- **Run the gate locally.** On a machine with Android Studio: `./gradlew assembleDebug`, `./gradlew lint`, `./gradlew test`. Expect the first run to download Gradle 9.6.0 and the new dependency set.
- **Manual device testing is required.** Device-only behaviour cannot be covered by the deferred gate. Minimum matrix:
  - Android 12 — battery-optimization onboarding step appears and the exemption takes effect.
  - Android 13 — notification permission step; trigger SMS while backgrounded.
  - Android 14+ — full-screen-intent onboarding step and auto-launch of the dismiss screen when locked and when unlocked.
  - Current target-SDK (36) device — notification-channel migration from `alarm_channel` to `alarm_channel_v2`.
  - On every device: alarm audibility, no duplicate/overlapping audio, keyguard retention after dismiss, stale-notification stop, and FGS-from-background delivery.
- Out of scope, per the plan: sender-number whitelisting (would also only need `RECEIVE_SMS`), R8/minification and release signing, crash reporting, density-specific launcher icons.

## Changelog Entry

```
feat(alarm): fix duplicate alarm audio, add lock-screen dismiss flow, and modernize toolchain (v1.1)
```
