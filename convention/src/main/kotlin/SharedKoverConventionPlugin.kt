import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Self-registering Kover coverage convention plugin.
 *
 * - Applied to a build's root project: applies Kover and configures the shared report filters +
 *   verify rule that gate the aggregated coverage report.
 * - Applied to any leaf module: applies Kover AND self-registers into the root's `kover`
 *   aggregation via `rootProject.dependencies.add("kover", project)`. Each module opts itself in
 *   by applying `id("shared.kover")` — no central subprojects list to maintain.
 *
 * Apply on the root project first (so its `kover` configuration exists before any subproject
 * tries to register into it), then on any module you want counted.
 */
class SharedKoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        if (project == rootProject) {
            extensions.configure<KoverProjectExtension> {
                reports {
                    filters {
                        excludes {
                            classes(
                                "*.di.*", // Koin / DI modules
                                "*.BuildConfig",
                                "*ComposableSingletons*", // Compose generated lambda holders
                                "*_*Factory*", // Generated factories
                                "*\$ComposableLambda\$*",
                                "*Preview*", // @Preview functions
                                "*Test*", // test helpers themselves
                            )
                            packages(
                                "*.generated.*",
                                "*.ksp.*",
                            )
                            annotatedBy(
                                // @Composable funcs are better tested via screenshot/UI tests,
                                // not Kover line coverage.
                                "androidx.compose.runtime.Composable",
                            )
                        }
                    }
                    verify {
                        // ponytail: single global floor while coverage grows; split per-module when it matters.
                        rule { minBound(40) }
                    }
                }
            }
        } else {
            rootProject.dependencies.add("kover", project)
        }
    }
}
