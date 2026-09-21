import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for an Android-only *leaf* library module (no KMP targets — e.g. kmp-toolkit's
 * `:security` + its 11 payment-provider modules).
 *
 * Applies AGP library + Compose-compiler plugins and the shared android config identical across
 * those 12 modules: compileSdk 37, minSdk 24, Java 21, Compose enabled, and a single "release"
 * variant published with sources. Module-specific config (namespace, dependencies) stays in the
 * consumer's own `android { }` / `dependencies { }` blocks.
 */
class SharedAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            with(pluginManager) {
                // AGP 9 provides built-in Kotlin support — applying kotlin.android is no longer needed.
                apply("com.android.library")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure<LibraryExtension> {
                compileSdk = CompileSdk
                defaultConfig { minSdk = MinSdk }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
                buildFeatures {
                    compose = true
                    buildConfig = false
                }
                testOptions {
                    // AGP 9.5.0-alpha06's generate<Variant>ComposePreviewRunfiles also hard-fails on
                    // AGP's default isIncludeAndroidResources = false, because Compose Preview needs
                    // compiled Android resources to resolve layout XML and theme attributes.
                    // NOTE: a behaviour change, not just a flag - unit tests in modules applying this
                    // plugin now run against compiled Android resources.
                    unitTests {
                        isIncludeAndroidResources = true
                    }
                }
                publishing {
                    singleVariant("release") { withSourcesJar() }
                }
            }
        }
}
