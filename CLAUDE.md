# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Cos'è questo progetto

Una **Jenkins Shared Library** in Groovy che fornisce funzioni riutilizzabili per le pipeline Jenkins. I moduli esposti sono:

- `vars/github.groovy` — integrazione con GitHub API (tag, release, asset)
- `vars/telegram.groovy` — invio messaggi e notifiche via Telegram Bot API

Non esiste un sistema di build locale. La libreria viene caricata direttamente da Jenkins; non ci sono comandi per compilare, testare o avviare il progetto in locale.

## Architettura

Ogni file in `vars/` diventa un modulo richiamabile nelle pipeline con `nomeFile.nomeMetodo()`. Il metodo `call()` in `telegram.groovy` permette anche la sintassi `telegram(args)`.

Le funzioni private (non esposte alle pipeline) sono prefissate con `_` (es. `_parseRepoURL` in `github.groovy`).

Le chiamate HTTP alle API esterne usano `curl` via passo `sh`, non librerie Groovy native — vedere ADR-004 in `wiki/DECISIONS.md`.

## Convenzioni chiave

- **Indentazione:** 2 spazi.
- **Parametri:** tutte le funzioni pubbliche usano `Map args = [:]` — mai firme con parametri espliciti.
- **Risoluzione parametri:** `args.param ?: env.ENV_VAR` — sempre con fallback sulla variabile d'ambiente.
- **Validazione:** parametri obbligatori validati esplicitamente all'inizio della funzione con `error "[modulo] messaggio"`.
- **Credenziali:** sempre tramite `withCredentials`, mai in chiaro.

Per le convenzioni complete vedere `wiki/CONVENTIONS.md`. Per le specifiche di ogni funzione (parametri, valori di ritorno, errori) vedere `wiki/SPEC.md`.

## Documentazione wiki

| File | Contenuto |
|---|---|
| `wiki/CONVENTIONS.md` | Regole di stile e pattern da seguire nel codice |
| `wiki/SPEC.md` | Specifica completa di ogni modulo e funzione |
| `wiki/DECISIONS.md` | Decisioni architetturali (ADR) con motivazioni |
| `wiki/SECURITY.md` | Linee guida di sicurezza e permessi minimi |
| `wiki/TODO.md` | Feature, miglioramenti e debito tecnico pendente |

## Versionamento

Il progetto usa **semantic versioning tramite Git tag** (`v1.0.0`, `v1.1.0`, ecc.). Le pipeline Jenkins referenziano la versione nella dichiarazione `@Library`:

```groovy
library(identifier: 'my-jenkins-utils-lib@v1.0.0', changelog: false)
```

Le modifiche vanno documentate in `CHANGELOG.md` sotto la sezione `[Unreleased]` usando le categorie `Added`, `Changed`, `Fixed`, `Removed`.

## Note generali

- La documentazione del progetto è in **italiano**.
- Non aggiornare la documentazione è considerato un bug, alla pari di un test rotto.
