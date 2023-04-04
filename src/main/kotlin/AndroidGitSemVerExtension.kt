package org.brighiandrea.gradle.androidgitsemver

import org.danilopianini.gradle.gitsemver.GitSemVerExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.File
import kotlin.math.pow

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

    private fun computeIncrementalVersionCode(): Long {
        with(project) {
            return runCommand("git", "rev-list", "--count", "HEAD")
                ?.toLong()
                ?: 0
        }
    }

    private fun computeSemanticVersionCode(): Long {
        if (versionCodeMajorDigits.get() + versionCodeMinorDigits.get() + versionCodePatchDigits.get() > 9) {
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
            (10.0).pow(versionCodeMinorDigits.get() + versionCodePatchDigits.get().toDouble()).toLong()
        val versionCodeMinorPosition = (10.0).pow(versionCodePatchDigits.get().toDouble()).toLong()
        val versionCodePatchPosition = 1L
        if (parts[0].toLong() >= (10.0).pow(versionCodeMajorDigits.get().toDouble()).toLong()) {
            throw IllegalArgumentException(
                "The major version is too big for the versionCode. The maximum value for versionCodeMajorDigits " +
                    "is ${(10.0).pow(versionCodeMajorDigits.get().toDouble()).toLong() - 1}.",
            )
        }
        if (parts[1].toLong() >= (10.0).pow(versionCodeMinorDigits.get().toDouble()).toLong()) {
            throw IllegalArgumentException(
                "The minor version is too big for the versionCode. " +
                    "The maximum value for versionCodeMinorDigits is " +
                    "${(10.0).pow(versionCodeMinorDigits.get().toDouble()).toLong() - 1}.",
            )
        }
        if (parts[2].toLong() >= (10.0).pow(versionCodePatchDigits.get().toDouble()).toLong()) {
            throw IllegalArgumentException(
                "The patch version is too big for the versionCode. " +
                    "The maximum value for versionCodePatchDigits is " +
                    "${(10.0).pow(versionCodePatchDigits.get().toDouble()).toLong() - 1}.",
            )
        }
        return parts[0]
            .toLong() * versionCodeMajorPosition + parts[1]
            .toLong() * versionCodeMinorPosition + parts[2]
            .toLong() * versionCodePatchPosition
    }

    fun computeVersionCode(): Long {
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
