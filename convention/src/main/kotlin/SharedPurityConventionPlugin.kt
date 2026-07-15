import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/**
 * `shared.purity` — a reusable dependency-purity tripwire. A module declares a list of coordinate
 * substrings that must never appear on its resolved runtime classpath; the registered `checkPurity`
 * task (wired into `check`) fails the build if any do. Keeps a "pure leaf" module (engine / domain /
 * pure algorithms) honest without standing up a full dependency-guard baseline.
 *
 *     plugins { id("shared.purity") }
 *     purity {
 *         forbidden.set(listOf("androidx.compose", "io.ktor", "kotlinx-coroutines"))
 *         // configuration defaults to "jvmRuntimeClasspath"
 *     }
 *
 * Note: like any classpath-resolving verification task, `checkPurity` reads project state at execution
 * time and is therefore not configuration-cache compatible — the same trade-off the inline tripwire it
 * replaces already made.
 */
abstract class PurityExtension {
    /** Coordinate substrings that must not appear on the resolved [configuration]. Empty = no-op. */
    abstract val forbidden: ListProperty<String>

    /** Which resolvable configuration to inspect. Defaults to `jvmRuntimeClasspath`. */
    abstract val configuration: Property<String>
}

class SharedPurityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            val ext = extensions.create<PurityExtension>("purity")
            ext.configuration.convention("jvmRuntimeClasspath")

            val checkPurity =
                tasks.register("checkPurity") {
                    group = "verification"
                    description = "Fails if the module resolves any forbidden dependency coordinate."
                    val forbidden = ext.forbidden
                    val configName = ext.configuration
                    doLast {
                        val forbid = forbidden.get()
                        if (forbid.isEmpty()) return@doLast
                        val cfg = target.configurations.findByName(configName.get()) ?: return@doLast
                        val requested =
                            cfg.incoming.resolutionResult.allDependencies
                                .map { it.requested.toString() }
                        val violations = requested.filter { d -> forbid.any { d.contains(it) } }
                        check(violations.isEmpty()) { "PURITY VIOLATION ($path): $violations" }
                    }
                }
            tasks.named("check") { dependsOn(checkPurity) }
            Unit
        }
}
