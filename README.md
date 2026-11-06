# kmp-build-logic

A Gradle composite build holding 18 convention plugins for Kotlin Multiplatform and Compose Multiplatform modules. A module applies one plugin id, like `shared.kmp.library`, instead of repeating the same target, compiler and test setup in every build file.

API docs for the plugins are generated with Dokka and published at [darkpandawarrior.github.io/kmp-build-logic](https://darkpandawarrior.github.io/kmp-build-logic/).

## Why it exists

I kept pasting the same block into every KMP module: the AGP multiplatform-library plugin, the iOS targets, the Compose compiler, the Koin and test baseline, detekt and ktlint. Copies drift: an app built against a different `compileSdk` than its libraries is how "works in the app, fails in the library" starts. This repo is that block, written once, with the reasons for each choice left in the KDoc next to the code.

It is vendored as a git submodule at `external/kmp-build-logic` and pulled in with `includeBuild`. Public repos that do this today: [Doori](https://github.com/darkpandawarrior/Doori), [PaymentsLab-KMP](https://github.com/darkpandawarrior/PaymentsLab-KMP), [Gaddi](https://github.com/darkpandawarrior/Gaddi) and [kmp-app-template](https://github.com/darkpandawarrior/kmp-app-template). [kmp-toolkit](https://github.com/darkpandawarrior/kmp-toolkit) includes it as a sibling checkout (`includeBuild("../kmp-build-logic")`). One private app uses it too.

## Use it

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

Your own version catalog needs the aliases the plugins look up (Koin, Room, Firebase and so on). The plugins read them by name at configuration time, so a missing alias fails with a `NoSuchElementException` when Gradle configures the module, not at compile time.

## The plugins

The three KMP plugins chain: `shared.kmp.library`, then `shared.kmp.compose` applies it and adds Compose, then `shared.cmp.feature` applies that and adds the usual feature-module dependencies. Everything else stands alone, except `shared.android.firebase`, which must come after `shared.android.application`.

| Plugin id | What it sets up |
|---|---|
| `shared.kmp.library` | Kotlin Multiplatform plus AGP's `com.android.kotlin.multiplatform.library`, `iosArm64` and `iosSimulatorArm64`, JDK 25 toolchain |
| `shared.kmp.compose` | the above plus Compose Multiplatform, the Compose compiler and stability/metrics wiring |
| `shared.cmp.feature` | the above plus Compose runtime/UI/Material3, Koin, navigation-compose, lifecycle-viewmodel, kotlinx-datetime |
| `shared.kmp.pure` | Kotlin Multiplatform only: `jvm()`, both iOS targets, `wasmJs { browser(); nodejs() }`. No Android, for leaf modules with no platform SDK |
| `shared.android.application` | AGP application, Compose compiler, `compileSdk 37`, Java 25 |
| `shared.android.library` | AGP library for an Android-only module, `compileSdk 37`, `minSdk 24`, Java 25, one release variant |
| `shared.android.firebase` | google-services and Crashlytics plugins, Firebase BOM libraries from the consumer's catalog, mapping upload on every build type |
| `shared.android.lint` | Android Lint with XML and SARIF reports and `checkDependencies` |
| `shared.test` | JVM/Android unit test stack on `testImplementation`: JUnit, MockK, coroutines-test, Turbine, Koin-test. Does not reach `commonTest` |
| `shared.kmp.test` | the multiplatform subset (kotlin-test, coroutines-test, Turbine, Koin-test) on `commonTest`, so iOS test source sets get a stack too |
| `shared.detekt` | detekt 2.x with default config plus the formatting ruleset |
| `shared.ktlint` | ktlint Gradle plugin (pick this or `shared.spotless`) |
| `shared.spotless` | Spotless with ktlint-based Kotlin formatting |
| `shared.kover` | Kover coverage, filters and verify at the root, leaves register themselves |
| `shared.purity` | a `checkPurity` task, wired into `check`, that fails if a forbidden coordinate resolves on a configurable classpath (default `jvmRuntimeClasspath`) |
| `shared.koin` | Koin core in `commonMain` and `commonTest` for non-Compose modules |
| `shared.room` | Room 3 (KMP) with KSP and schema export, across Android and both iOS targets |
| `shared.flavors` | kmp-product-flavors, so a KMP module gets Android-style flavors and build types |

### Things that bite

- A module only gets iOS targets from the KMP chain or `shared.kmp.pure`. Applying a `shared.android.*` plugin alone yields an Android-only module and nothing reports it.
- No plugin declares an iOS framework binary. One per module would give Xcode a framework per module. Declare it once in the umbrella module of the app.
- `shared.kmp.library` has no `iosX64`. If you add it yourself, `shared.room` still will not wire `kspIosX64`.
- `shared.test` never reaches `commonTest`. Use `shared.kmp.test` for that.
- `shared.kover` does not cover Kotlin/Native, only JVM and Android bytecode.
- The `room` alias in this repo's catalog versions the `androidx.room3` Gradle plugin, a different artifact line from the `androidx.room` 2.8.x runtime your app pins. They move separately.

## Design choices

The plugins are binary plugins written in Kotlin, not precompiled script plugins. That gives real KDoc, an explicit `apply(target: Project)` and a plugin id that does not hang on a file name, at the cost of more boilerplate per plugin.

Catalog lookups go through `VersionCatalogsExtension.findLibrary`, because the generated `libs.foo` accessors do not exist for a plugin class compiled before Gradle knows its project. The plugin classpath dependencies are `compileOnly`; with `implementation` the same plugin can load in two classloaders at two versions and fail at apply time with a `ClassCastException`.

The KMP chain uses AGP's `com.android.kotlin.multiplatform.library` instead of `com.android.library`, since it understands multiplatform source sets. When the workaround was written (AGP 9.4.0-alpha03) it did not package Android assets, so `shared.kmp.compose` carries `configureComposeResourcesAndroidAssetsWorkaround`. This README has not re-checked that against the AGP pinned today.

This repo does not redeclare its own catalog in `settings.gradle.kts`. The file sits at `gradle/libs.versions.toml`, which Gradle registers by itself, and an explicit `from(...)` fails with "Multiple `from` invocations".

## Versions pinned today

Read from `gradle/libs.versions.toml` and `gradle-wrapper.properties`:

| | |
|---|---|
| Kotlin | 2.5.0-Beta2 |
| Android Gradle Plugin | 9.6.0-alpha03 |
| Compose Multiplatform | 1.13.0-beta02 |
| Gradle | 9.9.0-rc-1 |
| detekt | 2.0.0-alpha.6 |
| Room (androidx.room3 plugin) | 3.1.0-alpha03 |
| JDK | 25 |

These are pre-release lines on purpose, so expect them to move. A renovate config keeps them current.

## Build it standalone

No consumer project is needed:

```bash
git clone https://github.com/darkpandawarrior/kmp-build-logic.git
cd kmp-build-logic
./gradlew :convention:check :convention:assemble
```

That is the command CI runs (`.github/workflows/ci.yml`). `check` pulls in detekt, ktlint, `validatePlugins` and the tests. A second workflow builds the Dokka site, and a third rejects commits carrying AI attribution lines.

## Left out on purpose

Repo-specific shapes did not make it in: a payment-provider module plugin, a watchOS plugin, a JVM desktop plugin. App-specific flavor dimensions stay in each app, because the apps sell different products. There is no publishing plugin yet, since every consumer still uses `includeBuild`.

Licence: MIT.
