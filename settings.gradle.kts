plugins {
    id("com.gradle.enterprise") version "3.16.2"
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishOnFailure()
    }
}

gitHooks {
    preCommit {
        tasks("ktlintCheck")
    }
    commitMsg { conventionalCommits() }
    createHooks(true)
}

rootProject.name = "android-git-sensitive-semantic-versioning-gradle-plugin"
