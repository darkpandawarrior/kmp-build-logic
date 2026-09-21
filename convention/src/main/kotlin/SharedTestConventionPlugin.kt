import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin bundling the generic JVM unit-test stack (JUnit, MockK, coroutines-test,
 * Turbine, Koin-test) so modules stop hand-rolling the same `testImplementation(...)` list.
 * Screenshot/Room/Compose-UI extras stay in the consuming module's own build file.
 *
 * Apply with `id("shared.test")` on any module with a `src/test` JVM unit-test source set. Each
 * alias is looked up via [org.gradle.api.artifacts.VersionCatalog.findLibrary] and silently skipped
 * if the consumer's catalog doesn't define it, so this plugin never forces every alias to exist.
 *
 * **JVM/Android only.** `testImplementation` does not reach a multiplatform module's `commonTest`,
 * so on a KMP module this plugin leaves `iosArm64Test` / `iosSimulatorArm64Test` with no test stack
 * and nothing fails to say so. Use [SharedKmpTestConventionPlugin] (`id("shared.kmp.test")`) for
 * those, or both when a module needs JUnit/MockK on the JVM side as well.
 */
class SharedTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            fun testImpl(alias: String) =
                libs.findLibrary(alias).ifPresent { lib ->
                    dependencies { add("testImplementation", lib.get()) }
                }

            testImpl("junit")
            testImpl("mockk")
            testImpl("kotlinx-coroutines-test")
            testImpl("turbine")
            testImpl("koin-test")
            testImpl("koin-test-junit4")
        }
}
