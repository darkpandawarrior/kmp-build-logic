<div align="center">

# kmp-build-logic

### Shared Gradle convention plugins for Kotlin Multiplatform + Compose Multiplatform projects — write the module config once, apply it with one line everywhere.

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84?logo=android&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20MP-1.11.1-4285F4?logo=jetpackcompose&logoColor=white)
![Plugins](https://img.shields.io/badge/plugins-5-success)
![License](https://img.shields.io/badge/license-MIT-blue)

</div>

---

## Why this exists

Every KMP + Compose Multiplatform module ends up re-declaring the same boilerplate: apply Kotlin
Multiplatform, apply the AGP KMP-library plugin, declare the iOS targets, wire the Compose compiler,
pull in the same Koin/lifecycle/navigation baseline for feature modules. Copy-pasting that across
modules — or across *repos* — is exactly the kind of drift a convention plugin exists to kill.

This repo extracts that shared surface out of two production KMP codebases into a standalone,
independently-buildable Gradle composite build: five convention plugins under a neutral `shared.*`
prefix, plus the Compose-compiler-metrics wiring they share. Anything that genuinely diverged between
the source repos (Android-only library defaults, repo-specific desktop/watchOS targets) was left out
on purpose — see [What's deliberately not here](#whats-deliberately-not-here).

## Plugin map

| Plugin ID | Class | Configures |
|---|---|---|
| `shared.kmp.library` | `SharedKmpLibraryConventionPlugin` | Kotlin Multiplatform + AGP KMP-library plugins, `iosArm64()` + `iosSimulatorArm64()` targets |
| `shared.kmp.compose` | `SharedKmpComposeConventionPlugin` | `shared.kmp.library` + Compose Multiplatform + Compose compiler plugins, Compose-metrics wiring |
| `shared.cmp.feature` | `SharedCmpFeatureConventionPlugin` | `shared.kmp.compose` + the standard feature-module dep set (Compose runtime/UI/Material3, Koin, JetBrains navigation-compose, lifecycle-viewmodel, kotlinx-datetime) in `commonMain`/`androidMain` |
| `shared.test` | `SharedTestConventionPlugin` | JVM unit-test stack on `testImplementation`: JUnit, MockK, coroutines-test, Turbine, Koin-test |
| `shared.android.application` | `SharedAndroidApplicationConventionPlugin` | AGP application + Compose-compiler plugins, `compileSdk 37` / Java 21 / Compose enabled (`buildConfig` off by default) |

Every plugin also picks up `configureComposeCompilerMetrics()`: it always wires the *consumer's*
rootProject `compose_stability.conf` if present, and additionally emits Compose compiler
metrics/stability reports under `build/compose-metrics` + `build/compose-reports` when the consumer
build is run with `-Pcompose.metrics`. See [`compose_stability.conf`](compose_stability.conf) in this
repo for the template — copy it into your own project root and edit it.

## Getting started

Add this repo as a submodule and include it as a composite build:

```bash
git submodule add https://github.com/darkpandawarrior/kmp-build-logic.git external/kmp-build-logic
```

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("external/kmp-build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Apply a plugin in any module's build file:

```kotlin
// core/data/build.gradle.kts
plugins {
    id("shared.kmp.library")
}

android {
    namespace = "com.example.core.data"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
}
```

`shared.cmp.feature` and `shared.kmp.compose` resolve their library dependencies from **your own**
version catalog (`libs`), so your project's `gradle/libs.versions.toml` must define the aliases they
reference — see the plugin map above and each plugin's KDoc for the exact alias names.

## Tech stack

| Layer | Version |
|---|---|
| Kotlin | 2.4.0 |
| Android Gradle Plugin | 9.2.1 |
| Compose Multiplatform | 1.11.1 |
| Gradle | 9.6.1 |
| JDK | 21 (resolved automatically via the foojay toolchain resolver if not installed) |

## Building standalone

This repo is a self-contained composite build with no consumer project required:

```bash
git clone https://github.com/darkpandawarrior/kmp-build-logic.git
cd kmp-build-logic
./gradlew build
```

## What's deliberately not here

- **`AndroidLibraryConventionPlugin`** — genuinely diverges between the two source repos
  (`minSdk`/Java target differ), so it stays local to each consumer instead of being papered over.
- **`AndroidProviderConventionPlugin`, `KmpLibraryWatchosConventionPlugin`, `KmpDesktopConventionPlugin`**
  — repo-specific targets (a payment-provider module shape, watchOS, JVM desktop) out of scope for a
  shared surface two arbitrary KMP projects would both want.
