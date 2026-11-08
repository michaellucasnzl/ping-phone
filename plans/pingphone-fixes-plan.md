# Plan: PingPhone alarm fixes, hardening, and dependency modernization

**Summary:** Modernize the Gradle/Android toolchain first, then deliver the alarm audio and full-screen dismiss behaviour as one atomic change, and finish with permission, receiver, settings, and UI hardening. The alarm channel will use a migrated channel ID because Android persists channel sound/vibration settings across app updates; merely changing the existing channel declaration would not reliably fix upgrades. CI verification is limited to `assembleDebug`, `lint`, JVM `test`, and diff inspection, with device-only alarm behaviour explicitly deferred to manual testing.

---

## Phase 1: Modernize the build toolchain and dependency catalog

- **Objective:** Establish the requested AGP 9, Kotlin, Gradle, SDK, and library baseline before changing application behaviour so all subsequent code is compiled against the final APIs.
- **Files to create or modify:**
  - `gradle/libs.versions.toml` — **modified**
    - Use the following versions:

      | Dependency/tool | Version |
      |---|---:|
      | AGP | `9.5.0` |
      | Kotlin / Compose compiler plugin | `2.4.21` |
      | Compose BOM | `2026.09.00` |
      | core-ktx | `1.19.1` (newest stable) |
      | datastore-preferences | `1.3.0` (newest stable) |
      | lifecycle-runtime-ktx / lifecycle-viewmodel-compose | `2.11.0` |
      | activity-compose | `1.13.0` |
      | Material Components | `1.14.0` |
      | JUnit 4 | `4.13.2` (already current) |
      | androidx.test.ext:junit | `1.3.0` |
      | Espresso Core | `3.7.0` |

    - Remove the unused `kotlin-android` plugin alias; retain `kotlin-compose`.
  - `app/build.gradle.kts` — **modified**
    - Remove `alias(libs.plugins.kotlin.android)` while retaining the Android application and Kotlin Compose plugins.
    - Replace `kotlinOptions` with AGP 9 built-in Kotlin's `kotlin.compilerOptions` DSL and `JvmTarget.JVM_17`.
    - Set Java source/target compatibility to 17; leave the Gradle daemon/JDK toolchain at 21.
    - Set `compileSdk = 37`, because Compose 1.12.x in BOM `2026.09.00` requires API 37 compilation.
    - Set `targetSdk = 36`, retain `minSdk = 26`, and set `versionCode = 2`, `versionName = "1.1"`.
  - `build.gradle.kts` — **modified**
    - Remove the root `kotlin.android ... apply false` declaration; keep the Compose plugin declaration.
  - `gradle/wrapper/gradle-wrapper.properties` — **modified**
    - Upgrade the distribution to Gradle `9.6.0`, the minimum/default version for AGP 9.5.0.
  - `gradle/wrapper/gradle-wrapper.jar` — **modified by regenerating the wrapper**
    - Regenerate with the Gradle wrapper task rather than hand-editing the binary.
  - `gradlew`, `gradlew.bat` — **modified only if wrapper regeneration changes them**
  - `settings.gradle.kts` — **modified**
    - Upgrade `org.gradle.toolchains.foojay-resolver-convention` to `1.0.0` if present; earlier releases are not the Gradle 9-compatible line.
- **Tests to write:**
  - No application tests in this phase. The existing JUnit 4 dependency already supplies JVM test infrastructure, so there is no missing-test-framework blocker.
  - Treat dependency resolution, Kotlin/Java compilation, manifest merging, resource linking, and lint configuration as this phase's build checks.
- **Acceptance criteria:**
  - `cd /workspace/ping-phone && ./gradlew assembleDebug` succeeds with the upgraded wrapper and produces the debug APK.
  - `./gradlew lint` completes without lint errors.
  - Diff inspection confirms there is no `kotlin-android`/`kotlin.android` application or `kotlinOptions` block, Java/Kotlin targets are 17, the daemon toolchain remains 21, and app version metadata is `2` / `1.1`.
  - The catalog contains exactly the confirmed dependency pins, with no dynamic versions or newly introduced runtime libraries.
  - `compileSdk` is 37 rather than 36, and `targetSdk` remains 36.
- **Rollback notes:**
  - Self-contained as a toolchain commit on top of the current app. Revert the catalog, build scripts, settings plugin, and generated wrapper files together; do not partially revert AGP without also reverting Gradle and built-in-Kotlin DSL changes.

---

