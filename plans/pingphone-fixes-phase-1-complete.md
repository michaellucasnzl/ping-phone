# Phase 1 Complete: Modernize the build toolchain and dependency catalog

## Files changed

### `gradle/libs.versions.toml` (modified)
- `agp` 8.13.2 → **9.5.0**
- `kotlin` 2.0.21 → **2.4.21**
- `coreKtx` 1.15.0 → **1.19.1**
- `lifecycleRuntimeKtx` 2.8.7 → **2.11.0**
- `activityCompose` 1.9.3 → **1.13.0**
- `composeBom` 2024.12.01 → **2026.09.00**
- `datastorePreferences` 1.1.1 → **1.3.0**
- `material` 1.13.0 → **1.14.0**
- `junitVersion` 1.2.1 → **1.3.0**
- `espressoCore` 3.6.1 → **3.7.0**
- `junit` unchanged at **4.13.2**
- Removed the `kotlin-android` plugin alias from `[plugins]`; retained `android-application` and `kotlin-compose`.

### `app/build.gradle.kts` (modified)
- Removed `alias(libs.plugins.kotlin.android)` from the `plugins { }` block.
- `compileSdk` 35 → **37**; `targetSdk` 35 → **36**; `minSdk` unchanged at 26.
- `versionCode` 1 → **2**; `versionName` "1.0" → **"1.1"**.
- `compileOptions` source/target compatibility 11 → **17**.
- Removed the `kotlinOptions { jvmTarget = "11" }` block.
- Added the AGP 9 built-in Kotlin DSL at top level:
  ```kotlin
  kotlin {
      compilerOptions {
          jvmTarget = JvmTarget.JVM_17
      }
  }
  ```
  with `import org.jetbrains.kotlin.gradle.dsl.JvmTarget` at the top of the file.
- `buildFeatures { compose = true }` and the `dependencies { }` block unchanged.

### `build.gradle.kts` (modified)
- Removed `alias(libs.plugins.kotlin.android) apply false`; retained the Android application and Kotlin Compose `apply false` declarations.

### `settings.gradle.kts` (modified)
- `org.gradle.toolchains.foojay-resolver-convention` 0.10.0 → **1.0.0**. All other content unchanged.

### `gradle/wrapper/gradle-wrapper.properties` (modified)
- `distributionUrl` gradle-8.13-bin.zip → **gradle-9.6.0-bin.zip**. All other lines unchanged.

## Deviations from the plan
- The plan listed `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, and `gradlew.bat` as potentially modified via wrapper regeneration. Per the phase instructions, `./gradlew wrapper` was **not** run (no Android SDK / no build execution in this phase). Only `gradle-wrapper.properties` was edited; the existing wrapper scripts and jar are version-agnostic bootstrap artifacts and will fetch Gradle 9.6.0 on the next build.
- The `kotlin { compilerOptions { } }` DSL uses the documented AGP 9 form with direct property assignment (`jvmTarget = JvmTarget.JVM_17`) rather than `.set(...)`; both are equivalent in the Kotlin DSL. This matches the official "Migrate to built-in Kotlin" guidance.

## Verification performed
- All five files were read back and confirmed syntactically consistent (Kotlin DSL, TOML, properties).
- `git status` confirms exactly the five intended files are modified; no Kotlin source, manifest, resource, or other file was touched.
- No Gradle build, lint, or test command was run. All build/test execution is deferred to the end-of-task verification gate.
