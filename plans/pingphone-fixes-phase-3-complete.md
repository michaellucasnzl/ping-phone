# Phase 3 Complete — Privacy/Reliability Hardening + TriggerMatcher Extraction

Status: implemented, not yet verified (Gradle build/lint/test deferred to the end-of-task verification gate).

## Files changed

### 1. `app/src/main/AndroidManifest.xml`
- Removed `READ_SMS`; kept only `RECEIVE_SMS` (with explanatory comment).
- Added `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Android 12+ so the OS doesn't kill the app and miss the trigger SMS).
- `android:allowBackup` changed `true` → `false` (alarm-app config should not be cloud-backed up).
- Removed `LOCKED_BOOT_COMPLETED` action from the `BootReceiver` intent-filter (`BOOT_COMPLETED` retained). The app is not `directBootAware`, so that branch could never work.
- `AlarmDismissActivity` entry and other Phase 2 changes left untouched.

### 2. `app/src/main/java/com/michaellucas/pingphone/data/TriggerMatcher.kt` (NEW)
- Pure Kotlin `object` with no Android/coroutine deps.
- `MAX_CODE_LENGTH = 50`, `isValidCode()` (non-blank && length <= 50), `matches()` (word-bounded `\b` regex with `Regex.escape`, invalid code never matches).

### 3. `app/src/main/java/com/michaellucas/pingphone/data/SettingsRepository.kt`
- `saveTriggerCode` now uses `if (!TriggerMatcher.isValidCode(code)) return`. No other changes.

### 4. `app/src/main/java/com/michaellucas/pingphone/receiver/SmsReceiver.kt`
- Matching now `TriggerMatcher.matches(fullBody, triggerCode)`.
- Named cancellable `CoroutineScope(Dispatchers.IO)`; `try/catch(CancellationException){throw}/catch(Exception){...}/finally{ pendingResult.finish(); scope.cancel() }`.
- `startForegroundService` wrapped in its own try/catch; logs only exception class/message (never SMS contents).
- Added `companion object { private const val TAG = "SmsReceiver" }`.
- Multipart-body concatenation unchanged.

### 5. `app/src/main/java/com/michaellucas/pingphone/receiver/BootReceiver.kt`
- Handles only `Intent.ACTION_BOOT_COMPLETED`; comment explains why `LOCKED_BOOT_COMPLETED` is dropped. Still calls `MonitoringNotificationHelper.showMonitoringNotification`.

### 6. `app/src/main/java/com/michaellucas/pingphone/viewmodel/SettingsViewModel.kt`
- `PermissionState` gained `batteryOptimization` (default true) and `fullScreenIntent` (default true); `allGranted` includes both.
- `refreshPermissions()` computes:
  - battery exemption via `PowerManager.isIgnoringBatteryOptimizations()` on API 31+, else true.
  - full-screen access via `NotificationManager.canUseFullScreenIntent()` on API 34+, else true.
- SMS check remains `RECEIVE_SMS`.

### 7. `app/src/main/java/com/michaellucas/pingphone/ui/screen/PermissionOnboardingScreen.kt`
- SMS request reduced to `arrayOf(Manifest.permission.RECEIVE_SMS)` (launcher type unchanged: `RequestMultiplePermissions`).
- Added Step 4 battery-optimization step on API 31+ (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `package:` URI, `FLAG_ACTIVITY_NEW_TASK`, try/catch `ActivityNotFoundException`).
- Added Step 5 full-screen-intent step on API 34+ only when `!permissions.fullScreenIntent` (`ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`, same pattern).
- Dynamic, gap-free step numbering: `batteryStep = if (showNotifications) 4 else 3`, `fullScreenStep = batteryStep + 1`.
- `all_set` button still gated on `permissions.allGranted`.

### 8. `app/src/main/java/com/michaellucas/pingphone/ui/screen/MainScreen.kt`
- Trigger field: `isError`/error supporting text shown only when input non-empty and invalid; Save button `enabled = isCodeValid`.
- Volume slider uses local `sliderValue` keyed on `volumePercent`, persists on `onValueChangeFinished`; label uses live value.
- Ringtone title cached in `remember(alarmSoundUri)`; ListItem uses `alarmSoundLabel`.

### 9. `app/src/main/res/values/strings.xml`
- Added `permission_battery_title`, `permission_battery_desc`, `permission_fullscreen_title`, `permission_fullscreen_desc`, `trigger_code_invalid`.
- Verified Phase 2 strings (`monitoring_channel_*`, `alarm_channel_*`, `alarm_legacy_channel_name`) are present.

### 10. `app/src/test/java/com/michaellucas/pingphone/data/TriggerMatcherTest.kt` (NEW)
- 13 JVM JUnit 4 tests covering boundary matching, `1777`/`7779` non-matches, blank body/code, literal regex metacharacters, and the 50/51-char validation boundary.

## Deviations from plan
- `supportingText` shows the error only when the input is non-empty and invalid (otherwise the existing hint), matching the plan's stated intent that empty input is not itself an error state. The plan's literal snippet would have shown the error for empty input.
- Tests import only `assertFalse`/`assertTrue` (the plan's `assertEquals` import was unused and removed).

## Not done in this phase (per instructions)
- No Gradle build, lint, or test was run.
- No commit made.
