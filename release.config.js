var publishCmd = `
git tag -a -f \${nextRelease.version} \${nextRelease.version} -F CHANGELOG.md || exit 1
./gradlew publishPlugins -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET" -PsigningKey="$SIGNING_KEY" -PsigningPassword="$SIGNING_PASSWORD" || exit 2
./gradlew publishPluginMavenPublicationToGithubRepository -PsigningKey="$SIGNING_KEY" -PsigningPassword="$SIGNING_PASSWORD" || exit 3
git push --force origin \${nextRelease.version} || exit 4
`
var config = require('semantic-release-preconfigured-conventional-commits');
config.plugins.push(
    ["@semantic-release/exec", {
        "publishCmd": publishCmd,
    }],
    "@semantic-release/github",
    "@semantic-release/git",
)
config.branches = [ "main" ]
module.exports = config