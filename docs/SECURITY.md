# Sicurezza

Questo documento descrive le linee guida di sicurezza della libreria, i rischi noti e le pratiche da seguire per un uso sicuro.

---

## Versioni supportate

Solo l'ultima versione rilasciata riceve aggiornamenti di sicurezza.

| Versione | Supportata |
|---|---|
| ultima | ✅ |
| precedenti | ❌ |

---

## Gestione delle credenziali

La libreria non gestisce mai segreti in chiaro. Tutte le credenziali transitano attraverso il sistema di credenziali Jenkins:

- I token GitHub vengono letti tramite `withCredentials([usernamePassword(...)])`.
- Il token del bot Telegram viene letto tramite `withCredentials([string(...)])`.
- I segreti vengono iniettati come variabili d'ambiente nel blocco `sh` e mascherati automaticamente da Jenkins nei log.

**Non fare mai:**

```groovy
// SBAGLIATO — il token appare nei log e nella cronologia Git
github.release(token: 'ghp_abc123...', ...)
```

**Fare sempre:**

```groovy
// CORRETTO — il token è gestito da Jenkins Credentials
github.release(credentialsId: 'my-github-creds', ...)
```

---

## Esposizione nei log

Jenkins maschera automaticamente i valori delle credenziali gestite con `withCredentials`. Tuttavia:

- Non passare mai segreti come parte di stringhe costruite manualmente (es. interpolazione in un URL loggato).
- Evitare di stampare con `echo` variabili che potrebbero contenere dati sensibili derivati da una credenziale.

---

## Chiamate HTTP

Le chiamate alle API di GitHub e Telegram avvengono tramite `curl` con HTTPS. Punti di attenzione:

- Le chiamate a GitHub usano `https://api.github.com` e `https://uploads.github.com`.
- Le chiamate a Telegram usano `https://api.telegram.org`.
- Il flag `-sS` di curl sopprime il progress bar ma mostra gli errori; il flag `-f` (usato in Telegram) fa fallire il comando in caso di errore HTTP.
- Non viene eseguita verifica esplicita del certificato TLS oltre a quella predefinita di curl; non disabilitare mai `-k` / `--insecure` negli script.

---

## Permessi GitHub

Il token GitHub usato deve avere i permessi minimi necessari:

| Operazione | Permesso richiesto |
|---|---|
| `fetchTags` | `contents: read` |
| `release` | `contents: write` |
| `assetFind` | `contents: read` (o nessuno per repository pubblici) |

Usare un token con scope ridotto al minimo necessario per ogni pipeline.

---

## Permessi Telegram

Il bot Telegram deve essere aggiunto alla chat o al canale di destinazione con i permessi di invio messaggi. Non concedere al bot permessi di amministrazione se non strettamente necessario.

---

## Raccomandazioni generali

- Ruotare periodicamente i token GitHub e Telegram e aggiornare le credenziali Jenkins corrispondenti.
- Non condividere la stessa credenziale Jenkins tra ambienti diversi (sviluppo, staging, produzione).
- Limitare l'accesso alle credenziali Jenkins solo ai folder e alle pipeline che ne hanno effettivo bisogno.
