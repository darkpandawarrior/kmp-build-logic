import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for the Android application module.
 *
 * Applies the AGP application + Compose-compiler plugins and the shared android config (compileSdk
 * 37, Java 21, Compose enabled). App-specific config (applicationId, minSdk/targetSdk, version,
 * buildTypes, testOptions) stays in the app's own `android { }` block.
 *
 * `buildConfig` is **off by default** (matches AGP 9's own default). An app that needs generated
 * `BuildConfig` fields must opt back in itself via `android { buildFeatures { buildConfig = true } }`.
 */
class SharedAndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            // AGP 9 provides built-in Kotlin support — applying kotlin.android is no longer needed.
            apply("com.android.application")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
        configureComposeCompilerMetrics()
        extensions.configure<ApplicationExtension> {
            compileSdk = 37
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            buildFeatures {
                compose = true
                buildConfig = false
            }

            // AGP 9.5.0-alpha06 registers generate<Variant>ComposePreviewRunfiles for every
            // Compose-enabled variant and hard-fails when that variant has no unit-test component.
            // Roborazzi disables the unit-test component on non-debug variants via beforeVariants,
            // so the two plugins contradict each other and the project cannot even configure.
            // Re-enabling the component only registers the task graph — no release unit test is
            // written or run — so Roborazzi's intent is preserved.
            // Remove once AGP stops requiring a unit-test component to generate preview runfiles.
            extensions.configure<com.android.build.api.variant.AndroidComponentsExtension<*, *, *>>("androidComponents") {
                beforeVariants(selector().all()) { variant ->
                    (variant as? com.android.build.api.variant.HasUnitTestBuilder)?.enableUnitTest = true
                }
            }

        }
    }
}
