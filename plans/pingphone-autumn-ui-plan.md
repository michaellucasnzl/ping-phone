# Plan: PingPhone autumn UI, launcher icons, and Save & Close

**Summary:** Replace the teal theme with a fixed, role-complete autumn Material 3 color scheme while preserving the dismiss screen's separate urgent rust-red color. Add five browser-previewable launcher icon concepts and matching Android vectors, install variant A as the working default with Android 13+ monochrome support, and add a validated full-width "Save & Close" action to the settings column. Deliver everything as one self-contained commit with no new dependencies, persistence changes, or automated tests.

---

## Phase 1: Autumn theme, five icon candidates, and Save & Close

- **Objective:** Deliver the complete, tightly coupled UI refresh in one buildable commit, with variant A active immediately so the app remains installable before the user selects a final icon.

### Files to create or modify

**Theme**
- `app/src/main/java/com/michaellucas/pingphone/ui/theme/Color.kt` — **modified**: remove all six teal constants and define the complete autumn light/dark role set specified below.
- `app/src/main/java/com/michaellucas/pingphone/ui/theme/Theme.kt` — **modified**: explicitly populate every listed Material 3 role from the autumn constants; preserve the existing theme-selection logic and `dynamicColor = false` default.
- `app/src/main/res/values/colors.xml` — **modified**: set `ic_launcher_background` to `#7A3E1A` and `alarm_red` to `#9A2C16`; update their comments to describe the autumn colors.

**Browser icon previews**
- `design/icons/icon_a.svg` — **new**: alarm clock plus phone handset.
- `design/icons/icon_b.svg` — **new**: ringing phone with sound waves.
- `design/icons/icon_c.svg` — **new**: bell plus phone.
- `design/icons/icon_d.svg` — **new**: phone with alert/exclamation badge.
- `design/icons/icon_e.svg` — **new**: megaphone alarm.

**Android launcher assets**
- `app/src/main/res/drawable/ic_launcher_foreground_a.xml` through `_e.xml` — **new**: five variant vector foregrounds.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — **modified**: replace the current ringing-phone artwork with the same geometry as variant A; this remains the single active foreground referenced by the launcher icons.
- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — **new**: simplified, fill-only variant A silhouette for Android 13+ themed icons.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — **modified**: add the monochrome layer while retaining the existing background and active foreground references.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — **modified**: make the identical monochrome-layer addition.

**Save & Close**
- `app/src/main/java/com/michaellucas/pingphone/ui/screen/MainScreen.kt` — **modified**: add the bottom-of-column button and its save/finish behavior.
- `app/src/main/res/values/strings.xml` — **modified**: add `save_and_close` with the value `Save &amp; Close`.

### Exact autumn palette

Define opaque Kotlin colors using `0xFF` followed by the six hex digits below. Name each constant by prefixing `Autumn` and suffixing `Light` or `Dark` to the PascalCase role name; for example, the primary pair is `AutumnPrimaryLight` and `AutumnPrimaryDark`, and the on-primary-container pair is `AutumnOnPrimaryContainerLight` and `AutumnOnPrimaryContainerDark`.

| Material 3 role | Light value | Dark value |
|---|---:|---:|
| primary | `#8C4D14` | `#FFB870` |
| onPrimary | `#FFFFFF` | `#512A00` |
| primaryContainer | `#FFDBC2` | `#6F390B` |
| onPrimaryContainer | `#2F1500` | `#FFDBC2` |
| secondary | `#765847` | `#E6BDA9` |
| onSecondary | `#FFFFFF` | `#432B1E` |
| secondaryContainer | `#FFDBC9` | `#5C4132` |
| onSecondaryContainer | `#2C160C` | `#FFDBC9` |
| tertiary | `#7A5900` | `#EAC248` |
| onTertiary | `#FFFFFF` | `#403000` |
| tertiaryContainer | `#FFDEA1` | `#5C4300` |
| onTertiaryContainer | `#261A00` | `#FFDEA1` |
| background | `#FFF8F4` | `#19110C` |
| onBackground | `#211A15` | `#F0DFD6` |
| surface | `#FFF8F4` | `#19110C` |
| onSurface | `#211A15` | `#F0DFD6` |
| surfaceVariant | `#F4DDD2` | `#52443B` |
| onSurfaceVariant | `#52443B` | `#D8C2B7` |
| outline | `#85736A` | `#A08D82` |
| outlineVariant | `#D8C2B7` | `#52443B` |
| error | `#BA1A1A` | `#FFB4AB` |
| onError | `#FFFFFF` | `#690005` |
| errorContainer | `#FFDAD6` | `#93000A` |
| onErrorContainer | `#410002` | `#FFDAD6` |

Map every row directly into the corresponding `lightColorScheme` and `darkColorScheme` argument. The burnt-orange primary, warm-brown secondary, and amber tertiary provide the autumn branding; the standard error roles remain independent from the alarm-dismiss color. Remove obsolete hardcoded scheme colors and any imports that become unused.

