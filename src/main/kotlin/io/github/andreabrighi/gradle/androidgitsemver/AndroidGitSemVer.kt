package io.github.andreabrighi.gradle.androidgitsemver

import org.danilopianini.gradle.gitsemver.GitSemVer
import org.danilopianini.gradle.gitsemver.SemanticVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/**
 * A Plugin for computing the project version based on the status of the local git repository for android.
 *
 * Compute both the version name and the version code.
 */
class AndroidGitSemVer @Inject constructor(
    private val providerFactory: ProviderFactory,
    private val objectFactory: ObjectFactory,
) : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        /*
         * Recursively scan project directory. If git repo is found, rely on GitSemVerExtension to inspect it.
         */
        val extension = createExtension<AndroidGitSemVerExtension>(
            AndroidGitSemVerExtension.EXTENSION_NAME,
            providerFactory,
            objectFactory,
            projectDir,
            version,
            logger,
        )
        afterEvaluate {
            properties[extension.forceVersionPropertyName.get()]?.let { forceVersion ->
                require(SemanticVersion.semVerRegex.matches(it.toString())) {
                    "The version '$it' is not a valid semantic versioning format"
                }
                logger.lifecycle(
                    "Forcing version to $it, mandated by property '$extension.forceVersionPropertyName'",
                )
                version = forceVersion
            } ?: run { version = extension.assignGitSemanticVersion() }
        }
        tasks.register("printGitSemVer") {
            val forceVersion = properties[extension.forceVersionPropertyName.get()]
            it.doLast {
                println(
                    "Version computed by ${GitSemVer::class.java.simpleName}: " +
                        "${forceVersion ?: extension.computeVersion()}",
                )
            }
        }

        tasks.register("printGitSemVerCode") {
            val forceVersion = properties[extension.forceVersionPropertyName.get()]
            it.doLast {
                println(
                    "Version Code computed by " +
                        "${GitSemVer::class.java.simpleName}: " +
                        "${forceVersion ?: extension.computeVersionCode()}",
                )
            }
        }
    }

    companion object {
        private inline fun <reified T> Project.createExtension(name: String, vararg args: Any?): T =
            project.extensions.create(name, T::class.java, *args)
    }
}