## Phase 2: Make alarm playback single-source and deliver the lock-safe full-screen dismiss flow

- **Objective:** Fix the duplicate alarm sound and replace notification-dependent dismissal with an immediate clock-style activity while keeping the keyguard locked.
- **Files to create or modify:**
  - `app/src/main/java/com/michaellucas/pingphone/PingPhoneApplication.kt` — **modified**
    - Define the alarm channel with `IMPORTANCE_HIGH`, `setSound(null, null)`, and `enableVibration(false)`; remove alarm-ringtone/`AudioAttributes` construction from channel setup.
    - Migrate existing installs to a new versioned alarm-channel ID and delete only the legacy `alarm_channel`. Reusing the old ID is insufficient because Android retains its persisted sound and vibration configuration.
    - Keep `monitoring_channel` unchanged.
    - Load monitoring/alarm channel names and descriptions from string resources.
  - `app/src/main/java/com/michaellucas/pingphone/service/AlarmService.kt` — **modified**
    - Mark the alarm notification silent with `NotificationCompat.Builder.setSilent(true)` as defence in depth; retain its alarm category, maximum priority, ongoing state, and full-screen intent.
    - After foreground startup, make a best-effort explicit launch of `AlarmDismissActivity` using `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`. A launch failure must not prevent foreground playback, and the full-screen notification remains the locked-device/system fallback.
    - Replace synchronous `prepare()` with `prepareAsync()`, starting playback only from `setOnPreparedListener`.
    - Guard prepared callbacks against stop/preparation races: if the alarm was stopped or the callback belongs to a stale player, release that player instead of starting audio.
    - Install `setOnErrorListener`; release the failed player and route the failure through the same cleanup path without allowing an uncaught asynchronous error.
    - Route coroutine startup failures, missing usable ringtone URIs, and player errors through cleanup that resets `isAlarming`, releases player/wake lock, removes foreground state, stops the service, and restores saved audio/DND state exactly once. Do not treat normal coroutine cancellation as a new failure.
    - Make `ACTION_STOP` idempotent: when no alarm is active, only remove foreground state and stop the service; do not touch `MediaPlayer`, ringer mode, interruption filter, or alarm stream volume.
    - Ensure stopping while asynchronous preparation is in flight cannot subsequently restart playback.
  - `app/src/main/java/com/michaellucas/pingphone/ui/AlarmDismissActivity.kt` — **modified**
    - Retain show-when-locked and turn-screen-on behaviour, with the API 26 window-flag fallback.
    - Do **not** call `KeyguardManager.requestDismissKeyguard`, add `FLAG_DISMISS_KEYGUARD`, or otherwise unlock the device.
    - Route both the single Dismiss button and system back/gesture navigation through one function that sends `AlarmService.ACTION_STOP` and finishes the activity. Use `OnBackPressedCallback` rather than maintaining two independent back handlers.
    - Configure edge-to-edge/system-bar appearance without the deprecated global `window.statusBarColor` assignment; the visible status-bar area and initial window must be the alarm red with appropriately contrasting icons.
    - Add lightweight `@Preview` composables for `AlarmDismissScreen`; previews must remain side-effect-free and must not start services.
  - `app/src/main/AndroidManifest.xml` — **modified**
    - Declare `android.permission.USE_FULL_SCREEN_INTENT`.
    - Add `android:excludeFromRecents="true"` to `AlarmDismissActivity` and apply its alarm-specific theme.
    - Keep the activity non-exported and retain `showWhenLocked`, `turnScreenOn`, and `singleInstance`.
  - `app/src/main/res/values/themes.xml` — **modified**
    - Add an alarm-dismiss theme with a red initial window background and red/system-bar-compatible styling to prevent a white frame before Compose renders.
  - `app/src/main/res/values/colors.xml` — **modified**
    - Add one shared alarm-red color resource used by the window theme and Compose/system-bar treatment rather than duplicating color literals.
  - `app/src/main/java/com/michaellucas/pingphone/ui/theme/Theme.kt` — **modified**
    - Remove the deprecated `window.statusBarColor` write and obsolete imports/side effects. Rely on activity-level edge-to-edge/system-bar configuration so the general theme cannot overwrite the dismiss activity's red status area.
  - `app/src/main/res/values/strings.xml` — **modified**
    - Add monitoring/alarm channel names and descriptions.
- **Tests to write:**
  - Add no device-dependent service tests and no new Robolectric dependency. `MediaPlayer`, notification-channel migration, full-screen intents, background activity starts, and keyguard behaviour cannot be established reliably by the Verifier's JVM-only commands.
  - The added `@Preview` functions provide compile-time UI coverage, not behavioural tests.
