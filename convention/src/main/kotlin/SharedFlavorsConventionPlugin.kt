import com.mobilebytelabs.kmpflavors.KmpFlavorExtension
import com.mobilebytelabs.kmpflavors.KmpFlavorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin wiring `kmp-product-flavors` (Android-style product flavors for KMP, with an AGP
 * bridge) with build-type support enabled.
 *
 * Applies the upstream `KmpFlavorPlugin` and seeds `buildConfigPackage` / `appId` / `appDisplayName`
 * from the consuming project's own version catalog `appId` / `appDisplayName` version entries,
 * falling back to this project's group/name if the consumer hasn't declared them.
 *
 * Flavor dimensions, flavors, and build types are deliberately left to each consuming app's own
 * `build.gradle.kts` — this build-logic module is shared by 3 apps with divergent products/branding,
 * so baking one app's specific flavor list (demo/prod, staging, ...) in here would fight every app
 * that doesn't want it. Apply with `id("shared.flavors")`, then configure
 * `extensions.configure<KmpFlavorExtension> { flavorDimensions { ... }; flavors { ... } }` in the
 * consuming module.
 */
class SharedFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(KmpFlavorPlugin::class.java)

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val resolvedAppId =
            libs.findVersion("appId")
                .map { it.requiredVersion }
                .orElse(target.group.toString())
        val resolvedAppDisplayName =
            libs.findVersion("appDisplayName")
                .map { it.requiredVersion }
                .orElse(target.name)

        extensions.configure<KmpFlavorExtension> {
            buildConfigPackage.set(resolvedAppId)
            appId.set(resolvedAppId)
            appDisplayName.set(resolvedAppDisplayName)
            enableBuildTypes.set(true)
        }
    }
}
