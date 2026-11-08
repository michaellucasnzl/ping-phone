# Phase 1 Complete — Autumn theme, five icon candidates, and Save & Close

All work was implemented in a single pass, matching `plans/pingphone-autumn-ui-plan.md`.
No build, lint, or test commands were run (no JDK/Android SDK in this sandbox); the user
must run `./gradlew assembleDebug` and `./gradlew lint` plus the on-device visual checks.

## Files created

| File | Purpose |
|---|---|
| `design/icons/icon_a.svg` … `icon_e.svg` | Five self-contained 192×192 browser previews (viewBox `0 0 108 108`), `#7A3E1A` background, `#FFF8F4` glyph, artwork inside the 18–90 safe zone. |
| `app/src/main/res/drawable/ic_launcher_foreground_a.xml` … `_e.xml` | Matching 108dp / 108-viewport vector foregrounds, `#FFFFFFFF` artwork inside 18–90. |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | Simplified variant-A silhouette (ring, bells, feet, handset; clock hands removed) for Android 13+ themed icons. |

## Files modified

| File | Change |
|---|---|
| `ui/theme/Color.kt` | Removed all six `Ping*` teal constants; added 48 autumn constants (24 roles × Light/Dark) using the exact contract hex values. |
| `ui/theme/Theme.kt` | `lightColorScheme`/`darkColorScheme` now map all 24 roles from the autumn constants; removed hardcoded `Color(…)` values and the now-unused `androidx.compose.ui.graphics.Color` import. `dynamicColor = false`, the `S`-gate dynamic branch, and the `PingPhoneTheme` signature are unchanged. |
| `res/values/colors.xml` | `ic_launcher_background` → `#7A3E1A`, `alarm_red` → `#9A2C16`; comments updated to describe the autumn palette. |
| `res/drawable/ic_launcher_foreground.xml` | Replaced the ringing-phone artwork with variant A geometry (alarm clock + handset). |
| `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | Added `<monochrome android:drawable="@drawable/ic_launcher_monochrome" />` alongside the unchanged background/foreground references. |
| `ui/screen/MainScreen.kt` | Added `rememberCoroutineScope` + `kotlinx.coroutines.launch` + `android.app.Activity` imports and a full-width `Button` as the final settings-column child: saves the trigger code, joins the returned `Job`, then finishes the host activity. `enabled = isCodeValid`. |
| `res/values/strings.xml` | Added `<string name="save_and_close">Save &amp; Close</string>` next to `save`/`test_alarm`. |

## Verification performed

- All 11 new/modified XML/SVG files parse as well-formed XML.
- `Color.kt`: 48 constants, all values match the contract table exactly; zero teal leftovers.
- `Theme.kt`: 48 role assignments across both schemes; no leftover hardcoded `Color(...)`.
- SVG path data is byte-identical (whitespace-normalised) to the corresponding Android vector for all five variants.
- Endpoint scan of every Android vector confirms all geometry lies inside the 18–90 safe zone (`foreground`/A/monochrome x:[27,77.8] y:[19,89]; B x:[40,81]; C x:[34,86]; D x:[40,76] badge radius → max 87; E x:[31.2,88.1], plus ≤1.5 stroke bleed → 89.6).
- `git status` shows only the files listed above were touched.

## Deviations from the plan

- **Variant A handset form:** implemented as a filled rounded bar plus two circular cups (fill-only), separated from the clock by a small negative-space gap. This keeps the handset fill-only and satisfies "distinguishable through negative space" without overlapping white-on-white shapes.
- **Strokes:** variant B's three sound-wave arcs and variant E's three sound rays use `strokeColor` + round caps (the plan permits this for sound waves); everything else is fill-only. `ic_launcher_monochrome.xml` is entirely fill-only.

## Notes

- **Tests:** the plan explicitly specifies none — this phase only changes visual assets and a button that composes existing validation, repository saving, and `Activity.finish()`, so there is no new JVM-testable behaviour.
- **Out-of-scope observations:** none.
- **Security-relevant:** none (no auth, secrets, network, deserialisation, or new dependencies; the only new file I/O is none — the DataStore write is the pre-existing `SettingsRepository` call).
