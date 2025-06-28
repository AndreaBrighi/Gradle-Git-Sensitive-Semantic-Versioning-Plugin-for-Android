plugins {
    id("com.gradle.develocity") version "3.17"
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.26"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-use"
        termsOfUseAgree = "yes"
        publishing.onlyIf { it.buildResult.failures.isNotEmpty() }
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
