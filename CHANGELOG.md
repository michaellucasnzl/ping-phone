# Changelog

## [Unreleased] — 2026-11-09

### Added

- 5 launcher icon candidates (alarm-clock+phone, ringing phone+waves, bell+phone, phone+alert badge, megaphone) as browser-previewable SVGs + Android vector drawables. Variant A (alarm-clock + phone handset) installed as the default.
- Android 13+ monochrome themed-icon layer (`ic_launcher_monochrome.xml`) — the system can now tint the launcher icon to match the user's wallpaper/theme.
- "Save & Close" button on the settings screen — full-width, at the bottom of the settings column. Disabled when the trigger code is invalid; saves and exits when valid.

### Changed

- App colour theme: teal → autumn (burnt-orange/amber Material 3 scheme). Applied throughout: settings, onboarding, status bar, surfaces. Dynamic color remains OFF for consistent branding across all devices.
- Alarm-dismiss screen colour: bright Material red (#B71C1C) → deep autumn rust red (#9A2C16). Still reads as urgent; harmonises with the autumn brand.
- Adaptive icon background: deep teal (#004D40) → deep autumn brown-orange (#7A3E1A).

## [1.1] — 2026-11-08

### Fixed

- Duplicate/overlapping alarm audio. The alarm notification channel had a sound and vibration enabled, layering a second audio source on top of the looping `MediaPlayer`. The channel is now soundless and vibration-free, with a versioned `alarm_channel_v2` ID and a create-then-delete migration of the legacy `alarm_channel` (Android persists channel settings across app updates).
- Dismiss screen was hard to reach when the device was unlocked — the full-screen intent only auto-launches when locked or the screen is off, so users had to pull down the notification shade. The dismiss activity now launches immediately after `startForeground`.
- A stale `ACTION_STOP` arriving at a fresh service instance could set the alarm volume to 0 (default `savedAlarmVolume = 0`). Stop is now idempotent.
- System Back / gesture navigation finished the dismiss activity without stopping the alarm. Both now route through `dismissAlarm()`.
- White flash before Compose rendered on the dismiss screen. New `Theme.PingPhone.AlarmDismiss` sets a red window background and matching status/navigation bars.
- `MediaPlayer.prepare()` blocked the main thread. Replaced with `prepareAsync()` plus prepared and error listeners.
- `isAlarming` was not reset on the coroutine failure path; `CancellationException` is now rethrown rather than swallowed as a failure.

### Added

- Full-screen dismiss activity that appears over the lock screen. The keyguard stays locked after Dismiss, matching the AOSP clock-alarm behaviour.
- `USE_FULL_SCREEN_INTENT` permission and an Android 14+ onboarding step that deep-links to the system full-screen-intent settings when access is not granted.
- Battery-optimization exemption permission and a 4th onboarding step (Android 12+) so the OS does not kill the app and miss the trigger SMS.
- `TriggerMatcher` — a pure-Kotlin object shared by `SettingsRepository` and `SmsReceiver` — with 13 JVM JUnit 4 tests covering word boundaries, partial-token non-matches, blank input, literal regex metacharacters, and the 50/51-character limit.
- Inline validation on the trigger-code field; Save is disabled for blank or overlong codes.
- `@Preview` composable for `AlarmDismissScreen`.
- Structured error handling in `SmsReceiver` that logs only the exception class and message, never SMS contents.

### Changed

- Toolchain: AGP 9.5.0, Kotlin 2.4.21, Compose BOM 2026.09.00, Gradle 9.6.0, foojay-resolver-convention 1.0.0, plus library bumps (core-ktx 1.19.1, datastore 1.3.0, lifecycle 2.11.0, activity-compose 1.13.0, material 1.14.0, androidx.test.ext:junit 1.3.0, espresso 3.7.0).
- compileSdk 37, targetSdk 36, minSdk 26; Java/Kotlin target 17; JDK toolchain remains 21.
- versionCode 2, versionName 1.1.
- `kotlinOptions` replaced by the AGP 9 built-in Kotlin DSL (`kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }`); the `kotlin-android` plugin was removed.
- Volume slider persists on `onValueChangeFinished` instead of writing to DataStore on every drag frame.
- `android:allowBackup` set to `false`.
- Notification channel names and descriptions moved from hardcoded strings into `strings.xml`.

### Removed

- `READ_SMS` permission — `RECEIVE_SMS` provides both the message body and sender number.
- `VIBRATE` permission — unused after the soundless channel migration.
- `LOCKED_BOOT_COMPLETED` from `BootReceiver` — the app is not `directBootAware`, so that branch could never run.
- Deprecated `window.statusBarColor` write from `Theme.kt`.
- Synchronous `MediaPlayer.prepare()`.
