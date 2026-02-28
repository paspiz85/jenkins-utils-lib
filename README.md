# jenkins-utils-lib

---

## Installazione

- Vai in Manage Jenkins → Configure System
- Scorri fino a Global Pipeline Libraries
- Clicca Add
- Inserisci la libreria con:
  - Name: my-jenkins-utils-lib
  - Default version: main
  - Load implicitly: OFF
  - Allow default version to be overridden: ON
  - Include @Library changes in job recent changes: OFF (altrimenti ad ogni modifica della libreria partono le modifiche alle pipeline)
  - Cache fetched versions on controller for quick retrieval: OFF
  - Repo URL: https://github.com/paspiz85/jenkins-utils-lib.git
- Salva

## Uso nel Jenkinsfile

Se non hai “Load implicitly”:

```
@Library(value="my-jenkins-utils-lib", changelog=false)

pipeline {
  agent any
  environment {
    TELEGRAM_BOT_TOKEN_ID = "telegram-token"
    TELEGRAM_CHAT_ID = "-100xxx"
  }
  stages {
    stage('Notify') {
      steps {
        script {
          telegram.sendMessage(
            message: "Hello World!"
          )
        }
      }
    }
  }
}
```