Keep `PingPhoneTheme`'s existing `darkTheme` handling, dynamic-color branch, and `dynamicColor: Boolean = false` default unchanged. No caller should enable dynamic color. Do not modify `Type.kt`.

In `colors.xml`, use the following exact resource values:
- `ic_launcher_background`: `#7A3E1A`, a deep autumn brown-orange shared by the adaptive icon and SVG previews.
- `alarm_red`: `#9A2C16`, an opaque deep rust red reserved for alarm urgency. Do not substitute the brand primary or the Material error role.

Leave `themes.xml` structurally unchanged. Its existing `@color/alarm_red` references will propagate the new rust red to the dismiss window, status bar, and navigation bar. `AlarmDismissActivity.kt` already uses `colorResource(R.color.alarm_red)` for both its Surface and dismiss-button content color, so it requires no edit.

### Icon construction specification

All five concepts must be visually distinct, legible at launcher size, and built from simple filled geometry rather than text, fonts, raster images, gradients, shadows, or external assets.

| Variant | Required composition |
|---|---|
| A — default | An alarm clock with a circular face, two bells, clock hands, and small feet, paired with a clearly recognizable phone handset across its lower/right portion. Keep the handset distinguishable through negative space rather than extra colors. |
| B | A centered smartphone with three concentric ringing/sound-wave arcs on its right. The existing foreground geometry may be reused for this candidate. |
| C | A smartphone silhouette combined with a prominent notification bell in front; include the bell's clapper and avoid adding sound waves so it remains distinct from B. |
| D | A smartphone with a circular alert badge overlapping its upper-right corner; the badge contains a bold exclamation mark formed from vector paths. |
| E | A bold megaphone angled upward to the right with three short sound rays; use a handle and open horn mouth to make it readable without relying on fine detail. |

For every `design/icons/icon_*.svg`:
- Use a 192×192 display size with a `0 0 108 108` viewBox so the browser artwork can share coordinates with the Android vectors.
- Include a full-canvas background rectangle filled with `#7A3E1A`.
- Render the glyph in warm white `#FFF8F4`.
- Keep the SVG self-contained with inline attributes/styles and no external references.
- Use the same geometry as its corresponding Android foreground, apart from the preview glyph color, so browser selection accurately represents the installable candidate.

For every `ic_launcher_foreground_[a-e].xml`:
- Use 108dp width and height with a 108×108 viewport and a transparent background.
- Use opaque white `#FFFFFFFF` for all visible foreground geometry.
- Keep every part of the visible glyph, including sound rays and badges, inside the adaptive-icon safe bounds from coordinate 18 through 90 on both axes.
- Prefer filled paths over stroke-dependent artwork so rendering remains consistent across devices.
- Keep the five candidates as swap targets; only `ic_launcher_foreground.xml` is referenced by the mipmaps in this phase.

Copy variant A's geometry into the active `ic_launcher_foreground.xml`. Derive `ic_launcher_monochrome.xml` from that same variant, using only opaque white `#FFFFFFFF` filled shapes and transparent negative space. Remove internal detail that would disappear in a single-alpha-channel themed icon, but retain the combined alarm-clock/handset outer silhouette. Do not include a background rectangle in any foreground or monochrome drawable.

In both adaptive-icon XMLs, retain:
- Background: `@color/ic_launcher_background`
- Foreground: `@drawable/ic_launcher_foreground`

Add `<monochrome android:drawable="@drawable/ic_launcher_monochrome" />` alongside them. Keep both files in `mipmap-anydpi-v26`; adaptive icons require API 26+, and the project's minimum SDK is already 26. Do not follow any lint suggestion to merge them into an unqualified `mipmap-anydpi` directory.

Do not modify `app/src/main/res/drawable/ic_notification.xml`; it must remain a 24dp, single-color white notification small icon for system tinting.

### Save & Close specification

Add one Material 3 `Button` as the final child of the existing scrollable settings `Column`, after all conditional `WarningCard` entries:
- Use `Modifier.fillMaxWidth()` and the button's default primary-themed colors.
- Set its label through `stringResource(R.string.save_and_close)`.
- Set `enabled = isCodeValid`, reusing the existing local validation result based on `TriggerMatcher.isValidCode`.
- Always invoke `viewModel.saveTriggerCode(triggerCodeInput)` when clicked, including when the value is unchanged, then finish the host activity obtained from the existing `LocalContext` by casting it to `android.app.Activity`.
- Because `saveTriggerCode` currently returns the `Job` from `viewModelScope.launch`, await that job's completion before finishing so activity destruction cannot cancel the DataStore write. This can be done from a composition-owned coroutine scope in `MainScreen.kt`; do not change `SettingsViewModel`.
- Add only the required Activity/coroutine imports. Do not add a TopAppBar action, another close button, navigation logic, or changes to `MainActivity.kt`.

### Tests to write

None. JUnit 4 is already configured, and the existing `TriggerMatcherTest` coverage remains unaffected because validation and trigger-matching logic do not change. The palette and icons are visual assets, while Save & Close only composes existing validation, repository saving, and activity finishing; no new JVM-testable behavior is introduced. Manual device/emulator checks are mandatory as listed below.

