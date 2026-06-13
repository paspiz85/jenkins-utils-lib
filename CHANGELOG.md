# Changelog

Tutte le modifiche rilevanti del progetto sono documentate in questo file.

Il formato segue [Keep a Changelog](https://keepachangelog.com/it/1.0.0/) e il progetto adotta il [Versionamento Semantico](https://semver.org/lang/it/).

---

## [Unreleased]

### Added
- Documentazione interna nella cartella `wiki/` (`CONVENTIONS.md`, `SPEC.md`, `DECISIONS.md`)

---

## [0.1.0] — 2025

### Added
- Modulo `telegram`: invio messaggi tramite Telegram Bot API (`sendMessage`)
- Modulo `telegram`: notifiche predefinite di pipeline (`notifyBuildSuccess`, `notifyBuildFailure`, `notifyBuildFixed`, `notifyDeploySuccess`)
- Modulo `github`: creazione release su GitHub con upload di asset (`release`)
- Modulo `github`: ricerca asset in una release tramite regex (`assetFind`)
- Modulo `github`: recupero tag dal repository remoto (`fetchTags`)
