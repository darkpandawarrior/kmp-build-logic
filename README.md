<div align="center">

<img src="docs/assets/banner.gif" alt="kmp-build-logic" width="700"/>

### Shared Gradle convention plugins for Kotlin Multiplatform + Compose Multiplatform projects, write the module config once, apply it with one line everywhere.

[![CI](https://github.com/darkpandawarrior/kmp-build-logic/actions/workflows/ci.yml/badge.svg)](https://github.com/darkpandawarrior/kmp-build-logic/actions/workflows/ci.yml)
[![No AI attribution](https://github.com/darkpandawarrior/kmp-build-logic/actions/workflows/no-ai-attribution.yml/badge.svg)](https://github.com/darkpandawarrior/kmp-build-logic/actions/workflows/no-ai-attribution.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-9.5.0--alpha06-3DDC84?logo=android&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20MP-1.13.0--alpha01-4285F4?logo=jetpackcompose&logoColor=white)
![Plugins](https://img.shields.io/badge/plugins-18-success)
![License](https://img.shields.io/badge/license-MIT-blue)

**[API reference](https://darkpandawarrior.github.io/kmp-build-logic/)** · **[Why](#why-this-exists)** · **[Features](#features)** · **[Architecture](#architecture)** · **[Tech stack](#tech-stack)** · **[Getting started](#getting-started)** · **[Roadmap](#roadmap)**

**Portfolio:** [cv-siddharth.vercel.app](https://cv-siddharth.vercel.app/) &nbsp;·&nbsp; **Consumers:** [Doori](https://github.com/darkpandawarrior/Doori) &nbsp;·&nbsp; [PaymentsLab-KMP](https://github.com/darkpandawarrior/PaymentsLab-KMP) &nbsp;·&nbsp; [kmp-toolkit](https://github.com/darkpandawarrior/kmp-toolkit) &nbsp;·&nbsp; [Candidai](https://github.com/darkpandawarrior/Candidai) &nbsp;·&nbsp; [Gaddi](https://github.com/darkpandawarrior/Gaddi) &nbsp;·&nbsp; [kmp-app-template](https://github.com/darkpandawarrior/kmp-app-template)

</div>

---

<details>
<summary><b>Table of contents</b></summary>

- [Why this exists](#why-this-exists)
- [Features](#features)
- [Architecture](#architecture)
  - [Engineering decisions](#engineering-decisions)
  - [Module map](#module-map)
  - [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Building standalone](#building-standalone)
- [Roadmap](#roadmap)
- [What's deliberately not here](#whats-deliberately-not-here)

</details>

> **At a glance**, **18 convention plugins** (KMP chain · Android · testing · quality · DI/data ·
> flavors · Firebase · lint · purity) under one neutral `shared.*` prefix, `:convention`-only
> composite build, consumed today by **6 sibling repos**: Doori, PaymentsLab-KMP, kmp-toolkit,
> Candidai, Gaddi and kmp-app-template.

## Why this exists

Every KMP + Compose Multiplatform module ends up re-declaring the same boilerplate: apply Kotlin
Multiplatform, apply the AGP KMP-library plugin, declare the iOS targets, wire the Compose compiler,
pull in the same Koin/test/quality baseline for every module. Copy-pasting that across modules, or
across *repos*, is exactly the kind of drift a convention plugin exists to kill.

This repo extracts that shared surface out of production KMP codebases into a standalone,
independently-buildable Gradle composite build: 18 convention plugins under a neutral `shared.*`
prefix, plus the Compose-compiler-metrics wiring several of them share. It's vendored as a git
submodule (`external/kmp-build-logic`) and pulled in via `pluginManagement { includeBuild(...) }` by
[**Doori**](https://github.com/darkpandawarrior/Doori),
[**PaymentsLab-KMP**](https://github.com/darkpandawarrior/PaymentsLab-KMP),
[**kmp-toolkit**](https://github.com/darkpandawarrior/kmp-toolkit) (currently at `2.0.0`),
[**Candidai**](https://github.com/darkpandawarrior/Candidai),
[**Gaddi**](https://github.com/darkpandawarrior/Gaddi) and
[**kmp-app-template**](https://github.com/darkpandawarrior/kmp-app-template), so the
Kotlin/AGP/Compose/quality setup isn't hand-copied per project. Anything that genuinely diverges
between consumers (app-specific flavor lists, repo-specific desktop/watchOS targets) was left out
on purpose, see [What's deliberately not here](#whats-deliberately-not-here).

## Features

| Area | Plugin ID | Configures |
|---|---|---|
| **KMP chain** | `shared.kmp.library` | Kotlin Multiplatform + AGP KMP-library plugins, `iosArm64()` + `iosSimulatorArm64()` |
| | `shared.kmp.compose` | `shared.kmp.library` + Compose Multiplatform + Compose compiler plugins, Compose-metrics wiring |
| | `shared.cmp.feature` | `shared.kmp.compose` + the standard feature-module dep set (Compose runtime/UI/Material3, Koin, JetBrains navigation-compose, lifecycle-viewmodel, kotlinx-datetime) |
| | `shared.kmp.pure` | Kotlin Multiplatform only, `jvm()`, `iosArm64()`, `iosSimulatorArm64()`, `wasmJs { browser(); nodejs() }`, for platform-SDK-free leaf modules |
| **Android** | `shared.android.application` | AGP application + Compose-compiler plugins, `compileSdk 37` / Java 21 / Compose enabled |
| | `shared.android.library` | AGP library + Compose-compiler plugins for an Android-only leaf module (e.g. kmp-toolkit's `:security` + its 11 payment-provider modules), `compileSdk 37` / `minSdk 24` / Java 21, single "release" variant with sources |
| | `shared.android.firebase` | Applies after `shared.android.application`, wires `google-services` + `firebase-crashlytics` Gradle plugins, the Firebase BOM/Analytics/Crashlytics runtime libs (looked up from the consumer's own catalog), and enables Crashlytics mapping-file upload for every build type |
| **Testing** | `shared.test` | JVM/Android unit-test stack on `testImplementation`: JUnit, MockK, coroutines-test, Turbine, Koin-test. Does **not** reach `commonTest` |
| | `shared.kmp.test` | Multiplatform sibling of `shared.test`: the KMP-capable subset (kotlin-test, coroutines-test, Turbine, Koin-test) on `commonTest`, so it reaches every declared target including iOS |
| **Quality** | `shared.detekt` | Detekt 2.x static analysis, `buildUponDefaultConfig`, `detekt-formatting` ruleset |
| | `shared.ktlint` | ktlint Gradle plugin with defaults (alternative to `shared.spotless`) |
| | `shared.spotless` | Spotless with ktlint-based Kotlin/Kotlin-script formatting |
| | `shared.kover` | Kover coverage, root project configures filters/verify, every leaf self-registers into the aggregation |
| | `shared.android.lint` | Shared Android Lint config, reuses the app/library Lint extension when present (else applies standalone `com.android.lint`), enables XML + SARIF reports, `checkDependencies`, disables the `GradleDependency` nag |
| | `shared.purity` | Dependency-purity tripwire, a `checkPurity` task (wired into `check`) fails the build if any forbidden coordinate substring resolves on a configurable classpath (default `jvmRuntimeClasspath`); keeps a pure leaf module (engine/domain) honest without a full dependency-guard baseline |
| **DI / data** | `shared.koin` | Koin core DI wired into `commonMain`/`commonTest` for non-Compose modules |
| | `shared.room` | Room 3 (KMP) + KSP, schema export, runtime/compiler wired across Android + iOS targets |
| **Flavors** | `shared.flavors` | `kmp-product-flavors` (Android-style product flavors for KMP) with build-type support |

Every plugin that applies the Compose compiler (`shared.kmp.compose` and transitively
`shared.cmp.feature`, plus `shared.android.application`/`shared.android.library` directly) also
picks up `configureComposeCompilerMetrics()`: it always wires the *consumer's* rootProject
`compose_stability.conf` if present, and additionally emits Compose compiler metrics/stability
reports under `build/compose-metrics` + `build/compose-reports` when run with `-Pcompose.metrics`.
See [`compose_stability.conf`](compose_stability.conf) in this repo for the template.

### Target matrix

Which targets each plugin actually configures. A module gets an iOS target only from a plugin in
the **KMP chain** or `shared.kmp.pure` — applying an `shared.android.*` plugin alone produces an
Android-only module, and nothing in the build reports that as an error.

| Plugin ID | Android | iOS | JVM | wasmJs | How the target set is decided |
|---|---|---|---|---|---|
| `shared.kmp.library` | yes (AGP `com.android.kotlin.multiplatform.library`) | `iosArm64` + `iosSimulatorArm64` | no | no | Declared by the plugin |
| `shared.kmp.compose` | yes | `iosArm64` + `iosSimulatorArm64` | no | no | Inherited, applies `shared.kmp.library` |
| `shared.cmp.feature` | yes | `iosArm64` + `iosSimulatorArm64` | no | no | Inherited, applies `shared.kmp.compose`; deps go to `commonMain` (all targets) + `androidMain` |
| `shared.kmp.pure` | no | `iosArm64` + `iosSimulatorArm64` | `jvm()` | `browser()` + `nodejs()` | Declared by the plugin; no Android by design |
| `shared.android.application` | yes | no | no | no | Android app module, correctly Android-only |
| `shared.android.library` | yes | no | no | no | Android-only leaf library by design; the KMP sibling is `shared.kmp.library` |
| `shared.android.firebase` | yes | no | no | no | Android-app-only, applies after `shared.android.application` |
| `shared.android.lint` | yes | no | JVM fallback | no | Reuses the app/library Lint extension when present, else standalone `com.android.lint` |
| `shared.room` | yes (`kspAndroid`) | `kspIosArm64` + `kspIosSimulatorArm64` | no | no | Follows the host module's targets; KSP configs match `shared.kmp.library`'s set exactly |
| `shared.koin` | inherited | inherited | inherited | inherited | `commonMain`/`commonTest`, so every target the host declares |
| `shared.test` | yes | **no** | yes | no | `testImplementation` only, JVM/Android; does not reach `commonTest` |
| `shared.kmp.test` | inherited | inherited | inherited | inherited | `commonTest`, so every target the host declares |
| `shared.detekt` / `shared.ktlint` / `shared.spotless` | n/a | n/a | n/a | n/a | Source-level, target-agnostic |
| `shared.kover` | yes | no | yes | no | JVM/Android bytecode coverage; Kover does not instrument Kotlin/Native |
| `shared.purity` | n/a | n/a | n/a | n/a | Inspects one resolvable configuration, default `jvmRuntimeClasspath` |
| `shared.flavors` | yes | inherited | inherited | inherited | `kmp-product-flavors` with an AGP bridge, target-agnostic |

**No convention plugin declares a `binaries.framework { }`, and that is deliberate.** A library
convention plugin is applied by every shared module, so declaring a framework binary in one would
produce a framework per module instead of the single umbrella framework Xcode links. The framework
binary belongs in the consuming app's umbrella module's own `build.gradle.kts`. A consumer whose
iOS build produces no framework should look there, not here.

`shared.kmp.library` deliberately omits `iosX64` (Intel simulator). If a consumer adds `iosX64()` in
its own build file, note that `shared.room` will **not** wire a `kspIosX64` dependency for it, its
KSP configurations are the fixed set above.

## Architecture

```mermaid
graph LR
    subgraph chain["KMP chain — each builds on the one before it"]
        KL["shared.kmp.library"]
        KC["shared.kmp.compose"]
        CF["shared.cmp.feature"]
        KL --> KC --> CF
    end

    subgraph standalone["Independent plugins — no chain"]
        KP["shared.kmp.pure"]
        AA["shared.android.application"]
        AL["shared.android.library"]
        FB["shared.android.firebase"]
        T["shared.test"]
        KT["shared.kmp.test"]
        DTK["shared.detekt"]
        KTL["shared.ktlint"]
        SPL["shared.spotless"]
        KOV["shared.kover"]
        LNT["shared.android.lint"]
        PUR["shared.purity"]
        KOI["shared.koin"]
        RM["shared.room"]
        FL["shared.flavors"]
        AA --> FB
    end
```

### Engineering decisions

| Decision | Why | Trade-off |
|---|---|---|
| Binary plugins (`kotlin-dsl` + explicit `gradlePlugin { plugins { register(...) } }`), not precompiled script plugins | Real KDoc, an explicit `apply(target: Project)` body, and a plugin ID independent of the file name | More boilerplate per plugin than a `foo.gradle.kts` auto-mapped ID |
| Version-catalog lookups inside plugin code go through `VersionCatalogsExtension.findLibrary(...)` reflectively, not the generated `libs.xyz` DSL | Type-safe `libs.foo` accessors don't exist for a binary plugin class compiled before Gradle knows which project it applies to | Alias typos surface at configuration time (`NoSuchElementException`), not compile time |
| Plugin-classpath deps (`libs.android.gradlePlugin`, etc.) are `compileOnly`, never `implementation` | Avoids the same plugin class being loaded by two classloaders at two versions, that surfaces as a `ClassCastException` at apply-time, not a build-script error | Every convention plugin author has to remember `compileOnly` |
| `shared.kmp.library` applies AGP's `com.android.kotlin.multiplatform.library`, not classic `com.android.library` | AGP 9's purpose-built plugin for an Android target inside a `kotlin { }` block has multiplatform source-set awareness the classic plugin lacks | As of AGP 9.4.0-alpha03 it has no assets-packaging support, `shared.kmp.compose` carries a documented workaround (`configureComposeResourcesAndroidAssetsWorkaround`) until upstream fixes it |
| `gradle/libs.versions.toml` is *not* re-declared in `settings.gradle.kts` | The file already sits at Gradle's conventional path, so Gradle auto-registers it, an explicit `versionCatalogs { create("libs") { from(...) } }` block fails with "Multiple `from` invocations" | Differs from consumer repos whose catalog lives one directory up, where the explicit block is required |

### Module map

| Module | Contents |
|---|---|
| `:convention` | The only module in this composite build, 18 `Plugin<Project>` classes + `ComposeMetrics.kt`, registered via `gradlePlugin { plugins { ... } }` in `convention/build.gradle.kts` |

### Project structure

```
kmp-build-logic/
├── convention/
│   ├── build.gradle.kts          # kotlin-dsl + gradlePlugin{} registrations
│   └── src/main/kotlin/
│       ├── ComposeMetrics.kt                          # shared metrics/stability helper (not a plugin)
│       ├── SharedKmpLibraryConventionPlugin.kt
│       ├── SharedKmpComposeConventionPlugin.kt
│       ├── SharedCmpFeatureConventionPlugin.kt
│       ├── SharedKmpPureConventionPlugin.kt
│       ├── SharedAndroidApplicationConventionPlugin.kt
│       ├── SharedAndroidLibraryConventionPlugin.kt
│       ├── SharedAndroidApplicationFirebaseConventionPlugin.kt
│       ├── SharedAndroidLintConventionPlugin.kt
│       ├── SharedTestConventionPlugin.kt
│       ├── SharedKmpTestConventionPlugin.kt
│       ├── SharedDetektConventionPlugin.kt
│       ├── SharedKtlintConventionPlugin.kt
│       ├── SharedSpotlessConventionPlugin.kt
│       ├── SharedKoverConventionPlugin.kt
│       ├── SharedPurityConventionPlugin.kt
│       ├── SharedKoinConventionPlugin.kt
│       ├── SharedRoomConventionPlugin.kt
│       └── SharedFlavorsConventionPlugin.kt
├── gradle/libs.versions.toml     # plugin-classpath coordinates only (compileOnly)
├── compose_stability.conf        # template — copy into a consumer's root
└── settings.gradle.kts
```

## Tech stack

| Layer | Version |
|---|---|
| Kotlin | 2.4.20 |
| Android Gradle Plugin | 9.5.0-alpha06 |
| Compose Multiplatform | 1.13.0-alpha01 |
| Gradle | 9.8.0-rc-2 |
| Detekt | 2.0.0-alpha.6 |
| ktlint-gradle | 14.2.0 |
| Spotless | 8.10.2 |
| Kover | 0.9.9 |
| Room (KMP) | 3.1.0-alpha01 (`androidx.room3`, not `androidx.room` 2.x) |
| KSP | 2.3.12 (no longer version-locked to Kotlin) |
| kmp-product-flavors | 2.10.0 |
| google-services | 4.5.0 |
| firebase-crashlytics (Gradle plugin) | 3.0.8 |
| Firebase BOM | 34.19.0 |
| JDK | 21 (resolved automatically via the foojay toolchain resolver if not installed) |

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

Plugins that look up dependencies from a version catalog (`shared.cmp.feature`, `shared.test`,
`shared.kmp.test`, `shared.koin`, `shared.room`, `shared.android.firebase`, `shared.flavors`)
resolve those aliases
from **your own** `gradle/libs.versions.toml` via `VersionCatalogsExtension.findLibrary(...)`
see the [Features](#features) table and each plugin's KDoc for the exact alias names it expects.

## Building standalone

This repo is a self-contained composite build with no consumer project required:

```bash
git clone https://github.com/darkpandawarrior/kmp-build-logic.git
cd kmp-build-logic
./gradlew :convention:validatePlugins :convention:assemble
```

That's also the exact command CI runs (`.github/workflows/ci.yml`).

## Roadmap

**Shipped**
- [x] KMP chain: `shared.kmp.library` → `shared.kmp.compose` → `shared.cmp.feature`, plus standalone `shared.kmp.pure`
- [x] Android application + Android-only library conventions
- [x] Quality stack: `shared.detekt`, `shared.ktlint`, `shared.spotless`, `shared.kover`, `shared.android.lint`
- [x] `shared.kmp.test`, the multiplatform sibling of `shared.test`, so a KMP module's `commonTest`
      (and therefore its iOS test source sets) gets a test stack instead of silently getting none
- [x] `shared.purity`, dependency-purity tripwire (`checkPurity`, wired into `check`) for pure leaf modules
- [x] DI/data: `shared.koin`, `shared.room`
- [x] `shared.flavors` (kmp-product-flavors integration)
- [x] `shared.android.firebase`, google-services + Crashlytics wiring, mapping-file upload
- [x] Compose compiler metrics/stability wiring shared across every Compose-applying plugin
- [x] CI: plugin validation + assemble, plus a commit-message AI-attribution guard
- [x] Grew from 2 to 6 consumers (Doori, PaymentsLab-KMP, kmp-toolkit, Candidai, Gaddi,
      kmp-app-template), including a non-app library consumer (kmp-toolkit) and a shared-vendoring
      pattern (kmp-toolkit itself pulled in as `external/kmp-toolkit` alongside
      `external/kmp-build-logic`)

**Exploring**
- [ ] `shared.android.library` variants for consumers needing more than a single "release" variant
- [ ] A `shared.publishing` convention once a consumer needs to publish artifacts beyond `includeBuild`

## What's deliberately not here

- **`AndroidProviderConventionPlugin`, `KmpLibraryWatchosConventionPlugin`, `KmpDesktopConventionPlugin`**
repo-specific targets (a payment-provider module shape, watchOS, JVM desktop) out of scope for a
  shared surface arbitrary KMP projects would both want.
- **App-specific flavor dimensions/build types**: `shared.flavors` wires the plugin, but the actual
  flavors are left to each consuming app, since the app consumers (Doori, PaymentsLab-KMP, Candidai)
  have divergent products and branding.

---

<div align="center">

**Portfolio:** [cv-siddharth.vercel.app](https://cv-siddharth.vercel.app/) &nbsp;·&nbsp; **Consumers:** [Doori](https://github.com/darkpandawarrior/Doori) &nbsp;·&nbsp; [PaymentsLab-KMP](https://github.com/darkpandawarrior/PaymentsLab-KMP) &nbsp;·&nbsp; [kmp-toolkit](https://github.com/darkpandawarrior/kmp-toolkit) &nbsp;·&nbsp; [Candidai](https://github.com/darkpandawarrior/Candidai) &nbsp;·&nbsp; [Gaddi](https://github.com/darkpandawarrior/Gaddi) &nbsp;·&nbsp; [kmp-app-template](https://github.com/darkpandawarrior/kmp-app-template)

</div>
