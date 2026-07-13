import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

private const val ktlintVersion = "1.0.1"

/**
 * Convention plugin applying Spotless with a ktlint-based Kotlin/Kotlin-script formatter.
 *
 * Apply with `id("shared.spotless")` on any module. Formats `.kt` and `.kts` sources via ktlint.
 * // ponytail: no license-header wiring — the donor template points at repo-specific
 * // `spotless/copyright.*` files that don't exist here; a consumer app that wants headers can
 * // extend its own `spotless {}` block with `licenseHeaderFile(...)`.
 */
class SharedSpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.diffplug.spotless")

        extensions.configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**/*.kt")
                ktlint(ktlintVersion).editorConfigOverride(mapOf("android" to "true"))
            }
            format("kts") {
                target("**/*.kts")
                targetExclude("**/build/**/*.kts")
            }
        }
    }
}
