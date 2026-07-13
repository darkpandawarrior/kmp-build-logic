import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin configuring Room 3 (KMP) for a module already using [SharedKmpLibraryConventionPlugin].
 *
 * Applies the `androidx.room3` + `com.google.devtools.ksp` Gradle plugins, points the schema export
 * directory at `<module>/schemas`, and wires `androidx-room-runtime` (commonMain) +
 * `androidx-room-compiler` (via KSP) for the three targets `shared.kmp.library` declares — Android,
 * iosArm64, iosSimulatorArm64. Both library aliases are looked up from the *consuming* project's own
 * version catalog (same pattern as [SharedTestConventionPlugin]) and silently skipped if absent, so
 * this repo's own catalog only needs the two Gradle-plugin coordinates used to compile/apply this
 * plugin.
 *
 * Apply with `id("shared.room")` *after* `id("shared.kmp.library")` (or `shared.kmp.compose`) on any
 * module with a Room database — the KSP per-target configurations (`kspAndroid`, `kspIosArm64`,
 * `kspIosSimulatorArm64`) only exist once the Kotlin Multiplatform targets are already declared.
 */
class SharedRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("androidx.room3")
            apply("com.google.devtools.ksp")
        }

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        libs.findLibrary("androidx-room-runtime").ifPresent { lib ->
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain") {
                    dependencies { implementation(lib.get()) }
                }
            }
        }

        libs.findLibrary("androidx-room-compiler").ifPresent { lib ->
            dependencies {
                add("kspAndroid", lib.get())
                add("kspIosArm64", lib.get())
                add("kspIosSimulatorArm64", lib.get())
            }
        }
    }
}