- **Acceptance criteria:**
  - `./gradlew assembleDebug` and `./gradlew lint` succeed.
  - Diff inspection confirms there is no synchronous `MediaPlayer.prepare()` and that playback starts only after successful asynchronous preparation.
  - Every preparation/startup error path reaches active-alarm cleanup and resets `isAlarming`; cancellation cannot resurrect playback.
  - A stale `ACTION_STOP` cannot restore a default saved volume of zero or otherwise alter audio/DND state.
  - The new channel is soundless and non-vibrating, the builder is silent, and upgrade migration does not continue posting to the old audible channel ID.
  - `AlarmDismissActivity` has exactly one visible Dismiss action, and both that action and back dispatch stop/finish through the same path.
  - The manifest declares full-screen intent access and excludes the dismiss activity from recents.
  - No keyguard-dismiss API or flag appears anywhere in the diff.
  - The initial dismiss window and status-bar treatment are red, and `Theme.kt` no longer writes the deprecated status-bar color property.
- **Rollback notes:**
  - Code changes are self-contained on top of Phase 1 and can be reverted together. Reverting cannot restore notification-channel preferences deleted on devices that already ran the migration; those devices may need a reinstall or app-data reset to reproduce the original channel state.

---

## Phase 3: Harden permissions, receivers, settings, and UI state

