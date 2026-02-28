# jenkins-utils-lib

---

## Installazione

- Vai in Manage Jenkins → Configure System
- Scorri fino a Global Pipeline Libraries
- Clicca Add
- Inserisci la libreria con:
    Name: paspiz85-jenkins-utils-lib
    Default version: main
    Load implicitly: OFF
    Allow default version to be overridden: ON
    Include @Library changes in job recent changes: ON
    Cache fetched versions on controller for quick retrieval: OFF
    Repo URL: https://github.com/paspiz85/jenkins-utils-lib.git
- Salva

## Uso nel Jenkinsfile

Se non hai “Load implicitly”:

```
@Library('paspiz85-jenkins-utils-lib') _

pipeline {
  agent any
  environment {
    TELEGRAM_BOT_TOKEN_ID = "telegram-token"
    TELEGRAM_BOT_CHAT_ID = "-100xxx"

  }
  stages {
    stage('Notify') {
      steps {
        script {
          telegram.sendMessage(
            credentialsId: env.TELEGRAM_BOT_TOKEN_ID, // optional
            credentialsId: env.TELEGRAM_BOT_CHAT_ID,  // optional
            message: "Hello World!"
          )
        }
      }
    }
  }
}
```
