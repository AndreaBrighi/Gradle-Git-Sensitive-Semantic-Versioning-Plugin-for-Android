import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

internal class Tests :
    StringSpec(
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
                val project =
                    configuredPlugin(
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
                val project =
                    configuredPlugin(
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
                val project =
                    configuredPlugin("noTagIdentifier.set(\"foo\")") {
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
                val project =
                    configuredPlugin(
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

            "test issues #40 case" {
                val project =
                    configuredPlugin(
                        """
                        minimumVersion.set("2.2.265")
                        versionPrefix.set("v")
                        incrementalCode.set(false)
                        """.trimIndent(),
                    ) {
                        initGit()
                    }
                val result = project.runGradle()
                println(result)
                result shouldContain "2.2.265"
                val resultCode = project.runGradleCode()
                println(resultCode)
                resultCode shouldContain "2002265"
            }
        },
    ) {
    companion object {
        fun folder(closure: Path.() -> Unit) = createTempDirectory("gitSemVerTest").apply(closure)

        fun Path.file(
            name: String,
            content: () -> String,
        ) = resolve(name)
            .createFile()
            .toFile()
            .writeText(content().trimIndent())

        fun Path.runCommand(
            vararg command: String,
            wait: Long = 10,
        ) {
            val process =
                ProcessBuilder(*command)
                    .directory(toFile())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .apply {
                        // git command isolation from the operating system environment
                        environment().let { env ->
                            env.clear()
                            env["PATH"] = System.getenv("PATH")
                            env["HOME"] = resolve(".git/.home.d/").absolutePathString()
                            env["GIT_CONFIG_NOSYSTEM"] = "true"
                        }
                    }.start()
            process.waitFor(wait, TimeUnit.SECONDS)
            require(process.exitValue() == 0) {
                "command '${command.joinToString(" ")}' failed with exit value ${process.exitValue()}"
            }
        }

        fun Path.runCommand(
            command: String,
            wait: Long = 10,
        ) = runCommand(
            *command.split(" ").toTypedArray(),
            wait = wait,
        )

        fun Path.initGit() {
            runCommand("git init")
            runCommand("git add .")
            runCommand("git config user.name gitsemver")
            runCommand("git config user.email none@test.com")
            runCommand("git config init.defaultBranch master")
            runCommand("git config commit.gpgsign no")
            runCommand("git", "commit", "-m", "\"Test commit\"")
        }

        fun Path.moreCommits() {
            file("something") { "something" }
            runCommand("git add .")
            runCommand("git", "commit", "-m", "\"Test commit\"")
        }

        fun Path.runGradle(vararg arguments: String = arrayOf("printGitSemVer", "--stacktrace")): String =
            GradleRunner
                .create()
                .withProjectDir(toFile())
                .withPluginClasspath()
                .withArguments(*arguments)
                .build()
                .output

        fun Path.runGradleCode(vararg arguments: String = arrayOf("printGitSemVerCode", "--stacktrace")): String =
            GradleRunner
                .create()
                .withProjectDir(toFile())
                .withPluginClasspath()
                .withArguments(*arguments)
                .build()
                .output

        fun configuredPlugin(
            pluginConfiguration: String = "",
            otherChecks: Path.() -> Unit = {},
        ): Path =
            folder {
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
