plugins {
    `kotlin-dsl`
}

group = "com.siddharth.kmp.buildlogic"

java {
    // Convention plugin code itself targets Java 21, not necessarily the modules it configures.
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Must be compileOnly, using implementation causes ClassCastException when the
    // same plugin class is loaded by two different classloaders at different versions.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.kmpFlavors.gradlePlugin)
}

tasks.validatePlugins {
    // Catches accidental use of internal Gradle APIs and missing @TaskAction annotations
    // before they fail at runtime on a different Gradle version.
    enableStricterValidation = true
    failOnWarning = true
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "shared.kmp.library"
            implementationClass = "SharedKmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "shared.kmp.compose"
            implementationClass = "SharedKmpComposeConventionPlugin"
        }
        register("cmpFeature") {
            id = "shared.cmp.feature"
            implementationClass = "SharedCmpFeatureConventionPlugin"
        }
        register("test") {
            id = "shared.test"
            implementationClass = "SharedTestConventionPlugin"
        }
        register("androidApplication") {
            id = "shared.android.application"
            implementationClass = "SharedAndroidApplicationConventionPlugin"
        }
        register("detekt") {
            id = "shared.detekt"
            implementationClass = "SharedDetektConventionPlugin"
        }
        register("ktlint") {
            id = "shared.ktlint"
            implementationClass = "SharedKtlintConventionPlugin"
        }
        register("spotless") {
            id = "shared.spotless"
            implementationClass = "SharedSpotlessConventionPlugin"
        }
        register("kover") {
            id = "shared.kover"
            implementationClass = "SharedKoverConventionPlugin"
        }
        register("koin") {
            id = "shared.koin"
            implementationClass = "SharedKoinConventionPlugin"
        }
        register("room") {
            id = "shared.room"
            implementationClass = "SharedRoomConventionPlugin"
        }
        register("flavors") {
            id = "shared.flavors"
            implementationClass = "SharedFlavorsConventionPlugin"
        }
        register("kmpPure") {
            id = "shared.kmp.pure"
            implementationClass = "SharedKmpPureConventionPlugin"
        }
    }
}
