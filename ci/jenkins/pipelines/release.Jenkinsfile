def helpers = null

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
            choices: ['patch', 'minor', 'major', 'chore'],
            description: 'Semantic bump to apply before packaging and publishing. chore republishes the current version.'
        )
        booleanParam(
            name: 'RUN_LINT',
            defaultValue: true,
            description: 'Run Android lint as part of release validation.'
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
                script {
                    helpers = load 'ci/jenkins/pipelines/helpers.groovy'
                }
            }
        }

        stage('Bootstrap') {
            steps {
                script {
                    helpers.bootstrapAndroid(this, params.ANDROID_SDK_ROOT_OVERRIDE, true)
                }
            }
        }

        stage('Prepare Version') {
            steps {
                script {
                    if (env.BRANCH_NAME && env.BRANCH_NAME != 'main') {
                        error("Release builds are only allowed from main. Current branch: ${env.BRANCH_NAME}")
                    }
                    def versionInfo = helpers.bumpVersion(this, params.RELEASE_TYPE)
                    env.RELEASE_VERSION = versionInfo.VERSION_NAME
                    env.RELEASE_TAG = versionInfo.VERSION_TAG
                    currentBuild.displayName = "#${env.BUILD_NUMBER} release ${env.RELEASE_TAG}"
                }
            }
        }

        stage('Quality Gates') {
            steps {
                sh './gradlew --no-daemon clean testDebugUnitTest assembleDebug'
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
            script {
                if (helpers != null) {
                    helpers.publishReports(this, true, params.RUN_CONNECTED_TESTS)
                } else {
                    junit allowEmptyResults: true, testResults: 'app/build/test-results/testDebugUnitTest/*.xml, app/build/outputs/androidTest-results/**/*.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'app/build/reports/**, app/build/outputs/apk/**, app/build/outputs/bundle/**, app/build/outputs/mapping/**, releases/**'
                }
            }
            cleanWs deleteDirs: true, disableDeferredWipeout: true
        }
    }
}
