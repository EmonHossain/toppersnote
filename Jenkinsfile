pipeline {
    agent {
        docker {
            // Jenkins will automatically run: docker pull & docker run this image
            image 'maven:3.9.6-eclipse-temurin-17'
            // Connects this dynamic container to your compose network
            args '--network jenkins-network' 
        }
    }
    stages {
        stage('Build Project') {
            steps {
                // This command runs inside the freshly booted Maven container
                sh 'mvn clean package'
            }
        }
    }
}