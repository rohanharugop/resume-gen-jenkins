pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    environment {
        DOCKER_IMAGE = "resume-ai-app"
        DOCKER_TAG = "${BUILD_NUMBER}"
        GROQ_API_KEY = credentials('groq-api-key')
        JAVA_HOME = tool('jdk21')
        PATH = "${JAVA_HOME}\\bin;${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/rohanharugop/AI-Resume-Maker.git'
            }
        }

        // 🔹 NEW STAGE: Build Info
        stage('Build Info') {
            steps {
                echo "Build Number: ${BUILD_NUMBER}"
                echo "Branch: ${env.GIT_BRANCH}"
                echo "Build Started At: ${new Date()}"
            }
        }

        // 🔹 NEW STAGE: Verify Tools
        stage('Verify Tools') {
            steps {
                bat 'java -version'
                bat 'mvn -v'
                bat 'node -v'
                bat 'npm -v'
            }
        }

        stage('Build Frontend') {
            steps {
                dir('resume_frontend') {
                    bat 'npm install'
                    bat 'npm run build'
                }
            }
        }

        // 🔹 NEW STAGE: Lint Frontend Code (if ESLint is set up)
        stage('Lint Frontend') {
            steps {
                dir('resume_frontend') {
                    bat 'npx eslint . || echo "Lint warnings found."'
                }
            }
        }

        stage('Test Backend') {
            steps {
                dir('resume-ai-builder') {
                    bat 'mvnw.cmd test'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube-server') {
                    withCredentials([string(credentialsId: 'sonar2', variable: 'SONAR_TOKEN')]) {
                        dir('resume-ai-builder') {
                            bat "mvnw.cmd sonar:sonar -Dsonar.projectKey=resume-ai -Dsonar.host.url=http://localhost:9000 -Dsonar.token=%SONAR_TOKEN%"
                        }
                    }
                }
            }
        }

        // 🔹 NEW STAGE: Archive Frontend Build Zip
        stage('Archive Frontend') {
            steps {
                dir('resume_frontend/dist') {
                    bat 'powershell Compress-Archive -Path * -DestinationPath ../resume-frontend.zip'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                bat "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
            }
        }

        stage('Deploy') {
            steps {
                bat '''
                    docker stop resume-app || echo Container not running
                    docker rm resume-app || echo Container not found
                '''
                bat "docker run -d --name resume-app -p 9090:8080 -e GROQ_API_KEY=%GROQ_API_KEY% ${DOCKER_IMAGE}:latest"
            }
        }
    }

    post {
        always {
            bat "docker image prune -f"
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
