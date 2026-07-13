import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for a pure-Kotlin Multiplatform library with no Android or Compose dependency.
 *
 * Applies only the Kotlin Multiplatform plugin and declares `jvm()`, `iosArm64()`,
 * `iosSimulatorArm64()`, and `wasmJs { browser(); nodejs() }` — the wasmJs/JVM target combination
 * [SharedKmpLibraryConventionPlugin] doesn't cover (that one targets Android + iOS only, via the AGP
 * KMP-library plugin). Use for shared leaf modules (engine/domain logic, pure algorithms) that have
 * no platform SDK dependency and need to run on desktop/web as well as iOS.
 *
 * Each consuming module declares its own dependencies and source sets; this convention only wires
 * targets.
 */
class SharedKmpPureConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalWasmDsl::class)
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")

        extensions.configure<KotlinMultiplatformExtension> {
            jvm()
            iosArm64()
            iosSimulatorArm64()
            wasmJs {
                browser()
                nodejs()
            }
        }
    }
}
