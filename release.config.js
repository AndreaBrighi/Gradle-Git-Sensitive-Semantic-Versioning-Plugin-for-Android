var publishCmd = `
git tag -a -f \${nextRelease.version} \${nextRelease.version} -F CHANGELOG.md || exit 1
git push --force origin \${nextRelease.version} || exit 3
`
var config = require('semantic-release-preconfigured-conventional-commits');
config.plugins.push(
    ["@semantic-release/exec", {
        "publishCmd": publishCmd,
    }],
    ["@semantic-release/github", {
        "assets": [
            { "path": "app/build/outputs/apk/release/*.apk" },
        ]
    }],
    "@semantic-release/git",
)
config.branches = [ "main" ]
module.exports = config