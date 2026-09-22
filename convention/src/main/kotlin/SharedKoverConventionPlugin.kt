import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project

/**
 * Global line-coverage floor for the aggregated report. Deliberately a single number while
 * coverage grows — see the `ponytail:` note at the call site for when to split it per module.
 */
private const val CoverageFloorPercent = 40

/**
 * Self-registering Kover coverage convention plugin.
 *
 * - Applied to a build's root project: applies Kover and configures the shared report filters +
 *   verify rule that gate the aggregated coverage report.
 * - Applied to any leaf module: applies Kover AND self-registers into the root's `kover`
 *   aggregation via `rootProject.dependencies.add("kover", ...)`. Each module opts itself in
 *   by applying `id("shared.kover")` — no central subprojects list to maintain.
 *
 * Apply on the root project first (so its `kover` configuration exists before any subproject
 * tries to register into it), then on any module you want counted.
 */
class SharedKoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
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
                            rule { minBound(CoverageFloorPercent) }
                        }
                    }
                }
            } else {
                // `add("kover", project)` — passing the Project object itself — is deprecated and fails
                // in Gradle 10 ("Using a Project object as a dependency notation"). The replacement
                // Gradle names is DependencyHandler.project(String), which is what the Kotlin-DSL
                // `project(path)` extension below resolves to. Project paths are absolute, so looking
                // it up from the ROOT project's handler still addresses this module.
                rootProject.dependencies.add("kover", rootProject.dependencies.project(path))
            }
        }
}
