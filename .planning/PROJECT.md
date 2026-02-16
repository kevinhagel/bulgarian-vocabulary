# Bulgarian Vocabulary Tutor

## What This Is

A Bulgarian vocabulary learning application for students being tutored by Elena. Users enter Bulgarian words and phrases (lemmas) either individually or by pasting text, and the system uses LLMs to detect lemmas, generate comprehensive inflections, and provide study tools (flashcards, word lists, audio playback). The focus is on building and organizing a personal Bulgarian vocabulary database with linguistic precision.

## Core Value

Each vocabulary entry must accurately represent the lemma (dictionary headword) with all its inflections, English translation, and metadata, enabling effective study through audio playback and interactive learning tools.

## Requirements

### Validated

(None yet — ship to validate)

### Active

**Phase 1: Vocabulary Domain Model & CRUD**

- [ ] Enter single Bulgarian word (any inflection) and LLM detects/generates lemma + all inflections
- [ ] Enter multi-word lemma explicitly (e.g., "казвам се", "искам да")
- [ ] Paste Bulgarian text/dialog and detect single-word lemmas (extract words only, not phrases)
- [ ] LLM auto-generates: lemma text, inflections, part of speech, category, difficulty level
- [ ] Store English translation (essential)
- [ ] Store user-editable notes field
- [ ] Review phase for LLM-generated metadata
- [ ] Edit page to modify/correct vocabulary entries
- [ ] Delete vocabulary entries (for corrections or re-entry)
- [ ] Generate audio (TTS via Edge TTS) for lemma and all inflections
- [ ] Seed reference vocabulary via Flyway (interrogatives, pronouns, prepositions, conjunctions, numerals)
- [ ] Basic vocabulary browsing and search/filter functionality
- [ ] Flashcard study mode with audio playback
- [ ] Create and manage word lists (by topic, difficulty, etc.)

### Out of Scope

- **Speech-to-Text (STT)**: Learned from bulgarian-tutor-web that no STT technologies worked reliably for Bulgarian — skip entirely
- **Fill-in-the-blank exercises**: Tried in bulgarian-tutor-web, didn't work well — defer to future phase
- **Multi-word phrase detection from text**: Too ambiguous — user manually enters multi-word lemmas (казвам се, искам да, etc.)
- **Mobile app**: Web-first, defer mobile to v2+
- **Real-time collaboration**: Single-user focus for v1

## Context

**Learning Environment:**
- Being tutored by Elena (Bulgarian tutor) along with Huw Jones
- Elena provides: vocabulary words (verbs, adjectives), dialog practice, printouts
- Need to organize and study vocabulary from lessons

**Bulgarian Linguistic Characteristics:**
- Lemma for verbs: first-person singular (e.g., пиша), not infinitive (Bulgarian has no infinitive)
- Bulgarian verbs inflect for: person, number, voice, aspect, mood, tense, sometimes gender
- Bulgarian nouns inflect for: number, gender, definiteness (suffixed article)
- Some words are invariant: кога (when), къде (where), защо (why)
- Multi-word lemmas exist: казвам се (to be called), искам да (to want to)

**Lessons from bulgarian-tutor-web (archived project):**
- ✓ Text-to-Speech (Edge TTS) worked very well
- ✗ Speech-to-Text failed across all technologies tried
- ✗ Fill-in-the-blank exercises weren't effective
- ✓ LLMs were smart at: lemma detection, part of speech, category, difficulty generation

**Development Setup:**
- Development on MacBook M2
- Docker Postgres on MacBook M2
- Ollama (LLM) on Mac Studio (networked)
- Edge TTS on both Mac Studio and MacBook M2
- Eventually: production deployment on Mac Studio

## Constraints

- **Tech Stack**: Spring Boot 4.2.x, Java 25, Spring Data JPA, PostgreSQL, Flyway
- **Frontend**: React with Tailwind CSS (modern, fast dev experience)
- **LLM**: Ollama on Mac Studio (network accessible from MacBook M2)
- **TTS**: Edge TTS only (no STT)
- **Database**: PostgreSQL via Docker
- **Development Environment**: MacBook M2 (local development), Mac Studio (LLM + eventual production)
- **Production Deployment**: All components deployed to Mac Studio; PostgreSQL stays within LAN (not exposed to internet); web frontend exposed to internet
- **Migration Tool**: Flyway for schema versioning and seed data

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Lemma-based domain model | Aligns with linguistic standards; lemma = canonical dictionary form (1st person singular for Bulgarian verbs) | — Pending |
| Single Lemma entity with source field | User-entered lemmas + system-seeded reference vocabulary (interrogatives, pronouns, etc.) share same entity type with source distinction | — Pending |
| JPA (Spring Data JPA) for data access | Rich domain model with relationships (Lemma → Inflections, Categories); JPA reduces boilerplate while maintaining flexibility | — Pending |
| Text detection extracts single words only | Multi-word lemma detection is ambiguous; user maintains control by manually entering phrasal lemmas | — Pending |
| Flyway seed data for reference vocabulary | Pre-populate interrogatives, pronouns, prepositions via versioned migrations; no manual entry needed | — Pending |
| No STT support | Learned from bulgarian-tutor-web that STT doesn't work reliably for Bulgarian | ✓ Good |
| Edge TTS for audio | Proven to work well from bulgarian-tutor-web | ✓ Good |

---
*Last updated: 2025-02-15 after initialization*
