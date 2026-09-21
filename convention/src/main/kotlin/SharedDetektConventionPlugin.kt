import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * Convention plugin applying Detekt 2.x static analysis (matches the family's `dev.detekt`
 * 2.0.0-alpha.5, not the donor template's legacy `io.gitlab.arturbosch.detekt` 1.x).
 *
 * Apply with `id("shared.detekt")`. Builds on Detekt's default rule config and wires the
 * `detekt-formatting` ruleset (ktlint-style formatting rules run inside Detekt). A consumer that
 * ships its own `config/detekt/detekt.yml` can layer it via the `detekt {}` extension; this
 * convention only sets the shared defaults so every toolkit repo starts from the same baseline.
 *
 * Also excludes everything under a module's `build/generated/` from every Detekt task (the glob is
 * spelled out at the call site — it cannot be written in KDoc, where it would close the comment).
 * KSP/Room/compose-resources output
 * accounted for 4,150 of the family's 6,307 baselined findings — `AgentDao_Impl.kt`'s `__db`
 * fields, `_columnIndexOf*` locals, generated `_collectCommonMainString0Resources` methods.
 * Nobody wrote those names, nobody can change them, and a clean rebuild reproduces them exactly.
 * Doing it here rather than only in `detekt.yml` covers EVERY rule, including rules enabled later
 * that nobody remembers to add the exclusion to. The per-rule `excludes:` in the shared
 * `detekt.yml` stays as well, because Mileway, Kursi and cv-siddharth do not apply this plugin.
 */
class SharedDetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("dev.detekt")

            extensions.configure<DetektExtension> {
                // 2.x exposes these as Gradle Property<Boolean>; precompiled .kt plugins don't get the
                // KTS assign-operator sugar, so use .set().
                buildUponDefaultConfig.set(true)
                allRules.set(false)
            }

            tasks.withType<Detekt>().configureEach {
                exclude("**/build/generated/**")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                libs.findLibrary("detekt-formatting").ifPresent { add("detektPlugins", it.get()) }
            }
        }
}
