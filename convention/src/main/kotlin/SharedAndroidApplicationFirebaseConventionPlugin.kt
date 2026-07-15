import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin wiring Firebase into an Android application module (apply *after*
 * [SharedAndroidApplicationConventionPlugin]).
 *
 * Applies the `com.google.gms.google-services` + `com.google.firebase.crashlytics` Gradle plugins,
 * wires the Firebase BOM + Analytics + Crashlytics runtime libraries, and enables Crashlytics
 * mapping-file upload for every build type. The three runtime libraries are looked up from the
 * *consuming* project's own version catalog (same `findLibrary(...).ifPresent {}` pattern as
 * [SharedRoomConventionPlugin]) and silently skipped if absent, so this repo's catalog only needs
 * the two Gradle-plugin coordinates used to compile/apply this plugin.
 *
 * Apply with `id("shared.android.firebase")`. Mapping-file upload only actually runs when a Firebase
 * backend is configured (a valid `google-services.json` is present).
 */
class SharedAndroidApplicationFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.gms.google-services")
            apply("com.google.firebase.crashlytics")
        }

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        dependencies {
            libs.findLibrary("firebase-bom").ifPresent { bom ->
                add("implementation", platform(bom.get()))
            }
            libs.findLibrary("firebase-analytics").ifPresent { lib ->
                add("implementation", lib.get())
            }
            libs.findLibrary("firebase-crashlytics").ifPresent { lib ->
                add("implementation", lib.get())
            }
        }

        extensions.configure<ApplicationExtension> {
            buildTypes.configureEach {
                // Upload the Crashlytics mapping file so release stack traces are symbolicated.
                // Only effective when a Firebase backend is configured via google-services.json.
                configure<CrashlyticsExtension> {
                    mappingFileUploadEnabled = true
                }
            }
        }
    }
}
