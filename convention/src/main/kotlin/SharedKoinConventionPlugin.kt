import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin wiring core Koin DI dependencies into a Kotlin Multiplatform module's
 * `commonMain`/`commonTest` source sets.
 *
 * Looks up `koin-core` (commonMain) and `koin-test` (commonTest) from the *consuming* project's own
 * version catalog — same lookup pattern as [SharedTestConventionPlugin] /
 * [SharedCmpFeatureConventionPlugin] — and silently skips whichever alias the consumer's catalog
 * doesn't define, so this repo's own catalog never needs a Koin entry. Deferred behind
 * `pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform")` so it's safe regardless of
 * whether `shared.kmp.library` is declared before or after this plugin in the consumer's
 * `plugins {}` block.
 *
 * Modules that also need Compose-aware Koin (koin-compose / koin-compose-viewmodel / koin-android)
 * should apply `shared.cmp.feature` instead, which already wires those.
 *
 * Apply with `id("shared.koin")` on any KMP module that defines or consumes Koin modules outside of
 * a Compose feature (e.g. a shared data/domain module).
 */
class SharedKoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                libs.findLibrary("koin-core").ifPresent { lib ->
                    sourceSets.getByName("commonMain") {
                        dependencies { implementation(lib.get()) }
                    }
                }
                libs.findLibrary("koin-test").ifPresent { lib ->
                    sourceSets.getByName("commonTest") {
                        dependencies { implementation(lib.get()) }
                    }
                }
            }
        }
    }
}
