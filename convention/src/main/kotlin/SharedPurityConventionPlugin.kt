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
 * Configuration-cache safe: the resolved coordinates are read through a `NamedDomainObjectProvider`
 * (a serializable handle), not a live `ConfigurationContainer`, so the task action never derefs the
 * project at execution.
 */
abstract class PurityExtension {
    /** Coordinate substrings that must not appear on the resolved [configuration]. Empty = no-op. */
    abstract val forbidden: ListProperty<String>

    /** Which resolvable configuration to inspect. Defaults to `jvmRuntimeClasspath`. */
    abstract val configuration: Property<String>
}

class SharedPurityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val ext = extensions.create<PurityExtension>("purity")
            ext.configuration.convention("jvmRuntimeClasspath")

            val checkPurity =
                tasks.register("checkPurity") {
                    group = "verification"
                    description = "Fails if the module resolves any forbidden dependency coordinate."
                }
            tasks.named("check") { dependsOn(checkPurity) }

            // afterEvaluate: the configuration name is known and its Configuration exists by now, so we
            // resolve the NamedDomainObjectProvider here (config time) and only map/read it lazily.
            afterEvaluate {
                val configName = ext.configuration.get()
                val cfgProvider = configurations.named(configName)
                val forbiddenProp = ext.forbidden
                val projectPath = path
                checkPurity.configure {
                    val coords =
                        cfgProvider.map { cfg ->
                            cfg.incoming.resolutionResult.allDependencies.map { it.requested.toString() }
                        }
                    doLast {
                        val forbid = forbiddenProp.get()
                        if (forbid.isEmpty()) return@doLast
                        val violations = coords.get().filter { d -> forbid.any { d.contains(it) } }
                        check(violations.isEmpty()) { "PURITY VIOLATION ($projectPath): $violations" }
                    }
                }
            }
        }
    }
}
