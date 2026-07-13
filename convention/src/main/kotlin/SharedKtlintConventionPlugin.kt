import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin applying the ktlint Gradle plugin with its defaults.
 *
 * Apply with `id("shared.ktlint")` on any module that wants standalone `ktlintCheck` /
 * `ktlintFormat` tasks, as an alternative to Spotless's bundled ktlint integration
 * (`shared.spotless`). Pick one per module — running both against the same sources is redundant.
 */
class SharedKtlintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    }
}
