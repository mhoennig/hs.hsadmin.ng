pipeline {
    agent {
        dockerfile {
            filename 'etc/jenkinsAgent.Dockerfile'
            // additionalBuildArgs  ...
            args '--network=bridge --user root -v $PWD:$PWD -v /var/run/docker.sock:/var/run/docker.sock --group-add 984'
            reuseNode true
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
                stage('Unit-/Integration/Acceptance-Tests') {
                    steps {
                        sh './gradlew check --no-daemon -x pitest -x dependencyCheckAnalyze -x importOfficeData -x importHostingAssets'
                    }
                }
                stage('Import-Tests') {
                    steps {
                        sh './gradlew importOfficeData importHostingAssets --no-daemon'
                    }
                }
                stage ('Scenario-Tests') {
                    steps {
                        sh './gradlew scenarioTests --no-daemon'
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
            archiveArtifacts artifacts: 'doc/scenarios/*.html', allowEmptyArchive: true

            // cleanup workspace
            cleanWs()
        }
    }
}
