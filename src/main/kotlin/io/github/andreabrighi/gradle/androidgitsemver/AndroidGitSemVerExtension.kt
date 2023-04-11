package io.github.andreabrighi.gradle.androidgitsemver

import org.danilopianini.gradle.gitsemver.GitSemVerExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.File
import kotlin.math.pow

/**
 * The plugin extension with the DSL.
 *
 * Supports the following properties:
 * - [minimumVersion], defaulting to 0.1.0
 * - [developmentIdentifier], the identifier for the in-development versions
 * - [noTagIdentifier], the identifier for early versions of the project, when no tags are available yet
 * - [fullHash], whether to use the full commit hash as build metadata
 * - [maxVersionLength], cuts the version to the specified length. Useful for some destinations,
 *      e.g., the Gradle Plugin Portal, which limits version numbers to 20 chars.
 * - [developmentCounterLength], how many digits to use for the counter
 * - [enforceSemanticVersioning], whether the system should fail or just warn
 *      in case a non-SemVer compatible version gets produced
 * - [preReleaseSeparator], how to separate the pre-release information.
 *      Changing this value may generate non-SemVer-compatible versions.
 * - [buildMetadataSeparator], how to separate the pre-release information.
 *      Some destinations (e.g., the Gradle Plugin Portal) do not support the default value '+'.
 *      A reasonable alternative is using '-', lifting the build metadata to a pre-release segment.
 * - [distanceCounterRadix], the radix for the commit counter. Defaults to base 36. Bases from 2 to 36 allowed.
 * - [versionPrefix], to be used in case tags are prefixed with some symbols before the semantic version
 *      (e.g., v1.0.0 is prefixed with "v").
 * - [includeLightweightTags], to be used in case lightweight tags should be considered.
 * - [forceVersionPropertyName], the name of the property that, if set, will force the plugin to use the specified
 *      version. By default, the property name is "forceVersion".
 * - [incrementalCode], whether to use the incremental version code or not (default: true)
 * - [versionCodeMajorDigits], the number of digits to use for the major version (default: 3)
 * - [versionCodeMinorDigits], the number of digits to use for the minor version (default: 3)
 * - [versionCodePatchDigits], the number of digits to use for the patch version (default: 3)
 */
@Suppress("SameParameterValue")
open class AndroidGitSemVerExtension @JvmOverloads constructor(
    private val project: Project,
    minimumVersion: Property<String> = project.propertyWithDefault("0.1.0"),
    developmentIdentifier: Property<String> = project.propertyWithDefault("dev"),
    noTagIdentifier: Property<String> = project.propertyWithDefault("archeo"),
    fullHash: Property<Boolean> = project.propertyWithDefault(false),
    maxVersionLength: Property<Int> = project.propertyWithDefault(Int.MAX_VALUE),
    developmentCounterLength: Property<Int> = project.propertyWithDefault(2),
    enforceSemanticVersioning: Property<Boolean> = project.propertyWithDefault(true),
    preReleaseSeparator: Property<String> = project.propertyWithDefault("-"),
    buildMetadataSeparator: Property<String> = project.propertyWithDefault("+"),
    distanceCounterRadix: Property<Int> = project.propertyWithDefault(DEFAULT_RADIX),
    versionPrefix: Property<String> = project.propertyWithDefault(""),
    includeLightweightTags: Property<Boolean> = project.propertyWithDefault(true),
    forceVersionPropertyName: Property<String> = project.propertyWithDefault("forceVersion"),
    val incrementalCode: Property<Boolean> = project.propertyWithDefault(true),
    val versionCodeMajorDigits: Property<Int> = project.propertyWithDefault(3),
    val versionCodeMinorDigits: Property<Int> = project.propertyWithDefault(3),
    val versionCodePatchDigits: Property<Int> = project.propertyWithDefault(3),
) : GitSemVerExtension(
    project,
    minimumVersion,
    developmentIdentifier,
    noTagIdentifier,
    fullHash,
    maxVersionLength,
    developmentCounterLength,
    enforceSemanticVersioning,
    preReleaseSeparator,
    buildMetadataSeparator,
    distanceCounterRadix,
    versionPrefix,
    includeLightweightTags,
    forceVersionPropertyName,
) {

    /**
     * Computes the version code using the number of commits.
     */
    private fun computeIncrementalVersionCode(): Int {
        with(project) {
            return runCommand("git", "rev-list", "--count", "HEAD")
                ?.toInt()
                ?: 0
        }
    }

    /**
     * Computes the version code using the semantic version.
     */
    private fun computeSemanticVersionCode(): Int {
        if (versionCodeMajorDigits.get() + versionCodeMinorDigits.get() + versionCodePatchDigits.get() > MAX_DIGITS) {
            throw IllegalArgumentException(
                "The sum of versionCodeMajorDigits, versionCodeMinorDigits and versionCodePatchDigits " +
                    "must be less than 9 the greatest value Google Play allows for versionCode is 2100000000.",
            )
        }
        val parts =
            computeVersion()
                .split(preReleaseSeparator.get())[0]
                .split(".")
        val versionCodeMajorPosition =
            (10.0).pow(versionCodeMinorDigits.get() + versionCodePatchDigits.get().toDouble()).toInt()
        val versionCodeMinorPosition = (10.0).pow(versionCodePatchDigits.get().toDouble()).toInt()
        val versionCodePatchPosition = 1
        if (parts[0].toInt() >= (10.0).pow(versionCodeMajorDigits.get().toDouble()).toInt()) {
            throw IllegalArgumentException(
                "The major version is too big for the versionCode. The maximum value for versionCodeMajorDigits " +
                    "is ${(10.0).pow(versionCodeMajorDigits.get().toDouble()).toInt() - 1}.",
            )
        }
        if (parts[1].toInt() >= (10.0).pow(versionCodeMinorDigits.get().toDouble()).toLong()) {
            throw IllegalArgumentException(
                "The minor version is too big for the versionCode. " +
                    "The maximum value for versionCodeMinorDigits is " +
                    "${(10.0).pow(versionCodeMinorDigits.get().toDouble()).toInt() - 1}.",
            )
        }
        if (parts[2].toInt() >= (10.0).pow(versionCodePatchDigits.get().toDouble()).toInt()) {
            throw IllegalArgumentException(
                "The patch version is too big for the versionCode. " +
                    "The maximum value for versionCodePatchDigits is " +
                    "${(10.0).pow(versionCodePatchDigits.get().toDouble()).toInt() - 1}.",
            )
        }
        return parts[0]
            .toInt() * versionCodeMajorPosition + parts[1]
            .toInt() * versionCodeMinorPosition + parts[2]
            .toInt() * versionCodePatchPosition
    }

    /**
     * Computes the version code.
     */
    fun computeVersionCode(): Int {
        return if (incrementalCode.get()) {
            computeIncrementalVersionCode()
        } else {
            computeSemanticVersionCode()
        }
    }

    companion object {

        /**
         * The name of the extension, namely of the DSL entry-point.
         */
        const val EXTENSION_NAME = "androidGitSemVer"

        private const val DEFAULT_RADIX = 36

        private const val MAX_DIGITS = 9

        private inline fun <reified T> Project.propertyWithDefault(default: T): Property<T> =
            objects.property(T::class.java).apply { convention(default) }

        private fun Project.runCommand(vararg cmd: String) = projectDir.runCommandInFolder(*cmd)

        private fun File.runCommandInFolder(vararg cmd: String) = Runtime.getRuntime()
            .exec(cmd, emptyArray(), this)
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
            .takeIf { it.isNotEmpty() }
    }
}
