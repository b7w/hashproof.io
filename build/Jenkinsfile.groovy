node() {
    def isMaster = env.BRANCH_NAME == "master"

    checkoutStage()
    docker.image("node:8").inside {
        buildStage()
    }
    if (isMaster) {
        docker.image("python:3.6-slim").inside {
            deployStage()
        }
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
        sh("cd src && npm install -g gulp")
        sh("cd src && npm install")
        sh("cd src && gulp")
    }
}


def deployStage() {
    stage("Deploy") {
        withEnv(["XDG_CACHE_HOME=tmp"]) {
            sh("mkdir -p tmp")
            sh("pip3 install python-dateutil ansible boto3")
        }
        def vault = file(credentialsId: "ansible_vault", variable: "VAULT")
        withCredentials([vault]) {
            sh("ansible-playbook build/playbook.yml --extra-vars=@build/secrets.yml --vault-password-file=$VAULT")
        }
    }
}
