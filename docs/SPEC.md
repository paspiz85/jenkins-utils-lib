# Specifiche dei moduli

Questo documento descrive i moduli disponibili nella libreria, le funzioni esposte, i parametri accettati e il comportamento atteso.

---

## Modulo `github`

Integrazione con le API REST di GitHub. Permette di recuperare tag, creare release e cercare asset.

### `github.fetchTags`

Recupera i tag dal repository remoto tramite `git fetch --tags`.

**Parametri:**

| Parametro | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|
| `credentialsId` | String | Sì* | `env.GITHUB_CREDS_ID` | ID delle credenziali Jenkins (username/password o token) |
| `credentialId` | String | Sì* | — | Alias di `credentialsId` (compatibilità) |
| `url` | String | Sì* | `env.GIT_URL` | URL del repository GitHub (HTTPS o SSH) |

\* Il parametro è obbligatorio ma può essere fornito tramite variabile d'ambiente.

**Comportamento:**

- Risolve le credenziali nel seguente ordine: `args.credentialsId` → `args.credentialId` → `env.GITHUB_CREDS_ID`.
- Inietta il token nell'URL HTTPS per autenticare `git fetch`.
- Non restituisce alcun valore.

**Errori:**

| Condizione | Messaggio |
|---|---|
| Credenziali mancanti | `[github] 'credentialsId' not provided and GITHUB_CREDS_ID not set` |
| URL mancante | `[github] url not provided and GIT_URL not set` |

**Esempio:**

```groovy
github.fetchTags(credentialsId: 'my-github-creds', url: 'https://github.com/org/repo.git')
```

---

### `github.release`

Crea una release su GitHub per un tag specificato e, opzionalmente, carica asset allegati.

**Parametri:**

| Parametro | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|
| `credentialsId` | String | Sì* | `env.GITHUB_CREDS_ID` | ID delle credenziali Jenkins |
| `credentialId` | String | Sì* | — | Alias di `credentialsId` (compatibilità) |
| `url` | String | Sì* | `env.GIT_URL` | URL del repository GitHub |
| `tag` | String | Sì | — | Tag su cui creare la release |
| `name` | String | No | valore di `tag` | Titolo della release |
| `body` | String | No | valore di `tag` | Descrizione/note della release |
| `draft` | Boolean | No | `false` | Crea la release come bozza |
| `prerelease` | Boolean | No | `false` | Marca la release come pre-release |
| `generate_release_notes` | Boolean | No | `false` | Genera automaticamente le note di rilascio da GitHub |
| `skip_if_exists` | Boolean | No | `false` | Se `true`, non va in errore se la release esiste già (HTTP 422) |
| `assets` | List\<Map\> | No | `[]` | Lista di file da allegare alla release (vedi sotto) |

**Struttura di ogni elemento in `assets`:**

| Chiave | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|
| `file` | String | Sì | — | Percorso locale del file da caricare |
| `name` | String | No | nome del file | Nome con cui il file viene pubblicato su GitHub |
| `contentType` | String | No | `application/octet-stream` | MIME type del file |

**Valore di ritorno:**

```groovy
[
  owner  : 'org',
  repo   : 'repo-name',
  tag    : 'v1.0.0',
  release: 123456789,   // ID numerico della release su GitHub (null se la release esisteva già)
  url    : 'https://github.com/org/repo-name/releases/tag/v1.0.0'
]
```

**Errori:**

| Condizione | Messaggio |
|---|---|
| Credenziali mancanti | `[github] 'credentialsId' not provided and GITHUB_CREDS_ID not set` |
| Tag mancante | `[github] tag is required` |
| URL mancante | `[github] url not provided and GIT_URL not set` |
| URL non riconosciuto | `[github] URL non riconosciuto: <url>` |
| URL non valido | `[github] URL non valido (owner/repo mancante): <url>` |
| `asset.file` mancante | `[github] asset.file is required` |
| File asset non trovato | `[github] Asset file not found: <path>` |
| Creazione release fallita | `[github] release failure` |
| Upload asset fallito | `[github] upload asset failure` |

**Esempio:**

```groovy
def result = github.release(
  credentialsId       : 'my-github-creds',
  url                 : 'https://github.com/org/repo.git',
  tag                 : 'v1.2.0',
  name                : 'Release 1.2.0',
  body                : 'Bugfix release',
  skip_if_exists      : true,
  assets              : [
    [file: 'build/app.jar', name: 'app-1.2.0.jar', contentType: 'application/java-archive']
  ]
)
echo "Release URL: ${result.url}"
```

---

### `github.assetFind`

Cerca un asset in una release GitHub tramite espressione regolare sul nome del file.

**Parametri:**

| Parametro | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|
| `credentialsId` | String | No | `env.GITHUB_CREDS_ID` | ID delle credenziali Jenkins. Se assente, la chiamata è anonima (solo repository pubblici) |
| `credentialId` | String | No | — | Alias di `credentialsId` (compatibilità) |
| `owner` | String | Sì | — | Owner del repository (utente o organizzazione) |
| `repo` | String | Sì | — | Nome del repository |
| `tag` | String | No | `latest` | Tag della release. Se vuoto o `"latest"`, cerca nell'ultima release |
| `asset_regex` | String | No | `.*` | Espressione regolare sul nome del file (match completo) |

**Valore di ritorno:**

