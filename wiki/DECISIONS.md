# Decisioni architetturali

Questo documento traccia le scelte di design significative prese nel progetto, con la motivazione dietro a ciascuna. L'obiettivo è che chiunque lavori sulla libreria — ora o in futuro — capisca il *perché* prima di cambiare qualcosa.

---

## ADR-001 — Jenkins Shared Library invece di script inline nei Jenkinsfile

**Stato:** adottato

**Contesto:**  
La logica di integrazione con GitHub e Telegram sarebbe potuta essere scritta direttamente nei `Jenkinsfile` di ogni progetto.

**Decisione:**  
Usare una Jenkins Shared Library centralizzata, caricata da tutti i progetti tramite `@Library`.

**Motivazioni:**
- **Riutilizzo:** la stessa logica serve in più pipeline e repository; mantenerla in un posto solo evita la duplicazione.
- **Separazione delle responsabilità:** chi mantiene la libreria (questo repo) è separato da chi scrive le pipeline dei singoli progetti.

**Conseguenze:**
- Ogni progetto che usa la libreria dipende da questo repository; una modifica incompatibile qui può rompere più pipeline.
- Le versioni della libreria vanno gestite con attenzione (tag, branch stabili).

---

## ADR-002 — Parametri delle funzioni tramite `Map args = [:]`

**Stato:** adottato

**Contesto:**  
Le funzioni pubbliche avrebbero potuto usare parametri espliciti e tipizzati, ad esempio:
```groovy
def release(String tag, String credentialsId, Boolean draft = false) { ... }
```

**Decisione:**  
Tutte le funzioni pubbliche accettano un'unica `Map args = [:]`.

**Motivazione:**  
Flessibilità: aggiungere un nuovo parametro opzionale non rompe i chiamanti esistenti. Con la firma esplicita, ogni nuova opzione richiederebbe un overload o un parametro aggiuntivo obbligatorio per tutti i chiamanti.

**Conseguenze:**
- Non c'è controllo del tipo a compile time; gli errori su parametri mancanti o errati emergono solo a runtime.
- La documentazione dei parametri deve essere tenuta aggiornata in `wiki/SPEC.md`, dato che la firma del metodo non è autoesplicativa.

---

## ADR-003 — Risoluzione a cascata delle credenziali e dei parametri

**Stato:** adottato

**Contesto:**  
Le credenziali e alcuni parametri frequenti (es. `chatId`, URL del repository) potrebbero essere richiesti esplicitamente ad ogni chiamata.

**Decisione:**  
I parametri vengono risolti con la seguente priorità:
1. Valore passato esplicitamente nell'argomento
2. Variabile d'ambiente Jenkins (`env.GITHUB_CREDS_ID`, `env.TELEGRAM_CHAT_ID`, ecc.)

```groovy
def credentialsId = args.credentialsId ?: args.credentialId ?: env.GITHUB_CREDS_ID
```

**Motivazione:**  
Supportare due modalità d'uso nella stessa libreria:
- **Parametri espliciti:** utile quando si gestiscono più account o repository diversi nella stessa pipeline.
- **Variabili d'ambiente:** utile quando le env sono configurate a livello di folder o di pipeline e si vuole semplificare il Jenkinsfile chiamante, evitando di ripetere le stesse credenziali su ogni chiamata.

**Conseguenze:**
- Le variabili d'ambiente attese devono essere documentate (vedi `wiki/SPEC.md`).
- Se una variabile d'ambiente è configurata per sbaglio su un agent, può sovrascrivere silenziosamente un parametro non passato; il comportamento è intenzionale ma va tenuto presente.

---

## ADR-004 — Chiamate HTTP tramite `curl` in shell invece di librerie Groovy native

**Stato:** adottato

**Contesto:**  
Le chiamate alle API REST di GitHub e Telegram avrebbero potuto essere implementate con `HttpURLConnection` o librerie Groovy come HttpBuilder (tramite `@Grab`).

**Decisione:**  
Tutte le chiamate HTTP usano `curl` eseguito tramite il passo `sh`.

**Motivazioni:**
- **Nessuna dipendenza esterna:** `curl` è disponibile su qualsiasi agent Linux/Mac senza configurazioni aggiuntive. Le librerie Groovy richiedono `@Grab` o di aggiungere jar al classpath del master Jenkins, il che introduce complessità di gestione.
- **Leggibilità nei log:** il comando `curl` completo appare esplicitamente negli stage logs di Jenkins, rendendo immediato il debug in caso di errore.

**Conseguenze:**
- La libreria richiede `curl` sull'agent di esecuzione (assunzione ragionevole su ambienti Linux standard).
- Non funziona su agent Windows senza un'installazione esplicita di `curl` o WSL.

---

## ADR-005 — Assenza di test automatizzati

**Stato:** aperto

**Contesto:**  
Framework come [JenkinsPipelineUnit](https://github.com/jenkinsci/JenkinsPipelineUnit) permettono di scrivere unit test per le Shared Library in Groovy senza bisogno di un'istanza Jenkins reale.

**Decisione:**  
Al momento non sono stati introdotti test automatizzati. Il testing avviene eseguendo le pipeline reali sui progetti che usano la libreria.

**Conseguenze:**
- Le regressioni possono emergere solo durante l'esecuzione di una pipeline reale.
- Prima di introdurre test, vale la pena valutare se la complessità aggiunta (setup Gradle/Maven, mock dei passi Jenkins) è giustificata rispetto alla dimensione attuale della libreria.
