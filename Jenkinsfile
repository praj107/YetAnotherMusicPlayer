def parseKeyValueBlock(String raw) {
    raw
        .trim()
        .split('\n')
        .collectEntries { line ->
            def parts = line.split('=', 2)
            [(parts[0]): parts.length > 1 ? parts[1] : '']
        }
}

pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        skipStagesAfterUnstable()
        timestamps()
    }

    parameters {
        choice(
            name: 'RELEASE_TYPE',
            choices: ['ci', 'patch', 'minor', 'major', 'chore'],
            description: 'ci runs quality gates only; the other modes create a tagged GitHub release.'
        )
        booleanParam(
            name: 'RUN_LINT',
            defaultValue: true,
            description: 'Run Android lint as part of the quality gate.'
        )
        booleanParam(
            name: 'RUN_CONNECTED_TESTS',
            defaultValue: false,
            description: 'Run connected Android tests when an emulator/device is available on the agent.'
        )
        string(
            name: 'ANDROID_SDK_ROOT_OVERRIDE',
            defaultValue: '',
            description: 'Optional SDK path override. Leave empty to use controller/agent environment variables.'
        )
    }

    environment {
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle-home"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Bootstrap') {
            steps {
                script {
                    def sdkRoot = params.ANDROID_SDK_ROOT_OVERRIDE?.trim()
                    if (!sdkRoot) {
                        sdkRoot = env.ANDROID_SDK_ROOT ?: env.ANDROID_HOME
                    }
                    if (!sdkRoot) {
                        error('ANDROID_SDK_ROOT is not configured on this Jenkins node.')
                    }
                    env.EFFECTIVE_ANDROID_SDK_ROOT = sdkRoot
                    writeFile file: 'local.properties', text: "sdk.dir=${sdkRoot}\n"
                }
                sh 'chmod +x gradlew scripts/build-release.sh scripts/ci/*.sh scripts/ci/lib/*.sh'
                sh '''
                    set -e
                    test -d "$EFFECTIVE_ANDROID_SDK_ROOT/platforms/android-35"
                    test -x "$EFFECTIVE_ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner"
                    java -version
                    gh --version
                '''
            }
        }

        stage('Prepare Version') {
            steps {
                script {
                    def versionInfo
                    if (params.RELEASE_TYPE == 'ci') {
                        versionInfo = parseKeyValueBlock(sh(
                            returnStdout: true,
                            script: 'scripts/ci/current-version.sh'
                        ))
                    } else {
                        if (env.BRANCH_NAME && env.BRANCH_NAME != 'main') {
                            error("Release builds are only allowed from main. Current branch: ${env.BRANCH_NAME}")
                        }
                        versionInfo = parseKeyValueBlock(sh(
                            returnStdout: true,
                            script: "scripts/ci/bump-version.sh ${params.RELEASE_TYPE}"
                        ))
                    }
                    env.RELEASE_VERSION = versionInfo.VERSION_NAME
                    env.RELEASE_TAG = versionInfo.VERSION_TAG
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.RELEASE_TAG}"
                }
            }
        }

        stage('Quality Gates') {
            steps {
                sh '''
                    set -e
                    ./gradlew --no-daemon clean testDebugUnitTest assembleDebug
                '''
            }
        }

        stage('Lint') {
            when {
                expression { return params.RUN_LINT }
            }
            steps {
                sh './gradlew --no-daemon lintDebug lintRelease'
            }
        }

        stage('Connected Tests') {
            when {
                expression { return params.RUN_CONNECTED_TESTS }
            }
            steps {
                sh './gradlew --no-daemon connectedDebugAndroidTest'
            }
        }

        stage('Build Release Assets') {
            when {
                expression { return params.RELEASE_TYPE != 'ci' }
            }
            steps {
                withCredentials([
                    file(credentialsId: 'yamp-release-keystore', variable: 'YAMP_KEYSTORE_FILE'),
                    string(credentialsId: 'yamp-keystore-password', variable: 'YAMP_KEYSTORE_PASSWORD'),
                    string(credentialsId: 'yamp-key-alias', variable: 'YAMP_KEY_ALIAS'),
                    string(credentialsId: 'yamp-key-password', variable: 'YAMP_KEY_PASSWORD')
                ]) {
                    sh '''
                        set -e
                        ./gradlew --no-daemon assembleRelease bundleRelease
                        scripts/ci/package-release.sh --version "$RELEASE_VERSION"
                    '''
                }
            }
        }

        stage('Publish Release') {
            when {
                expression { return params.RELEASE_TYPE != 'ci' }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'github-token', variable: 'GH_TOKEN')
                ]) {
                    sh '''
                        set -e
                        git config user.name "Jenkins"
                        git config user.email "jenkins@local"

                        if ! git diff --quiet -- version.properties; then
                            git add version.properties
                            git commit -m "chore: bump version to ${RELEASE_VERSION}"
                        fi

                        if ! git rev-parse "${RELEASE_TAG}" >/dev/null 2>&1; then
                            git tag -a "${RELEASE_TAG}" -m "Release ${RELEASE_TAG}"
                        fi

                        REPO_URL="$(git remote get-url origin)"
                        AUTHED_URL="$(printf '%s' "$REPO_URL" | sed "s#https://#https://x-access-token:${GH_TOKEN}@#")"
                        trap 'git remote set-url origin "$REPO_URL"' EXIT
                        git remote set-url origin "$AUTHED_URL"
                        git push origin HEAD:${BRANCH_NAME:-main} --follow-tags

                        scripts/ci/publish-github-release.sh
                    '''
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'app/build/test-results/testDebugUnitTest/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'app/build/reports/**, releases/**'
            script {
                def lintTools = []
                if (fileExists('app/build/reports/lint-results-debug.xml')) {
                    lintTools << androidLintParser(pattern: 'app/build/reports/lint-results-debug.xml')
                }
                if (fileExists('app/build/reports/lint-results-release.xml')) {
                    lintTools << androidLintParser(pattern: 'app/build/reports/lint-results-release.xml')
                }
                if (!lintTools.isEmpty()) {
                    recordIssues enabledForFailure: true, id: 'android-lint', name: 'Android Lint', tools: lintTools
                }
            }
            cleanWs deleteDirs: true, disableDeferredWipeout: true
        }
    }
}
