
pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3.9.12'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code'
            }
        }

        stage('Build') {
            steps {
                echo 'Building exchange_simulator'
                bat 'mvn clean package'
            }
        }

        stage('Build Result') {
            steps {
                echo 'Build completed successfully!'
            }
        }
    }
}