```groovy
// Asset trovato
[
  tag : 'v1.2.0',
  name: 'app-1.2.0.jar',
  url : 'https://api.github.com/repos/org/repo/releases/assets/987654321'
]

// Asset non trovato
null
```

**Errori:**

| Condizione | Messaggio |
|---|---|
| `owner` mancante | `[github] owner is required` |
| `repo` mancante | `[github] repo is required` |
| Risposta HTTP non 200 | `[github] find asset failure` |

**Esempio:**

```groovy
def asset = github.assetFind(
  credentialsId: 'my-github-creds',
  owner        : 'org',
  repo         : 'repo-name',
  tag          : 'v1.2.0',
  asset_regex  : '.*\\.jar'
)
if (asset) {
  echo "Trovato: ${asset.name} - ${asset.url}"
}
```

---

## Modulo `telegram`

Integrazione con le API di Telegram Bot. Permette di inviare messaggi e notifiche di stato della pipeline.

### `telegram.sendMessage` / `telegram()`

Invia un messaggio a una chat Telegram. Il modulo è richiamabile anche direttamente come `telegram(args)` grazie al metodo `call()`.

**Parametri:**

| Parametro | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|
| `message` | String | Sì | — | Testo del messaggio da inviare |
| `chatId` | String | Sì* | `env.TELEGRAM_CHAT_ID` | ID della chat o del canale Telegram |
| `credentialId` | String | Sì* | `env.TELEGRAM_BOT_TOKEN_ID` | ID della credenziale Jenkins di tipo "Secret text" contenente il token del bot |
| `parseMode` | String | No | `env.TELEGRAM_PARSE_MODE` | Modalità di formattazione del testo (`Markdown`, `MarkdownV2`, `HTML`) |
| `threadId` | String | No | `env.TELEGRAM_THREAD_ID` | ID del thread in un supergroup con topic abilitati |

\* Obbligatorio ma può essere fornito tramite variabile d'ambiente.

**Comportamento:**

- Usa `curl` con `--data-urlencode` per gestire correttamente caratteri speciali nel messaggio.
- `threadId` e `parseMode` sono inclusi nella richiesta solo se valorizzati.
- Non restituisce alcun valore.

**Errori:**

| Condizione | Messaggio |
|---|---|
| `message` mancante | `[telegram] Parameter 'message' is required` |
| `chatId` mancante | `[telegram] Parameter 'chatId' not provided and TELEGRAM_CHAT_ID not set` |
| `credentialId` mancante | `[telegram] Parameter 'credentialId' not provided and TELEGRAM_BOT_TOKEN_ID not set` |
| Errore HTTP dall'API Telegram | La shell esce con errore (flag `-f` di curl) |

**Esempio:**

```groovy
telegram.sendMessage(
  credentialId: 'telegram-bot-token',
  chatId      : '-1001234567890',
  message     : 'Deploy completato con successo!',
  parseMode   : 'Markdown'
)

// Forma abbreviata equivalente
telegram(
  credentialId: 'telegram-bot-token',
  chatId      : '-1001234567890',
  message     : 'Deploy completato con successo!'
)
```

---

### `telegram.notifyBuildSuccess`

Invia una notifica di build completata con successo.

**Messaggio inviato:**

```
✅ Build SUCCESS
Job: <JOB_NAME>
Build: #<BUILD_NUMBER>
[Description: <currentBuild.description>]   ← inclusa solo se valorizzata
URL: <BUILD_URL>
```

**Parametri:** stessi di `sendMessage`, eccetto `message` (generato automaticamente).

---

### `telegram.notifyBuildFailure`

Invia una notifica di build fallita.

**Messaggio inviato:**

```
❌ Build FAILED
Job: <JOB_NAME>
Build: #<BUILD_NUMBER>
[Description: <currentBuild.description>]
URL: <BUILD_URL>
```

**Parametri:** stessi di `sendMessage`, eccetto `message`.

---

### `telegram.notifyBuildFixed`

Invia una notifica di build tornata al verde dopo un fallimento.

**Messaggio inviato:**

```
✅ Build FIXED
Job: <JOB_NAME>
Build: #<BUILD_NUMBER>
[Description: <currentBuild.description>]
URL: <BUILD_URL>
```

**Parametri:** stessi di `sendMessage`, eccetto `message`.

---

### `telegram.notifyDeploySuccess`

Invia una notifica di deploy completato con successo.

**Messaggio inviato:**

```
🚀 Deploy SUCCESS
Job: <JOB_NAME>
Build: #<BUILD_NUMBER>
[Description: <currentBuild.description>]
URL: <BUILD_URL>
```

**Parametri:** stessi di `sendMessage`, eccetto `message`.

---

### Esempio completo con `post` di Jenkins

```groovy
pipeline {
  environment {
    TELEGRAM_BOT_TOKEN_ID = 'telegram-bot-token'
    TELEGRAM_CHAT_ID      = '-1001234567890'
    GITHUB_CREDS_ID       = 'my-github-creds'
    GIT_URL               = 'https://github.com/org/repo.git'
  }
  stages {
    stage('Tag') {
      steps {
        github.fetchTags()
      }
    }
    stage('Release') {
      steps {
        script {
          github.release(tag: '1.0.0', skip_if_exists: true)
        }
      }
    }
  }
  post {
    success {
      telegram.notifyBuildSuccess()
    }
    failure {
      telegram.notifyBuildFailure()
    }
    fixed {
      telegram.notifyBuildFixed()
    }
  }
}
```