### Acceptance criteria

- On the user's machine, `./gradlew assembleDebug` and `./gradlew lint` complete successfully. These tasks cannot run in the Conductor sandbox because it has no JDK/Android SDK. Existing `./gradlew test` behavior remains unchanged, with no new tests required.
- `Color.kt` contains no teal constants or values, and `Theme.kt` explicitly maps every role in the palette table for both light and dark schemes. `dynamicColor = false` remains the default.
- `colors.xml` contains exactly `#7A3E1A` for `ic_launcher_background` and `#9A2C16` for `alarm_red`.
- All five self-contained SVGs exist and open directly in a browser. They show the specified distinct concepts on the autumn background without missing assets or font dependencies.
- All five 108dp Android foreground vectors exist, use white artwork within the 18–90 safe zone, and correspond to their SVG previews.
- Variant A is installed as `ic_launcher_foreground.xml`; both adaptive icons reference that active foreground and the new monochrome drawable.
- The new monochrome asset is a readable, white-only variant A silhouette. Adding it resolves the known `MonochromeLauncherIcon` finding; the informational `ObsoleteSdkInt` finding may remain.
- The settings screen shows one full-width "Save & Close" button after the warning cards. It is disabled for blank, whitespace-only, or overlength trigger codes and enabled whenever the existing `isCodeValid` is true.
- With a valid changed code, tapping Save & Close persists the code and finishes `MainActivity`; reopening the app shows the saved code. With an invalid code, the button cannot invoke save or finish.
- The existing per-field trailing Save action continues to work unchanged.
- `AlarmDismissActivity.kt`, `themes.xml`, `Type.kt`, `MainActivity.kt`, `SettingsViewModel.kt`, and `ic_notification.xml` remain unchanged. The dismiss screen picks up `#9A2C16` automatically through its existing resource references.
- The user must perform visual verification on a device/emulator: light and dark autumn rendering, alarm-dismiss urgency and contrast, Save & Close placement in a scrolled column, launcher appearance under square and circular masks, and Android 13+ themed-icon appearance. Browser SVG inspection alone is not sufficient for launcher-mask or monochrome approval.

### Rollback notes

This phase is self-contained in one commit. Reverting it removes the new design/vector assets and Save & Close string/button, restores the teal schemes and previous launcher foreground, and removes the monochrome references. There are no dependencies, database migrations, API changes, or persistent-format changes to unwind. A trigger code already saved by the button remains valid user data and needs no rollback.

### Out of scope and follow-up

- After browser preview and user selection, a small follow-up copies the chosen candidate into `ic_launcher_foreground.xml` and regenerates `ic_launcher_monochrome.xml` from that same variant. Variant A remains the default until then.
- No density-specific PNG/WebP launcher icons are added. The project's API 26+ adaptive vectors are sufficient for this task.
- No typography, navigation, notification-icon, MainActivity, ViewModel, or alarm-dismiss code changes are included.
- No algorithmic Material Theme Builder export is required; the fixed values above define the implementation contract.

## Open Questions

None blocking. All product decisions are confirmed, and the hex values above are the implementation defaults. Any palette refinement after on-device preview is a small visual follow-up, not a prerequisite for this phase. The final icon selection is likewise a confirmed post-preview follow-up; variant A provides the buildable default meanwhile.

## Risks

- **Browser preview differs from the masked launcher result — Likelihood: Medium; Impact: Low.** SVGs show square canvases while launchers apply circular, rounded-square, or other masks. Mitigate by keeping all artwork within the 18–90 safe zone, using identical SVG/vector geometry, and checking the installed icon under multiple launcher masks before final selection.
- **Autumn palette needs aesthetic or contrast refinement — Likelihood: Medium; Impact: Medium.** Fixed hex values can render differently across displays and light/dark schemes. Mitigate by using explicit foreground/background role pairs, retaining separate error and alarm colors, centralizing every value in `Color.kt`/`colors.xml`, and requiring user light/dark and dismiss-screen previews. Any refinement then requires only centralized value edits.
- **Variant A loses readability as a monochrome themed icon — Likelihood: Low; Impact: Medium.** Fine clock or handset details may disappear when reduced to one alpha mask. Mitigate by simplifying to a strong fill-only silhouette, avoiding strokes and tiny cutouts, and verifying on an Android 13+ themed home screen.
- **Finishing immediately could cancel the asynchronous trigger-code write — Likelihood: Low when implemented as specified; Impact: Medium.** `saveTriggerCode` launches in `viewModelScope`, whose lifetime is tied to the activity. Mitigate by awaiting the returned save job before calling `finish()` and manually verifying persistence after an immediate relaunch.
- **Vector/resource mistakes break build or lint — Likelihood: Low; Impact: Medium.** Hand-authored path data and adaptive-icon XML can be malformed or extend beyond the safe zone. Mitigate with simple filled geometry, consistent vector attributes, both mipmap references, and the required `assembleDebug`/`lint` verification on the user's machine.
