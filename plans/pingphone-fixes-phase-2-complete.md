# Phase 2 Complete — Single-source alarm audio & lock-safe full-screen dismiss

Implements Phase 2 of `plans/pingphone-fixes-plan.md`: the duplicate-alarm-sound fix and the
clock-alarm-style immediate dismiss flow that keeps the device locked.

## Files changed

### `app/src/main/res/values/strings.xml`
- Added notification-channel name/description strings:
  `monitoring_channel_name`, `monitoring_channel_description`, `alarm_channel_name`,
  `alarm_channel_description`, `alarm_legacy_channel_name`.

### `app/src/main/res/values/colors.xml`
- Added `alarm_red` (`#FFB71C1C`), shared by the dismiss window theme and Compose surface.
- Existing `ic_launcher_background` retained.

### `app/src/main/res/values/themes.xml`
- Added `Theme.PingPhone.AlarmDismiss` (parent `Theme.PingPhone`) with red
  `windowBackground`, `statusBarColor`, `navigationBarColor`, and `windowLightStatusBar=false`.
- `Theme.PingPhone` unchanged.

### `app/src/main/AndroidManifest.xml`
- Declared `android.permission.USE_FULL_SCREEN_INTENT`.
- `AlarmDismissActivity`: added `android:theme="@style/Theme.PingPhone.AlarmDismiss"` and
  `android:excludeFromRecents="true"`; kept `exported=false`, `showWhenLocked`,
  `turnScreenOn`, `singleInstance`.
- No other manifest changes.

### `app/src/main/java/com/michaellucas/pingphone/PingPhoneApplication.kt`
- Monitoring channel name/description now loaded from string resources.
- New soundless/non-vibrating `CHANNEL_ALARM_V2` (`alarm_channel_v2`) at `IMPORTANCE_HIGH`.
- Legacy `CHANNEL_ALARM` (`alarm_channel`) is created then immediately deleted inside a
  `try/catch` to migrate existing installs away from its persisted audible configuration.
- Removed `RingtoneManager`/`AudioAttributes` channel-sound code and their imports.
- Companion now exposes `CHANNEL_MONITORING`, `CHANNEL_ALARM`, `CHANNEL_ALARM_V2`.

### `app/src/main/java/com/michaellucas/pingphone/service/AlarmService.kt`
- Notification builder now uses `CHANNEL_ALARM_V2` and `.setSilent(true)` (defence in depth),
  retaining `PRIORITY_MAX`, `CATEGORY_ALARM`, full-screen intent, Dismiss action, ongoing,
  non-auto-cancel.
- After `startForeground(...)`, best-effort `startActivity(AlarmDismissActivity)` with
  `NEW_TASK | CLEAR_TOP` so the dialog pops immediately when unlocked too; wrapped in `try/catch`.
- Replaced synchronous `prepare()`/`start()` with `setOnPreparedListener` + `setOnErrorListener`
  + `prepareAsync()`. Prepared callback starts only when `isAlarming` and it is the current player,
  otherwise releases the stale player.
- Added `cleanupAndStopSelf()` as the shared full-cleanup path (sets `isAlarming=false`, cancels
  `startupJob`, releases player/wake lock, restores audio/DND, removes foreground, stops self).
- `ACTION_STOP` is now idempotent via `stopAlarm(idempotent = true)`: a stale stop on a fresh
  instance only removes foreground state and stops, never touching audio/DND (avoids zeroing the
  alarm stream from the default `savedAlarmVolume = 0`).
- Coroutine failure path calls `cleanupAndStopSelf()`; `CancellationException` is rethrown so
  normal cancellation is not treated as an error. Missing ringtone URIs route through the same
  cleanup.
- `onDestroy()` wake-lock release retained.

### `app/src/main/java/com/michaellucas/pingphone/ui/AlarmDismissActivity.kt`
- Back/gesture routed through `dismissAlarm()` via
  `onBackPressedDispatcher.addCallback(this) { dismissAlarm() }`.
- Compose surface/button colours now use `colorResource(R.color.alarm_red)` instead of a literal.
- Added side-effect-free `@Preview` for `AlarmDismissScreen`.
- No keyguard-dismiss API/flag added; the device stays locked.

### `app/src/main/java/com/michaellucas/pingphone/ui/theme/Theme.kt`
- Removed the deprecated `window.statusBarColor` `SideEffect` and its now-unused imports
  (`Activity`, `SideEffect`, `toArgb`, `LocalView`, `WindowCompat`).
- Colour schemes and `PingPhoneTheme` signature unchanged.

## Deviations from the plan

1. **Channel sound/vibration:** the plan text says to call `setSound(null, null)` and
   `enableVibration(false)` explicitly; per the phase instruction these calls are omitted because
   `NotificationChannel` defaults to no sound and no vibration. Behaviour is identical
   (soundless/non-vibrating), and the plan acceptance criterion is met.
2. **Monitoring channel:** the plan said "keep unchanged", but the phase instruction asked to load
   its name/description from string resources; the ID/importance are unchanged.
3. **Edge-to-edge in `AlarmDismissActivity`:** `enableEdgeToEdge()` was intentionally NOT called.
   The red status/navigation bars and white icons are provided by `Theme.PingPhone.AlarmDismiss`
   (`windowBackground`/`statusBarColor`/`navigationBarColor` + `windowLightStatusBar=false`), and
   the Compose `Surface` fills the window, so the status-bar area is red with contrasting icons.
4. **Error listener:** sets `mediaPlayer = null` before `mp.release()` so the shared cleanup does
   not call `isPlaying`/`release` a second time on an already-released player.
5. **Compose colour lookup:** used `colorResource(R.color.alarm_red)` rather than a
   `Color(R.color...)` literal, eliminating the duplicated hex.

## Verification status

Per instruction, no Gradle build, lint, or test command was run in this phase. All eight files
were read back and reviewed for valid imports, balanced braces, and syntactically correct
Kotlin/XML. Build, lint, and any JVM tests are deferred to the end-of-task verification gate.

## Out-of-scope work discovered

None.
