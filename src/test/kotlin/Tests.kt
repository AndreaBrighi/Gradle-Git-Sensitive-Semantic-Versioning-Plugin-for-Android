import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import java.util.concurrent.TimeUnit

internal class Tests : StringSpec(
    {
        "minimal configuration increment version code" {
            val project = configuredPlugin()
            val result = project.runGradle()
            println(result)
            result shouldContain "0.1.0-archeo+"
            val resultCode = project.runGradleCode()
            println(resultCode)
            resultCode shouldContain "0"
        }

        "minimal configuration semantic version code" {
            val project = configuredPlugin("incrementalCode.set(false)")
            val result = project.runGradle()
            println(result)
            result shouldContain "0.1.0-archeo+"
            val resultCode = project.runGradleCode()
            println(resultCode)
            resultCode shouldContain "1000"
        }

        "simple usage of extension increment version code" {
            val project = configuredPlugin("noTagIdentifier.set(\"foo\")")
            val result = project.runGradle()
            println(result)
            result shouldContain "0.1.0-foo+"
            val resultCode = project.runGradleCode()
            println(resultCode)
            resultCode shouldContain "0"
        }

        "simple usage of extension semantic version code" {
            val project = configuredPlugin(
                """
                noTagIdentifier.set("foo")
                preReleaseSeparator.set("-")
                incrementalCode.set(false)
                """.trimIndent(),
            )
            val result = project.runGradle()
            println(result)
            result shouldContain "0.1.0-foo+"
            val resultCode = project.runGradleCode()
            println(resultCode)
            resultCode shouldContain "1000"
        }

        "git single commit semantic version code" {
            val project = configuredPlugin(
                """
                noTagIdentifier.set("foo")
                incrementalCode.set(false)
                """.trimIndent(),
            ) {
                initGit()
            }
            val result = project.runGradle()
            println(result)
            result shouldContain "0.1.0-foo"
            val resultCode = project.runGradleCode()
            println(resultCode)
            resultCode shouldContain "1000"
        }

        "git single commit increment version code" {
            val project = configuredPlugin("noTagIdentifier.set(\"foo\")") {
                initGit()
            }
            val result = project.runGradle()
            println(result)
            result shouldContain "0.1.0-foo"
            val resultCode = project.runGradleCode()
            println(resultCode)
            resultCode shouldContain "1"
        }

        "git multiple commits semantic version code" {
            val project = configuredPlugin(
                """
                noTagIdentifier.set("foo")
                """.trimIndent(),
            ) {
                initGit()
                moreCommits()
            }
            val result = project.runGradle()
            println(result)
            result shouldContain "0.1.0-foo"
            val resultCode = project.runGradleCode()
            println(resultCode)
            resultCode shouldContain "2"
        }
    },
) {
    companion object {

        fun folder(closure: TemporaryFolder.() -> Unit) = TemporaryFolder().apply {
            create()
            closure()
        }

        fun TemporaryFolder.file(name: String, content: () -> String) = newFile(name).writeText(content().trimIndent())

        fun TemporaryFolder.runCommand(vararg command: String, wait: Long = 10) {
            val process = ProcessBuilder(*command)
                .directory(root)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start()
            process.waitFor(wait, TimeUnit.SECONDS)
            require(process.exitValue() == 0) {
                "command '${command.joinToString(" ")}' failed with exit value ${process.exitValue()}"
            }
        }

        fun TemporaryFolder.runCommand(command: String, wait: Long = 10) = runCommand(
            *command.split(" ").toTypedArray(),
            wait = wait,
        )

        fun TemporaryFolder.initGit() {
            runCommand("git init")
            runCommand("git add .")
            runCommand("git config user.name gitsemver")
            runCommand("git config user.email none@test.com")
            runCommand("git config --global init.defaultBranch master")
            runCommand("git", "commit", "-m", "\"Test commit\"")
        }

        fun TemporaryFolder.initGitWithTag() {
            initGit()
            runCommand("git", "tag", "-a", "1.2.3", "-m", "\"test\"")
        }

        fun TemporaryFolder.moreTags() {
            runCommand("git add .")
            runCommand("git", "commit", "-m", "\"Test commit\"")
            runCommand("git", "tag", "-a", "1.2.4", "-m", "\"test\"")
        }

        fun TemporaryFolder.moreCommits() {
            file("something") { "something" }
            runCommand("git add .")
            runCommand("git", "commit", "-m", "\"Test commit\"")
        }

        fun TemporaryFolder.runGradle(
            vararg arguments: String = arrayOf("printGitSemVer", "--stacktrace"),
        ): String = GradleRunner
            .create()
            .withProjectDir(root)
            .withPluginClasspath()
            .withArguments(*arguments)
            .build().output

        fun TemporaryFolder.runGradleCode(
            vararg arguments: String = arrayOf("printGitSemVerCode", "--stacktrace"),
        ): String = GradleRunner
            .create()
            .withProjectDir(root)
            .withPluginClasspath()
            .withArguments(*arguments)
            .build().output

        fun configuredPlugin(
            pluginConfiguration: String = "",
            otherChecks: TemporaryFolder.() -> Unit = {},
        ): TemporaryFolder = folder {
            file("settings.gradle") { "rootProject.name = 'testproject'" }
            file("build.gradle.kts") {
                """
                plugins {
                    id("io.github.andreabrighi.git-semver")
                }
                androidGitSemVer {
                    $pluginConfiguration
                }
               
                """.trimIndent()
            }
            otherChecks()
        }
    }
}
