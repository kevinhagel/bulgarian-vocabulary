# Bulgarian Vocabulary Tutor

A personal vocabulary learning app for Bulgarian, built for daily study with spaced repetition, dictionary-backed word creation, and text-to-speech.

> **Note:** This project is not yet packaged for third-party deployment. It runs on a single Mac Studio with local infrastructure and expects specific environment configuration (Vault, Ollama, nginx, etc.).

## Stack

- **Backend:** Spring Boot 3.5.11, Java 25, Spring AI, JPA/Hibernate
- **Frontend:** React 19, Vite 6, TypeScript, TanStack Query, Zustand, Tailwind CSS v4
- **Database:** PostgreSQL 16 with PGroonga (Bulgarian Cyrillic full-text search)
- **Cache:** Valkey 7.2 (Redis-compatible)
- **LLM:** Ollama with BgGPT (`todorov/bggpt:9B-IT-v1.0.Q4_K_M`) for inflection generation and sentence examples
- **TTS:** edge-tts for Bulgarian audio
- **Secrets:** HashiCorp Vault
- **Infrastructure:** Docker Compose (PostgreSQL, Valkey, pgAdmin, backend)

## Dictionary

Vocabulary entries are backed by the **Kaikki Bulgarian dictionary**, extracted from Wiktionary by the [Kaikki project](https://kaikki.org/). The JSONL file (`kaikki-dictionary-Bulgarian.jsonl`) contains ~18,900 Bulgarian words with inflection forms, translations, IPA, and grammatical tags.

The dictionary data is imported into `dictionary_words` and `dictionary_forms` tables via `POST /api/admin/dictionary/import`. When adding vocabulary through the frontend, the app searches the dictionary first and uses authoritative inflection data when available, falling back to BgGPT LLM generation only when no dictionary match is found.

The JSONL file is not checked into the repo (see `.gitignore`, `data/` directory). It can be downloaded from [kaikki.org/dictionary/Bulgarian](https://kaikki.org/dictionary/Bulgarian/).
