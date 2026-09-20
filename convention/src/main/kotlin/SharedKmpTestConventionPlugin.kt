import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Multiplatform sibling of [SharedTestConventionPlugin]: wires the shared test stack into
 * `commonTest`, so it reaches **every** declared target, iOS included.
 *
 * [SharedTestConventionPlugin] adds to `testImplementation`, which is the JVM/Android unit-test
 * configuration only. On a module using `shared.kmp.library` / `shared.kmp.compose` that reaches
 * neither `iosArm64Test` nor `iosSimulatorArm64Test` — an iOS test source set would compile against
 * no test framework at all, silently, with nothing in the build failing to say so. Applying this
 * plugin instead is what gives those source sets a stack.
 *
 * Only the aliases that are genuinely multiplatform are wired here: `kotlin-test`,
 * `kotlinx-coroutines-test`, `turbine` and `koin-test`. JUnit and MockK are JVM-only and stay in
 * [SharedTestConventionPlugin] — putting them in `commonTest` would fail to resolve for the native
 * targets. A module that needs both can apply both plugins.
 *
 * Every alias is looked up in the *consuming* project's own version catalog via
 * `findLibrary(...).ifPresent {}` (same pattern as [SharedKoinConventionPlugin] /
 * [SharedRoomConventionPlugin]) and silently skipped when absent, so this repo's own catalog needs
 * no test entries. The whole body is deferred behind
 * `pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform")`, so it is order-independent with
 * respect to `shared.kmp.library` in the consumer's `plugins {}` block.
 *
 * Apply with `id("shared.kmp.test")`.
 */
class SharedKmpTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                listOf(
                    "kotlin-test",
                    "kotlinx-coroutines-test",
                    "turbine",
                    "koin-test",
                ).forEach { alias ->
                    libs.findLibrary(alias).ifPresent { lib ->
                        sourceSets.getByName("commonTest") {
                            dependencies { implementation(lib.get()) }
                        }
                    }
                }
            }
        }
    }
}
