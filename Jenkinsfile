pipeline {
    agent {
        dockerfile {
            filename 'etc/jenkinsAgent.Dockerfile'
            // additionalBuildArgs  ...
            args '--network=bridge --user root -v $PWD:$PWD \
                    -v /var/run/docker.sock:/var/run/docker.sock --group-add 984 \
                    --memory=6g --cpus=3'
       }
    }

    environment {
        DOCKER_HOST = 'unix:///var/run/docker.sock'
        HSADMINNG_POSTGRES_ADMIN_USERNAME = 'admin'
        HSADMINNG_POSTGRES_RESTRICTED_USERNAME = 'restricted'
        HSADMINNG_MIGRATION_DATA_PATH = 'migration'
    }

    triggers {
        pollSCM('H/1 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage ('Compile') {
            steps {
                sh './gradlew clean processSpring compileJava compileTestJava --no-daemon'
            }
        }

        stage ('Tests') {
            parallel {
                stage('Unit-Tests') {
                    steps {
                        sh './gradlew unitTest  --no-daemon'
                    }
                }
                stage('General-Tests') {
                    steps {
                        sh './gradlew generalTest --no-daemon'
                    }
                }
                stage('Office-Tests') {
                    steps {
                        sh './gradlew officeIntegrationTest --no-daemon'
                    }
                }
                stage('Booking+Hosting-Tests') {
                    steps {
                        sh './gradlew bookingIntegrationTest hostingIntegrationTest --no-daemon'
                    }
                }
                stage('Import-Tests') {
                    steps {
                        sh './gradlew importOfficeData importHostingAssets --no-daemon'
                    }
                }
                stage ('Scenario-Tests') {
                    steps {
                        sh './gradlew scenarioTest --no-daemon'
                    }
                }
            }
        }

        stage ('Check') {
            steps {
                sh './gradlew check -x pitest -x dependencyCheckAnalyze --no-daemon'
            }
        }
    }

    post {
        always {
            // archive test results
            junit 'build/test-results/test/*.xml'

            // archive the JaCoCo coverage report in XML and HTML format
            jacoco(
                execPattern: 'build/jacoco/*.exec',
                classPattern: 'build/classes/java/main',
                sourcePattern: 'src/main/java'
            )

            // archive scenario-test reports in HTML format
            sh '''
                ./gradlew convertMarkdownToHtml
            '''
            archiveArtifacts artifacts:
                    'build/doc/scenarios/*.html, ' +
                    'build/reports/dependency-license/dependencies-without-allowed-license.json',
                allowEmptyArchive: true

            // cleanup workspace
            cleanWs()
        }
    }
}
