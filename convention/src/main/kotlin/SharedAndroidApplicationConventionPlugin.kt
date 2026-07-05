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
        }
    }
}
