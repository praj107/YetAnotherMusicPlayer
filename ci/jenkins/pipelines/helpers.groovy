def parseKeyValueBlock(String raw) {
    raw
        .trim()
        .split('\n')
        .collectEntries { line ->
            def parts = line.split('=', 2)
            [(parts[0]): parts.length > 1 ? parts[1] : '']
        }
}

def resolveSdkRoot(script, String overrideValue = '') {
    def sdkRoot = overrideValue?.trim()
    if (!sdkRoot) {
        sdkRoot = script.env.ANDROID_SDK_ROOT ?: script.env.ANDROID_HOME
    }
    if (!sdkRoot) {
        script.error('ANDROID_SDK_ROOT is not configured on this Jenkins node.')
    }
    script.env.EFFECTIVE_ANDROID_SDK_ROOT = sdkRoot
    script.writeFile file: 'local.properties', text: "sdk.dir=${sdkRoot}\n"
    return sdkRoot
}

def bootstrapAndroid(script, String sdkRootOverride = '', boolean requireGh = true) {
    resolveSdkRoot(script, sdkRootOverride)
    script.sh 'chmod +x gradlew scripts/build-release.sh scripts/ci/*.sh scripts/ci/lib/*.sh scripts/jenkinsctl.py'

    def ghCheck = requireGh ? 'gh --version' : 'true'
    script.sh """
        set -e
        test -d "\$EFFECTIVE_ANDROID_SDK_ROOT/platforms/android-35"
        test -x "\$EFFECTIVE_ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner"
        java -version
        ${ghCheck}
    """
}

def currentVersion(script) {
    parseKeyValueBlock(script.sh(
        returnStdout: true,
        script: 'scripts/ci/current-version.sh'
    ))
}

def bumpVersion(script, String bumpType) {
    parseKeyValueBlock(script.sh(
        returnStdout: true,
        script: "scripts/ci/bump-version.sh ${bumpType}"
    ))
}

def archivePattern(boolean includeReleaseArtifacts = false, boolean includeAndroidTests = false) {
    def parts = ['app/build/reports/**']
    if (includeAndroidTests) {
        parts << 'app/build/outputs/androidTest-results/**'
    }
    if (includeReleaseArtifacts) {
        parts << 'app/build/outputs/apk/**'
        parts << 'app/build/outputs/bundle/**'
        parts << 'app/build/outputs/mapping/**'
        parts << 'releases/**'
    }
    parts.join(', ')
}

def publishReports(script, boolean includeReleaseArtifacts = false, boolean includeAndroidTests = false) {
    def testPatterns = ['app/build/test-results/testDebugUnitTest/*.xml']
    if (includeAndroidTests) {
        testPatterns << 'app/build/outputs/androidTest-results/**/*.xml'
        testPatterns << 'app/build/reports/androidTests/connected/**/*.xml'
    }

    script.junit allowEmptyResults: true, testResults: testPatterns.join(', ')
    script.archiveArtifacts allowEmptyArchive: true, artifacts: archivePattern(includeReleaseArtifacts, includeAndroidTests)

    if (script.fileExists('app/build/reports/lint-results-debug.xml')) {
        script.recordIssues(
            enabledForFailure: true,
            id: 'android-lint-debug',
            name: 'Android Lint Debug',
            tools: [script.androidLintParser(pattern: 'app/build/reports/lint-results-debug.xml')]
        )
    }
    if (script.fileExists('app/build/reports/lint-results-release.xml')) {
        script.recordIssues(
            enabledForFailure: true,
            id: 'android-lint-release',
            name: 'Android Lint Release',
            tools: [script.androidLintParser(pattern: 'app/build/reports/lint-results-release.xml')]
        )
    }
}

return this
