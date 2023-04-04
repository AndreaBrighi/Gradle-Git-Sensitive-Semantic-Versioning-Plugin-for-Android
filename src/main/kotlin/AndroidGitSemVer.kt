package org.brighiandrea.gradle.androidgitsemver

import org.danilopianini.gradle.gitsemver.GitSemVer
import org.danilopianini.gradle.gitsemver.SemanticVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidGitSemVer : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            /*
             * Recursively scan project directory. If git repo is found, rely on GitSemVerExtension to inspect it.
             */
            val extension =
                project.createExtension<AndroidGitSemVerExtension>(AndroidGitSemVerExtension.EXTENSION_NAME, project)
            project.afterEvaluate {
                with(extension) {
                    properties[extension.forceVersionPropertyName.get()]?.let {
                        require(SemanticVersion.semVerRegex.matches(it.toString())) {
                            "The version '$it' is not a valid semantic versioning format"
                        }
                        project.logger.lifecycle(
                            "Forcing version to $it, mandated by property '$forceVersionPropertyName'",
                        )
                        project.version = it.toString()
                    } ?: run { assignGitSemanticVersion() }
                }
            }
            tasks.register("printGitSemVer") {
                it.doLast {
                    println(
                        "Version computed by ${GitSemVer::class.java.simpleName}: " +
                            "${properties[extension.forceVersionPropertyName.get()] ?: extension.computeVersion()}",
                    )
                }
            }

            tasks.register("printGitSemVerCode") {
                it.doLast {
                    println(
                        "Version Code computed by " +
                            "${GitSemVer::class.java.simpleName}: " +
                            "${properties[extension.forceVersionPropertyName.get()] ?: extension.computeVersionCode()}",
                    )
                }
            }
        }
    }

    companion object {
        private inline fun <reified T> Project.createExtension(name: String, vararg args: Any?): T =
            project.extensions.create(name, T::class.java, *args)
    }
}
