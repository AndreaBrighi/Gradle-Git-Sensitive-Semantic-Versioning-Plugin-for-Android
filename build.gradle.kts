import io.github.andreabrighi.gradle.gitsemver.conventionalcommit.ConventionalCommit
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = libs.versions.kotlin.get()

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.multiJvmTesting)
    signing
    `maven-publish`
    alias(libs.plugins.taskTree)
}

gitSemVer {
    maxVersionLength.set(20)
    buildMetadataSeparator.set("-")
    commitNameBasedUpdateStrategy(ConventionalCommit::semanticVersionUpdate)
}

buildscript {
    dependencies {
        classpath(libs.convetional)
    }
}

group = "io.github.andreabrighi"

class ProjectInfo {
    val projectId = "$group.$name"
    val fullName = "Gradle Git-Sensitive Semantic Versioning Plugin for Android"
    val projectDetails =
        "A Gradle plugin that forces semantic versioning in an Android project" +
            " (version name and version code) and relies on git to detect the project state, based on" +
            " Git-Sensitive Semantic Versioning Plugin by Danilo Pianini."
    val pluginImplementationClass = "io.github.andreabrighi.gradle.androidgitsemver.AndroidGitSemVer"

    val websiteUrl = "https://github.com/AndreaBrighi/Gradle-Git-Sensitive-Semantic-Versioning-Plugin-for-Android"
    val vcsUrl = "https://github.com/AndreaBrighi/Gradle-Git-Sensitive-Semantic-Versioning-Plugin-for-Android.git"
    val scm = "scm:git:$websiteUrl.git"

    // val scm = "scm:git:$websiteUrl.git"
    val tags = listOf("git", "semver", "semantic versioning", "vcs", "tag", "android")
}

val info = ProjectInfo()

repositories {
    mavenCentral()
    gradlePluginPortal()
}

multiJvm {
    jvmVersionForCompilation = 11
    maximumSupportedJvmVersion = latestJavaSupportedByGradle
}

// Enforce Kotlin version coherence
configurations.matching { it.name != "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
            useVersion(kotlinVersion)
            because("All Kotlin modules should use the same version, and compiler uses $kotlinVersion")
        }
    }
}

dependencies {
    api(gradleApi())
    api(gradleKotlinDsl())
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.git.sem.ver)
    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.kotlin.testing)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
            events(*TestLogEvent.values())
        }
    }
    withType<KotlinCompile> {
        compilerOptions {
            allWarningsAsErrors.set(true)
            freeCompilerArgs.addAll(listOf("-opt-in=kotlin.RequiresOptIn", "-Xinline-classes"))
        }
    }
}

publishOnCentral {
    projectDescription.set(info.projectDetails)
    projectLongName.set(info.fullName)
    projectUrl.set(info.websiteUrl)
    scmConnection.set(info.scm)
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                developers {
                    developer {
                        name.set("Andrea Brighi")
                    }
                }
            }
        }
    }
}

if (System.getenv("CI") == "true") {
    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
} else {
    signing {
        useGpgCmd()
        sign(configurations.archives.get())
    }
}

gradlePlugin {
    plugins {
        website.set(info.websiteUrl)
        vcsUrl.set(info.vcsUrl)
        create("long") {
            id = info.projectId
            displayName = info.fullName
            description = info.projectDetails
            implementationClass = info.pluginImplementationClass
            tags.set(info.tags)
        }
        create("short") {
            id = "$group.android-git-sensitive-semantic-versioning"
            displayName = info.fullName
            description = info.projectDetails
            implementationClass = info.pluginImplementationClass
            tags.set(info.tags)
        }
    }
}
