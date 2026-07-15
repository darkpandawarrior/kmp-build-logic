import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin applying the shared Android Lint configuration.
 *
 * Reuses the existing `com.android.application` / `com.android.library` Lint extension when present,
 * otherwise applies the standalone `com.android.lint` plugin so non-Android JVM modules can be linted
 * too. Enables XML + SARIF reports (for CI ingestion), turns on `checkDependencies`, and disables the
 * noisy `GradleDependency` check (version-bump nags belong to Renovate, not Lint).
 *
 * Apply with `id("shared.android.lint")`.
 */
class SharedAndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        when {
            pluginManager.hasPlugin("com.android.application") ->
                configure<ApplicationExtension> { lint(Lint::configure) }

            pluginManager.hasPlugin("com.android.library") ->
                configure<LibraryExtension> { lint(Lint::configure) }

            else -> {
                apply(plugin = "com.android.lint")
                configure<Lint>(Lint::configure)
            }
        }
    }
}

private fun Lint.configure() {
    xmlReport = true
    sarifReport = true
    checkDependencies = true
    disable += "GradleDependency"
}
