node() {
    checkoutStage()
    docker.image("node:8").inside {
        buildStage()
    }
    docker.image("python:3.6-slim").inside {
        deployStage()
    }
}


def checkoutStage() {
    stage("Checkout") {
        checkout scm
    }
}


def buildStage() {
    stage("Build") {
        sh("rm -rf target")
        dir("src") {
            sh("npm install")
            sh("gulp")
        }
    }
}


def deployStage() {
    stage("Deploy") {
        withEnv(["XDG_CACHE_HOME=tmp", "ANSIBLE_HOST_KEY_CHECKING=False"]) {
            sh("mkdir -p tmp")
            sh("pip3 install python-dateutil ansible boto3")
        }
        dir("build") {
            def vault = file(credentialsId: "ansible_vault", variable: "VAULT")
            withCredentials([vault]) {
                sh("ansible-playbook playbook.yml --extra-vars=@secrets.yml --vault-password-file=$VAULT")
            }
        }
    }
}
