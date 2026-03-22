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
        booleanParam(
            name: 'RUN_UNIT_TESTS',
            defaultValue: true,
            description: 'Run JVM unit tests before connected-device validation.'
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
                    helpers.bootstrapAndroid(this, params.ANDROID_SDK_ROOT_OVERRIDE, false)
                    def versionInfo = helpers.currentVersion(this)
                    env.RELEASE_VERSION = versionInfo.VERSION_NAME
                    env.RELEASE_TAG = versionInfo.VERSION_TAG
                    currentBuild.displayName = "#${env.BUILD_NUMBER} connected ${env.RELEASE_TAG}"
                }
            }
        }

        stage('Preflight Build') {
            steps {
                sh './gradlew --no-daemon clean assembleDebug assembleDebugAndroidTest'
            }
        }

        stage('Unit Tests') {
            when {
                expression { return params.RUN_UNIT_TESTS }
            }
            steps {
                sh './gradlew --no-daemon testDebugUnitTest'
            }
        }

        stage('Connected Tests') {
            steps {
                sh './gradlew --no-daemon connectedDebugAndroidTest'
            }
        }
    }

    post {
        always {
            script {
                if (helpers != null) {
                    helpers.publishReports(this, false, true)
                } else {
                    junit allowEmptyResults: true, testResults: 'app/build/test-results/testDebugUnitTest/*.xml, app/build/outputs/androidTest-results/**/*.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'app/build/reports/**, app/build/outputs/androidTest-results/**'
                }
            }
            cleanWs deleteDirs: true, disableDeferredWipeout: true
        }
    }
}
