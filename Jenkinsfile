#!/usr/bin/env groovy

node {
    withEnv(["PATH=$HOME/bin:$PATH"]) {
        stage('checkout') {
            checkout scm
        }

        stage('check java') {
            sh "java -version"
        }

        stage('clean+spotless') {
            sh "chmod +x gradlew"
            sh "./gradlew clean spotlessCheck --no-daemon"
        }

        stage('npm install') {
            sh "./gradlew npm_install -PnodeInstall --no-daemon"
        }

        stage('backend tests') {
            try {
                sh "./gradlew test -PnodeInstall --no-daemon"
            } catch (err) {
                throw err
            } finally {
                junit '**/build/**/TEST-*.xml'
            }
        }

        stage('backend check') {
            try {
                sh "./gradlew check -PnodeInstall --no-daemon"
            } catch (err) {
                throw err
            } finally {
                archiveArtifacts artifacts: '**/build/reports/jacoco/test/html/', fingerprint: true
            }
        }

        stage('frontend tests') {
            try {
                sh "./gradlew npm_run_test -PnodeInstall --no-daemon"
            } catch (err) {
                throw err
            } finally {
                junit '**/build/test-results/TESTS-*.xml'
            }
        }

        stage('packaging') {
            sh "./gradlew bootWar -x test -Pprod -PnodeInstall --no-daemon"
            archiveArtifacts artifacts: '**/build/libs/*.war', fingerprint: true
        }
    }
}
