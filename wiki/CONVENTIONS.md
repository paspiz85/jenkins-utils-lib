# Convenzioni di codice

Questo documento descrive le convenzioni da seguire per scrivere e mantenere il codice della libreria.

---

## Struttura del progetto

La libreria segue le convenzioni standard di una **Jenkins Shared Library**:

```
jenkins-utils-lib/
├── vars/           # Moduli richiamabili nelle pipeline
│   ├── github.groovy
│   └── telegram.groovy
└── wiki/           # Documentazione interna
```

Ogni file in `vars/` corrisponde a un modulo richiamabile direttamente nelle pipeline Jenkins con la sintassi `nomeModulo.nomeMetodo()`.

---

## Linguaggio

- **Groovy** per tutti i file sorgente.
- Nessun sistema di build (no Gradle, no Maven): la libreria viene caricata direttamente da Jenkins.

---

## Naming

| Elemento | Stile | Esempio |
|---|---|---|
| File modulo | camelCase | `github.groovy` |
| Funzioni pubbliche | camelCase | `fetchTags`, `sendMessage` |
| Funzioni private | camelCase con prefisso `_` | `_parseRepoURL` |
| Variabili locali | camelCase | `credentialsId`, `responseStatus` |
| Chiavi di mappe | camelCase | `[chatId: '...', parseMode: '...']` |
| Variabili d'ambiente Jenkins | UPPER_SNAKE_CASE | `GITHUB_CREDS_ID`, `TELEGRAM_BOT_TOKEN_ID` |

---

## Formattazione

- **Indentazione:** 2 spazi (no tab).
- **Lunghezza riga:** preferibilmente sotto i 100 caratteri.
- **Stile parentesi graffe:** stessa riga dell'istruzione di apertura (stile Java/Groovy).
- **Argomenti lunghi:** spezzati su più righe allineati per leggibilità.

```groovy
// Corretto
def fetchTags(Map args = [:]) {
  withCredentials([
    usernamePassword(credentialsId: credentialsId, usernameVariable: 'USER', passwordVariable: 'TOKEN')
  ]) {
    // ...
  }
}

// Sbagliato
def fetchTags(Map args = [:])
{
    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'USER', passwordVariable: 'TOKEN')]) {
        // ...
    }
}
```

---

## Parametri delle funzioni

Le funzioni pubbliche accettano un unico parametro di tipo `Map` con valore di default `[:]`:

```groovy
def nomeMetodo(Map args = [:]) {
  // ...
}
```

I parametri obbligatori vengono risolti con priorità scalare: valore esplicito → variabile d'ambiente → chiave alternativa:

```groovy
def credentialsId = args.credentialsId ?: args.credentialId ?: env.GITHUB_CREDS_ID
```

---

## Validazione dei parametri

I parametri obbligatori vanno validati esplicitamente all'inizio della funzione. Il messaggio di errore deve includere il prefisso `[nome-modulo]`:

```groovy
def release(Map args = [:]) {
  def credentialsId = args.credentialsId ?: env.GITHUB_CREDS_ID
  if (!credentialsId) {
    error "[github] credentialsId parameter is required"
  }
  // ...
}
```

Il prefisso `[nome-modulo]` è obbligatorio in tutti i messaggi di errore e nei log `echo` per facilitare il debug nelle pipeline.

---

## Valori di ritorno

Le funzioni restituiscono `Map` con chiavi esplicite, oppure `null` in caso di risultato non trovato:

```groovy
return [owner: owner, repo: repo, tag: tag, release: releaseId, url: url]

// oppure
return null
```

Non usare eccezioni per segnalare assenza di risultato: restituire `null` e lasciare al chiamante la gestione.

---

## Gestione delle credenziali

Le credenziali devono sempre passare attraverso il sistema di credenziali Jenkins (`withCredentials` o `withEnv`). È vietato inserire segreti in chiaro nel codice o nei log.

```groovy
withCredentials([
  usernamePassword(credentialsId: credentialsId, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')
]) {
  sh """
    curl -u "\${GITHUB_USER}:\${GITHUB_TOKEN}" ...
  """
}
```

---

## Chiamate HTTP via shell

Usare `curl` con il flag `-w "\\n%{http_code}"` per separare il corpo della risposta dallo status code:

```groovy
def out = sh(script: '''
  set -e
  curl -s -X POST \
    -H "Content-Type: application/json" \
    -d "${BODY}" \
    -w "\n%{http_code}" \
    "https://api.example.com/endpoint"
''', returnStdout: true).trim()

def lines = out.readLines()
def responseStatus = lines.last().trim().toInteger()
def responseBody = (lines.size() > 1) ? lines[0..-2].join('\n') : ""

if (responseStatus != 200) {
  error "[modulo] operazione fallita con status ${responseStatus}"
}
```

I blocchi shell multiriga usano triple virgolette `"""` o `'''`. Preferire `'''` quando non serve interpolazione Groovy per evitare conflitti con `$`.

---

## Gestione del JSON

- **Parsing:** `new groovy.json.JsonSlurper().parseText(responseBody)`
- **Serializzazione:** `groovy.json.JsonOutput.toJson([key: value])`
- **Accesso sicuro ai campi:** usare l'operatore `?.` per evitare NullPointerException

```groovy
def responseJSON = new groovy.json.JsonSlurper().parseText(responseBody)
def url = responseJSON?.html_url ?: '(nessun url ricevuto)'
```

---

## Commenti

I commenti nel codice vanno ridotti al minimo: il codice deve essere autoesplicativo tramite nomi significativi. Aggiungere un commento solo quando il **perché** di una scelta non è evidente dalla lettura del codice.

Non commentare cosa fa il codice, ma perché lo fa in quel modo:

```groovy
// Corretto: spiega il perché
# Print body to stdout and append status code as last line

// Sbagliato: ridondante, il codice lo dice già
// Parse the JSON response
def responseJSON = new groovy.json.JsonSlurper().parseText(responseBody)
```

---

## Logging

Usare `echo` per i messaggi informativi nelle pipeline, sempre con prefisso `[nome-modulo]`:

```groovy
echo "[github] release creata: ${releaseURL}"
echo "[telegram] messaggio inviato"
```

---

## Documentazione dei moduli

Ogni nuovo modulo aggiunto in `vars/` deve essere documentato nella cartella `wiki/` con una descrizione delle funzioni esposte, i parametri accettati e gli esempi d'uso.
