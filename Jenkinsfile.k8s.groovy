#!groovy

//String podTemplateConcat = "${serviceName}-${buildNumber}-${uuid}"
def label = "worker-${UUID.randomUUID().toString()}"
println("label")
println("${label}")

podTemplate(
        label: "${label}",
        containers: [
                containerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave:alpine'),
                containerTemplate(name: 'docker', image: 'docker:dind', ttyEnabled: true, alwaysPullImage: true, privileged: true,
                        command: 'dockerd --host=unix:///var/run/docker.sock --host=tcp://0.0.0.0:2375 --storage-driver=overlay'), containerTemplate(name: 'jdk', image: 'openjdk:8-jdk-alpine', command: 'cat', ttyEnabled: true),
//              containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.8', command: 'cat', ttyEnabled: true),
//              containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:latest', command: 'cat', ttyEnabled: true)
        ],
        imagePullSecrets: ["regcred"],
        volumes: [
//                hostPathVolume(hostPath: '/data/volumes/tools/.m2/repository', mountPath: '/root/.m2/repository'),
//        persistentVolumeClaim(claimName: 'repository', mountPath: '/root/.m2/repository'),
hostPathVolume(mountPath: '/home/root/.gradle', hostPath: '/tmp/jenkins/.gradle'),
//                hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
emptyDirVolume(memory: false, mountPath: '/var/lib/docker'),
secretVolume(mountPath: '/etc/.dockercreds', secretName: 'docker-creds')

//                    hostPathVolume(mountPath: '/home/gradle/.gradle', hostPath: '/tmp/jenkins/.gradle')
        ]
//        envVars: [
//                secretEnvVar(key: 'DOCKER_USERNAME', secretName: 'docker-creds', secretKey: 'username'),
//                secretEnvVar(key: 'DOCKER_PASSWORD', secretName: 'docker-creds', secretKey: 'password'),
//        ]
) {

    node("${label}") {

        properties([
                pipelineTriggers([
                        pollSCM('H/10 * * * *')
                ])
        ])


        stage('Configure') {
            container('docker') {
                sh 'echo "Initialize environment"'
                sh """
                QUAY_USER=\$(cat "/etc/.dockercreds/username")
                cat "/etc/.dockercreds/password" | docker login -u \$QUAY_USER --password-stdin quay.io
                """
            }
        }
        stage('Checkout Infra') {
            sh 'mkdir -p ~/.ssh'
            sh 'ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts'
            dir('kubernetes') {
                git branch: "v5", url: 'https://github.com/reportportal/kubernetes.git'

            }
        }

        stage('Checkout Service') {
            dir('app') {
                checkout scm
            }
        }


        dir('app') {
            container('jdk') {
                stage('Build App') {
                    sh "./gradlew build --full-stacktrace"
                }
                stage('Test') {
                    sh "./gradlew test --full-stacktrace"
                }
                stage('Security/SAST') {
                    sh "./gradlew dependencyCheckAnalyze"
                }

                stage('Create Docker Image') {
                    sh "docker build -f docker/Dockerfile-dev-release -t quay.io/reportportal/service-authorozation:BUILD-${env.BUILD_NUMBER} ."
                    sh "docker push quay.io/reportportal/service-authorozation:BUILD-${env.BUILD_NUMBER}"

                }

            }
        }
        stage('Deploy to Dev Environment') {

        }
        stage('Execute Smoke Tests') {

        }

        stage('Deploy to QA Environment') {

        }

        stage('Execute functional tests') {

        }

    }
}

