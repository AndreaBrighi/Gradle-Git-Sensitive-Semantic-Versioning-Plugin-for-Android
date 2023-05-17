import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KOTLIN_VERSION

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

group = "io.github.andreabrighi"
class ProjectInfo {
    val projectId = "$group.$name"
    val fullName = "Gradle Git-Sensitive Semantic Versioning Plugin for Android"
    val projectDetails = "A Gradle plugin that forces semantic versioning in an Android project" +
        " (version name and version code) and relies on git to detect the project state, based on" +
        " Git-Sensitive Semantic Versioning Plugin by Danilo Pianini."
    val pluginImplementationClass = "io.github.andreabrighi.gradle.androidgitsemver.AndroidGitSemVer"

    val websiteUrl = "https://github.com/AndreaBrighi/Gradle-Git-Sensitive-Semantic-Versioning-Plugin-for-Android"
    val vcsUrl = "https://github.com/AndreaBrighi/Gradle-Git-Sensitive-Semantic-Versioning-Plugin-for-Android.git"

    // val scm = "scm:git:$websiteUrl.git"
    val tags = listOf("git", "semver", "semantic versioning", "vcs", "tag", "android")
}

val info = ProjectInfo()

gitSemVer {
    maxVersionLength.set(20)
    buildMetadataSeparator.set("-")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

multiJvm {
    maximumSupportedJvmVersion.set(latestJavaSupportedByGradle)
}

dependencies {
    api(gradleApi())
    api(gradleKotlinDsl())
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.git.sem.ver)
    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.kotlin.testing)
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
            useVersion(KOTLIN_VERSION)
            because("All Kotlin modules should use the same version, and compiler uses $KOTLIN_VERSION")
        }
    }
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
        kotlinOptions {
            allWarningsAsErrors = true
            freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn", "-Xinline-classes")
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            // Pass the pwd via -PmavenCentralPwd='yourPassword'
            val mavenCentralPwd: String? by project
            credentials {
                username = "andreabrighi"
                password = mavenCentralPwd
            }
        }
        publications {
            val androidSemanticVersion by creating(MavenPublication::class) {
                from(components["java"])
                // If the gradle-publish-plugins plugin is applied, these are pre-configured
                // artifact(javadocJar)
                // artifact(sourceJar)
                pom {
                    name.set(info.fullName)
                    description.set(info.projectDetails)
                    url.set(
                        "https://github.com/" +
                            "AndreaBrighi/Gradle-Git-Sensitive-Semantic-Versioning-Plugin-for-Android",
                    )
                    licenses {
                        license {
                            name.set("MIT")
                        }
                    }
                    developers {
                        developer {
                            name.set("Andrea Brighi")
                        }
                    }
                    scm {
                        url.set(
                            "https://github.com/" +
                                "AndreaBrighi/Gradle-Git-Sensitive-Semantic-Versioning-Plugin-for-Android.git",
                        )
                        connection.set(
                            "https://github.com/" +
                                "AndreaBrighi/Gradle-Git-Sensitive-Semantic-Versioning-Plugin-for-Android.git",
                        )
                    }
                }
            }
            signing { sign(androidSemanticVersion) }
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
