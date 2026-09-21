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
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            // AGP 9 provides built-in Kotlin support — applying kotlin.android is no longer needed.
            apply("com.android.library")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
        extensions.configure<LibraryExtension> {
            compileSdk = 37
            defaultConfig { minSdk = 24 }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            buildFeatures {
                compose = true
                buildConfig = false
            }
            testOptions {
                // AGP 9.5.0-alpha06 registers generate<Variant>ComposePreviewRunfiles for every
                // module with compose = true, and that registration hard-fails on AGP's default
                // isIncludeAndroidResources = false. Twelve modules apply this plugin, so this one
                // line is the whole fix. NOTE: it is a behaviour change, not just a flag - unit
                // tests in those modules now run against compiled Android resources.
                unitTests {
                    isIncludeAndroidResources = true
                }
            }
            publishing {
                singleVariant("release") { withSourcesJar() }
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
