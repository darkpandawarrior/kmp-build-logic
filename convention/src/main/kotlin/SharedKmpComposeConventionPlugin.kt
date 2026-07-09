import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

/**
 * Convention plugin for a Kotlin Multiplatform **Compose** library targeting Android + iOS.
 *
 * Builds on [SharedKmpLibraryConventionPlugin] and adds the Compose Multiplatform + Compose compiler
 * plugins. Consuming modules declare only their `android { namespace/... }` block + source sets.
 */
class SharedKmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("shared.kmp.library")
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
        configureComposeCompilerMetrics()
        configureComposeResourcesAndroidAssetsWorkaround()
    }

    /**
     * ponytail: AGP's `com.android.kotlin.multiplatform.library` plugin doesn't implement Android
     * assets packaging (`variant.sources.assets` is null for its variants as of AGP 9.4.0-alpha03),
     * so CMP's own `copyAndroidMainComposeResourcesToAndroidAssets` task never gets its
     * `outputDirectory` wired and fails validation if it ever runs. We set it here so the task
     * actually produces `composeResources/<package>/...` output that `:app` can pick up (see
     * [configureComposeResourcesAndroidAssetsElements] / the `composeAndroidAssets` config in
     * `app/build.gradle.kts`). Upgrade path: once AGP implements assets for this plugin type
     * (tracked upstream), delete this workaround and the matching `:app` consumption wiring —
     * CMP's built-in `addGeneratedSourceDirectory` path will take over automatically.
     *
     * `CopyResourcesToAndroidAssetsTask` is JVM-public but Kotlin-`internal`, so it can't be
     * referenced by type from this module — hence the reflection on its public getter.
     */
    private fun Project.configureComposeResourcesAndroidAssetsWorkaround() {
        tasks.configureEach {
            if (name == "copyAndroidMainComposeResourcesToAndroidAssets") {
                val outputDirectory =
                    this.javaClass.getMethod("getOutputDirectory").invoke(this) as DirectoryProperty
                outputDirectory.set(layout.buildDirectory.dir("composeResourcesForAndroidAssets"))
            }
        }
    }
}
