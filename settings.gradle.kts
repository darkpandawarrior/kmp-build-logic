pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Resolves Java toolchains (incl. the JDK criteria in gradle/gradle-daemon-jvm.properties):
// detects an installed matching JDK, and can provision one if absent.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // org.jlleitschuh.gradle:ktlint-gradle only publishes to the Gradle Plugin Portal, not
        // Maven Central — needed as a compileOnly dep for SharedKtlintConventionPlugin.
        gradlePluginPortal()
    }
    // NOTE: no explicit `versionCatalogs { create("libs") { from(...) } }` block here — this repo's
    // catalog already lives at the Gradle-conventional `gradle/libs.versions.toml` path, which Gradle
    // auto-registers as the `libs` catalog. Declaring it again explicitly causes a
    // "Multiple 'from' invocations" failure (Gradle only allows one `from()` call per catalog
    // builder, and the implicit auto-registration already claimed it). The donors' build-logic
    // settings files declare this block explicitly because their file lives at a NON-default path
    // relative to the included build (`../gradle/libs.versions.toml`, one level up from `build-logic/`)
    // — that's a real behavioral difference from this repo's flatter, single-build layout, not a
    // divergence between the two donors themselves.
}

rootProject.name = "kmp-build-logic"
include(":convention")
