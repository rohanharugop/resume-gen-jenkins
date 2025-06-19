pipeline {
    agent any
    
    environment {
        // Define your environment variables
        DOCKER_IMAGE = "resume-ai-app"
        DOCKER_TAG = "${BUILD_NUMBER}"
        GROQ_API_KEY = credentials('groq-api-key') // Store in Jenkins credentials
    }
    
    stages {
        stage('Checkout') {
            steps {
                // Checkout code from repository
                git branch: 'main', url: 'https://github.com/rohanharugop/AI-Resume-Maker.git'
            }
        }
        
        stage('Build Frontend') {
            steps {
                script {
                    // Build React frontend
                    bat '''
                        cd resume_frontend
                        npm install
                        npm run build
                    '''
                }
            }
        }
        
        stage('Test Backend') {
            steps {
                script {
                    // Run Spring Boot tests
                    bat '''
                        cd resume-ai-builder
                        ./mvnw test
                    '''
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    // Build Docker image using your existing Dockerfile
                    bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    bat "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    // Stop existing container and start new one
                    bat '''
                        docker stop resume-app || true
                        docker rm resume-app || true
                        docker run -d --name resume-app \
                            -p 8080:8080 \
                            -e GROQ_API_KEY=${GROQ_API_KEY} \
                            ${DOCKER_IMAGE}:latest
                    '''
                }
            }
        }
    }
    
    post {
        always {
            // Clean up old Docker images
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