- **Objective:** Complete the privacy/reliability changes and consolidate trigger matching/validation behind a JVM-testable pure component without changing the DataStore schema.
- **Files to create or modify:**
  - `app/src/main/AndroidManifest.xml` — **modified**
    - Remove `READ_SMS`.
    - Add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
    - Set `android:allowBackup="false"`.
    - Remove `LOCKED_BOOT_COMPLETED` from `BootReceiver`; retain `BOOT_COMPLETED`.
  - `app/src/main/java/com/michaellucas/pingphone/viewmodel/SettingsViewModel.kt` — **modified**
    - Keep SMS state based solely on `RECEIVE_SMS`; there is currently no separate `READ_SMS` check to preserve.
    - Add `batteryOptimization` and `fullScreenIntent` to `PermissionState`.
    - Refresh battery state with `PowerManager.isIgnoringBatteryOptimizations(packageName)`.
    - On Android 14/API 34+, refresh full-screen state with `NotificationManager.canUseFullScreenIntent()`; treat it as granted on older versions.
    - Treat battery state as satisfied below Android 12/API 31, where the new onboarding requirement does not apply.
    - Include both new values in `allGranted` only when applicable so the onboarding cannot be bypassed while required setup is incomplete.
  - `app/src/main/java/com/michaellucas/pingphone/ui/screen/PermissionOnboardingScreen.kt` — **modified**
    - Request only `RECEIVE_SMS` in the SMS runtime-permission launcher.
    - Add battery optimization as onboarding step 4 on Android 12+. Its explicit user action launches `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with the app's `package:` URI; do not request it automatically at startup.
    - On Android 14+, reuse the existing `PermissionStep` pattern for full-screen intent access rather than creating a separate one-off navigation component. Its action deep-links to `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` with the app package URI.
    - Continue refreshing permission state after runtime-permission results and through `MainActivity.onResume()` after returning from system settings.
  - `app/src/main/java/com/michaellucas/pingphone/receiver/BootReceiver.kt` — **modified**
    - Handle only `Intent.ACTION_BOOT_COMPLETED`; remove the impossible direct-boot branch because the app is not direct-boot aware.
  - `app/src/main/java/com/michaellucas/pingphone/data/TriggerMatcher.kt` — **new**
    - Add a pure Kotlin object with no Android or coroutine dependencies.
    - Centralize the existing escaped, word-boundary matching behaviour and blank/overlong validation, including the single 50-character limit.
    - Preserve literal handling of regex metacharacters and prevent matching a numeric trigger inside a longer alphanumeric token.
  - `app/src/main/java/com/michaellucas/pingphone/receiver/SmsReceiver.kt` — **modified**
    - Delegate matching to `TriggerMatcher` without changing multipart-body concatenation.
    - Use a named ad-hoc IO scope tied to the `goAsync()` operation.
    - Catch asynchronous storage/matching/service-start exceptions, including foreground-service start rejection, without logging SMS contents.
    - In `finally`, always call `PendingResult.finish()` and cancel the ad-hoc scope; never leave the broadcast pending or allow a failed service start to escape the coroutine.
  - `app/src/main/java/com/michaellucas/pingphone/data/SettingsRepository.kt` — **modified**
    - Reuse `TriggerMatcher` validation as the persistence-layer defence against blank and overlong values.
    - Preserve all existing preference keys and defaults; no migration is needed.
  - `app/src/main/java/com/michaellucas/pingphone/ui/screen/MainScreen.kt` — **modified**
    - Show an inline error and disable/hide Save for blank or overlong input instead of allowing a silent repository rejection. Keep valid-save behaviour unchanged.
    - Give the volume slider local UI state: update immediately in `onValueChange`, persist only in `onValueChangeFinished`, and synchronize local state when the stored value changes externally.
    - Cache the ringtone display title with `remember(alarmSoundUri)` so recomposition does not repeatedly construct a `Ringtone` and query its title.
  - `app/src/main/res/values/strings.xml` — **modified**
    - Add battery/full-screen onboarding copy and blank/overlong trigger-code validation messages.
  - `app/src/test/java/com/michaellucas/pingphone/data/TriggerMatcherTest.kt` — **new**
    - Add JVM-only JUnit 4 tests.
- **Tests to write:**
  - Verify a standalone default `777` trigger matches ordinary message text and punctuation-delimited text.
  - Verify `1777`, `7779`, and other longer-token occurrences do not match.
  - Verify blank/whitespace trigger codes never match and blank SMS bodies do not produce false positives.
  - Verify regex metacharacters in a valid custom code are interpreted literally rather than as regex syntax.
  - Verify validation accepts a nonblank 50-character value and rejects blank, whitespace-only, and 51-character values.
  - Do not add instrumentation tests: the Verifier will not run `connectedAndroidTest`, and no device/emulator is available.
- **Acceptance criteria:**
  - `./gradlew assembleDebug`, `./gradlew lint`, and `./gradlew test` all succeed; the new matcher tests are discovered and executed by the ordinary JVM `test` task.
  - `READ_SMS` and `LOCKED_BOOT_COMPLETED` are absent from source/manifest configuration, while `RECEIVE_SMS` remains in both declaration and runtime request paths.
  - The manifest contains battery-exemption and full-screen permissions and disables backup.
  - Onboarding step 4 uses the package-specific battery-exemption intent, and Android 14+ full-screen denial has a package-specific settings deep link.
  - Permission state refreshes from `PowerManager`/`NotificationManager`, and unsupported API levels do not block `allGranted`.
  - The receiver uses structured `try`/`catch`/`finally`, finishes its pending result on every asynchronous exit, cancels its scope, and contains no inline trigger regex.
  - Trigger validation is visible in the UI and remains enforced by the repository.
  - Slider persistence occurs only on gesture completion, and ringtone title lookup is keyed by `alarmSoundUri`.
  - Existing DataStore keys/defaults and trigger matching semantics are unchanged except for sharing their implementation.
- **Rollback notes:**
  - Self-contained on top of Phase 2. Reverting removes the new pure matcher/tests and restores local receiver/UI logic; no preference or database migration is required because stored settings and keys are unchanged.

---

## Verification limits and required manual device testing

The Verifier will run **only**:

- `cd /workspace/ping-phone && ./gradlew assembleDebug`
- `./gradlew lint`
- `./gradlew test` after Phase 3 adds JVM tests

No device or emulator is available in CI. Therefore, passing these commands does **not** verify full-screen-intent auto-launch, direct background activity launch, lock-screen overlay, keyguard retention, actual alarm audibility, notification-channel migration, MediaPlayer races, or foreground-service-from-background delivery. Those behaviours require manual testing on physical devices before release, at minimum covering Android 12, Android 13 notification permission, and Android 14+ full-screen-intent permission, plus a current target-SDK device.

The manual checklist must cover:

1. Fresh install and v1→v2 upgrade: one alarm sound only, with no channel-generated sound/vibration layered over `MediaPlayer`.
2. Trigger while unlocked, app-backgrounded, locked with screen on, and locked with screen off: the dismiss activity appears promptly where the OS permits, and the full-screen notification remains a fallback.
3. Dismiss and system back: playback stops, the activity finishes, the device remains locked, and prior alarm volume/ringer/DND states are restored.
4. Repeated trigger SMS messages, immediate stop during preparation, invalid/unavailable ringtone URI, and stale stop delivery: no overlapping playback, stuck foreground notification, wake lock, or zeroed alarm volume.
5. Android 14+ full-screen permission denied/re-granted through onboarding and battery exemption granted/revoked through onboarding.
6. SMS-triggered foreground service after the app has been backgrounded or swiped away, and monitoring behaviour after normal reboot. OEM background-activity and battery policies must be included in this testing because direct `startActivity` is best-effort rather than a universal guarantee.

## Open Questions

- `[TECHNICAL]` **AGP/Kotlin support boundary:** AGP 9.5.0 requires Gradle 9.6.0, and Kotlin 2.4.21 supports Gradle through 9.7.0, but Kotlin's published fully supported AGP range currently ends at AGP 9.3.1. The requested AGP 9.5.0/Kotlin 2.4.21 combination is therefore outside Kotlin's fully tested AGP matrix even though AGP's built-in Kotlin and D8 support the Kotlin 2.4 line. Confirm that the requested pins take precedence and that clean CI resolution is sufficient; otherwise select a mutually supported AGP/Kotlin pair before committing Phase 1.
- `[TECHNICAL]` **Exact pins versus newest stable:** Official release listings now show core-ktx `1.19.1` and DataStore `1.3.0`, while the confirmed requirements explicitly pin `1.19.0` and `1.2.1`. This plan assumes the explicit confirmed pins win. Confirm whether "all libraries latest stable" is intended to override those two pins.
- `[TECHNICAL]` **Compile/target SDK distinction:** Compose BOM `2026.09.00`/Compose 1.12 requires `compileSdk = 37`, so the requested 36-only compilation setting is not viable. This plan uses `compileSdk = 37` and `targetSdk = 36`; confirm that opting into target-SDK 37 behaviour changes is not also required.
- `[TECHNICAL]` **CI SDK availability:** The build environment must provide Android SDK platform 37 and the build tools required by AGP 9.5. Confirm this provisioning if the first build reports a missing platform; no source-phase change can substitute for the required SDK.

## Risks

- **High likelihood / High impact — Existing installs retain the audible channel.** Recreating a channel with the same ID does not reliably apply new sound/vibration settings.
  - **Mitigation:** Use a versioned channel ID, delete only the legacy alarm channel, and manually test an actual v1→v2 upgrade.
- **Medium likelihood / High impact — AGP 9.5 and Kotlin 2.4.21 expose an untested plugin interaction.**
  - **Mitigation:** Keep the build migration isolated in Phase 1, verify the published matrix before committing, and require all three Gradle gates after tests are added.
- **Medium likelihood / High impact — Android blocks or alters direct background activity launch or full-screen display.**
  - **Mitigation:** Call `startForeground` first, retain the high-priority alarm full-screen notification as fallback, handle Android 14+ access explicitly, avoid keyguard dismissal, and test across Android/OEM versions. Do not claim CI proves immediate display.
- **Medium likelihood / High impact — Asynchronous player preparation races with dismissal or failure cleanup.**
  - **Mitigation:** Guard callbacks by active-alarm/player identity, release stale players, centralize idempotent cleanup, and manually test stop-during-prepare and repeated triggers.
- **Medium likelihood / Medium impact — `setSilent(true)` suppresses heads-up peeking on some Android versions.**
  - **Mitigation:** Treat this as intentional silence defence rather than relying on heads-up audio; the explicit activity launch and full-screen intent provide the alarm UI. Validate visual behaviour on target devices.
- **Medium likelihood / High impact — Battery exemption/full-screen requests encounter Play policy or OEM settings differences.**
  - **Mitigation:** Launch settings only from explicit user actions, explain why each exemption is needed, preserve graceful setup denial, and review policy eligibility before distribution.
- **Medium likelihood / Medium impact — Compile SDK 37 or new stable libraries expose CI/source incompatibilities.**
  - **Mitigation:** Resolve them in Phase 1 rather than mixing toolchain failures into alarm behaviour changes; keep application source changes forced by the migration minimal and visible in review.
- **Low likelihood / Medium impact — Trigger extraction accidentally changes matching semantics.**
  - **Mitigation:** Reuse the escaped boundary expression and add focused tests for partial tokens, metacharacters, blanks, and validation length boundaries.

## Out of scope

- Sender-number whitelisting; design it as a separate follow-up.
- Release-build minification, R8/ProGuard policy, and release signing configuration.
- Crash reporting or introduction of a logging framework; any necessary receiver diagnostics should use minimal platform logging without SMS contents.
- Density-specific/new launcher icon production.
