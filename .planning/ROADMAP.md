# Roadmap: Bulgarian Vocabulary Tutor

## Overview

This roadmap delivers a Bulgarian vocabulary learning application from foundation to production. The journey begins with establishing the correct data model for Bulgarian's complex morphology, integrates LLM and TTS infrastructure, builds core vocabulary management with an intuitive React frontend, and culminates in a complete spaced repetition study system with word lists and progress tracking. Each phase delivers a coherent, verifiable capability that builds toward effective vocabulary learning through audio playback and interactive study tools.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundation & Data Model** - PostgreSQL schema, JPA entities, development environment
- [x] **Phase 2: LLM Integration** - Ollama connectivity, Spring AI setup, caching, async patterns
- [x] **Phase 3: TTS Audio Generation** - Edge TTS integration, file caching, background generation
- [x] **Phase 4: Core Vocabulary Management** - CRUD operations, reference data seeding, LLM orchestration
- [ ] **Phase 5: Frontend Foundation & Vocabulary UI** - React setup, vocabulary management interface
- [ ] **Phase 6: Flashcards & Basic Study** - Study mode, basic spaced repetition, progress tracking
- [ ] **Phase 7: Word Lists & Organization** - Create/manage lists, list-specific study sessions
- [ ] **Phase 8: Advanced SRS & Polish** - Forgiveness logic, review caps, production readiness

## Phase Details

### Phase 1: Foundation & Data Model
**Goal**: Establish correct database schema for Bulgarian morphology with proper lemma/inflection separation and development environment
**Depends on**: Nothing (first phase)
**Requirements**: LLM-01, LLM-02
**Success Criteria** (what must be TRUE):
  1. PostgreSQL database runs via Docker with Flyway migrations applied
  2. Lemma and Inflection JPA entities exist with proper relationships (one-to-many)
  3. Spring Data JPA repositories provide basic CRUD operations
  4. Development environment can connect to Ollama on Mac Studio from MacBook M2
  5. PGroonga extension configured for Bulgarian Cyrillic full-text search
**Plans**: 2 plans

Plans:
- [x] 01-01-PLAN.md -- Spring Boot scaffolding, Docker Compose PostgreSQL/PGroonga, Flyway migrations (schema + seed data)
- [x] 01-02-PLAN.md -- JPA entities (Lemma, Inflection, enums), Spring Data repositories, application verification

### Phase 2: LLM Integration
**Goal**: Connect to Ollama via Spring AI with async processing, caching, and circuit breaker patterns
**Depends on**: Phase 1
**Requirements**: LLM-03, LLM-04, LLM-05, LLM-06, LLM-07
**Success Criteria** (what must be TRUE):
  1. System successfully calls Ollama API on Mac Studio from Spring Boot application
  2. LLM can detect lemma from any Bulgarian word form (inflection)
  3. LLM can generate all inflections for a given lemma (person, number, tense, aspect, mood, gender)
  4. LLM can auto-generate part of speech, category, and difficulty level
  5. LLM responses are cached to avoid redundant API calls (observable via logs)
  6. LLM calls execute asynchronously without blocking request threads
  7. Circuit breaker activates when Ollama is unavailable (prevents cascading failures)
  8. Generated metadata can be reviewed before saving (validation queue exists)
**Plans**: 2 plans

Plans:
- [ ] 02-01-PLAN.md -- Spring AI Ollama dependencies, async/cache/circuit breaker configuration, structured output DTOs
- [ ] 02-02-PLAN.md -- LLM service classes (lemma detection, inflection generation, metadata generation), output validator, orchestration service

### Phase 3: TTS Audio Generation
**Goal**: Generate Bulgarian pronunciation audio using Edge TTS with file caching and background processing
**Depends on**: Phase 1
**Requirements**: AUDIO-01, AUDIO-02, AUDIO-03, AUDIO-06, AUDIO-07
**Success Criteria** (what must be TRUE):
  1. System can generate audio file for Bulgarian text using Edge TTS
  2. Audio files are stored on disk (not database BLOBs)
  3. Audio files are cached by content hash (no regeneration for same text)
  4. Audio generation happens asynchronously in background (non-blocking)
  5. REST endpoint serves audio files via GET /api/audio/{filename}
**Plans**: 2 plans

Plans:
- [ ] 03-01-PLAN.md -- Audio infrastructure: exception, content hash utility, storage config, async thread pool, application properties
- [ ] 03-02-PLAN.md -- Edge TTS service (ProcessBuilder CLI), async wrapper, REST audio controller with path traversal prevention

### Phase 4: Core Vocabulary Management
**Goal**: CRUD operations for vocabulary entries with LLM orchestration and reference data seeding
**Depends on**: Phase 2, Phase 3
**Requirements**: VOCAB-01, VOCAB-02, VOCAB-03, VOCAB-04, VOCAB-05, VOCAB-06, VOCAB-07, VOCAB-08, VOCAB-09, VOCAB-10, VOCAB-11, VOCAB-12, VOCAB-13, VOCAB-14, REF-01, REF-02, REF-03, REF-04, REF-05
**Success Criteria** (what must be TRUE):
  1. User can enter any Bulgarian word form and system detects canonical lemma via LLM
  2. User can enter multi-word lemma explicitly (e.g., "казвам се", "искам да")
  3. User can enter English translation (required field)
  4. User can enter notes (optional field)
  5. LLM auto-generates all inflections, part of speech, category, and difficulty level
  6. User can review LLM-generated metadata before saving
  7. User can edit vocabulary entry (lemma text, translation, notes, inflections)
  8. User can delete vocabulary entry
  9. User can browse all vocabulary entries
  10. User can search vocabulary by lemma text (Bulgarian full-text search with Cyrillic)
  11. User can filter vocabulary by part of speech, category, difficulty, source
  12. System pre-populates interrogatives, pronouns, prepositions, conjunctions, numerals via Flyway
  13. System distinguishes user-entered lemmas from system-seeded reference vocabulary
**Plans**: 2 plans

Plans:
- [x] 04-01-PLAN.md -- MapStruct dependency, API DTOs with validation groups, global exception handler, repository pagination/filtering enhancements
- [x] 04-02-PLAN.md -- LemmaMapper, VocabularyService with LLM orchestration integration, VocabularyController with REST CRUD endpoints

### Phase 5: Frontend Foundation & Vocabulary UI
**Goal**: React frontend with modern state management and vocabulary management interface
**Depends on**: Phase 4
**Requirements**: AUDIO-04, AUDIO-05
**Success Criteria** (what must be TRUE):
  1. React 19 + Vite 6 + TypeScript application runs and hot-reloads during development
  2. TanStack Query manages server state (vocabulary data from REST API)
  3. Zustand manages UI state (form inputs, modal visibility)
  4. User can view vocabulary list with Cyrillic rendering (large, clear fonts)
  5. User can add new vocabulary entry via form (calls backend REST API)
  6. User can edit existing vocabulary entry
  7. User can delete vocabulary entry with confirmation
  8. User can search/filter vocabulary (calls backend search endpoint)
  9. User can play audio for lemma in vocabulary list
  10. User can view lemma details and play audio for all inflections
  11. UI is mobile-responsive (works on phone, tablet, laptop)
**Plans**: 4 plans

Plans:
- [ ] 05-01-PLAN.md -- React 19 + Vite 6 project scaffolding, backend audio endpoint, TypeScript types, API client, layout shell, AudioPlayer
- [ ] 05-02-PLAN.md -- Vocabulary list page with TanStack Query hooks, Zustand store, search/filter, pagination, audio playback
- [ ] 05-03-PLAN.md -- CRUD forms (create/edit/delete) with React Hook Form + Zod validation, modals, mutation hooks
- [ ] 05-04-PLAN.md -- Vocabulary detail page with inflections table, per-inflection audio, human verification checkpoint

### Phase 6: Flashcards & Basic Study
**Goal**: Flashcard study mode with audio playback, basic spaced repetition, and progress tracking
**Depends on**: Phase 5
**Requirements**: FLASH-01, FLASH-02, FLASH-03, FLASH-04, FLASH-05, FLASH-06, FLASH-07, FLASH-08, SRS-01, SRS-02, SRS-03, SRS-04, PROGRESS-01, PROGRESS-02, PROGRESS-03, PROGRESS-04, PROGRESS-05, PROGRESS-06
**Success Criteria** (what must be TRUE):
  1. User can start flashcard study session
  2. Flashcard shows lemma text and plays audio on click
  3. User can reveal English translation on flashcard
  4. User can view all inflections on flashcard with audio playback
  5. User can mark answer as correct or incorrect
  6. Flashcard session shows progress (X of Y cards reviewed)
  7. User can end session early and save progress
  8. System schedules flashcard reviews based on spaced repetition algorithm
  9. System increases review interval when user answers correctly
  10. System decreases review interval when user answers incorrectly
  11. User sees due date for next review on vocabulary entry
  12. User can view overall progress dashboard (total vocabulary count, study sessions, cards reviewed)
  13. User can view per-lemma study stats (last reviewed, review count, correctness rate)
  14. System tracks total vocabulary count (user-entered only, excludes reference)
  15. System calculates retention rate (correct / total reviews)
**Plans**: TBD

Plans:
- [ ] 06-01: TBD during planning

### Phase 7: Word Lists & Organization
**Goal**: Create and manage word lists with list-specific study sessions
**Depends on**: Phase 6
**Requirements**: LISTS-01, LISTS-02, LISTS-03, LISTS-04, LISTS-05, LISTS-06
**Success Criteria** (what must be TRUE):
  1. User can create named word list (e.g., "Week 3 Verbs", "Food Vocabulary")
  2. User can add lemmas to word list
  3. User can remove lemmas from word list
  4. User can delete word list
  5. User can view all word lists
  6. User can study word list as flashcard session (list-specific study mode)
**Plans**: TBD

Plans:
- [ ] 07-01: TBD during planning

### Phase 8: Advanced SRS & Polish
**Goal**: Forgiving spaced repetition with review caps, validation mechanisms, and production readiness
**Depends on**: Phase 7
**Requirements**: SRS-05, SRS-06
**Success Criteria** (what must be TRUE):
  1. System caps daily reviews to prevent overwhelming backlog (max 100 cards)
  2. System implements forgiveness logic for missed reviews (gradual catch-up, no snowball of shame)
  3. User can report incorrect inflections or translations (feedback mechanism)
  4. All LLM-generated metadata is flagged for human review before treated as verified
  5. System validates LLM outputs for obvious errors (empty inflections, malformed data)
  6. Application has health checks and monitoring endpoints (Spring Boot Actuator)
  7. Application performs acceptably with 5000+ vocabulary entries and 100+ daily reviews
**Plans**: TBD

Plans:
- [ ] 08-01: TBD during planning

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Data Model | 2/2 | ✓ Complete | 2026-02-15 |
| 2. LLM Integration | 2/2 | ✓ Complete | 2026-02-16 |
| 3. TTS Audio Generation | 2/2 | ✓ Complete | 2026-02-16 |
| 4. Core Vocabulary Management | 2/2 | ✓ Complete | 2026-02-16 |
| 5. Frontend Foundation & Vocabulary UI | 0/4 | Not started | - |
| 6. Flashcards & Basic Study | 0/TBD | Not started | - |
| 7. Word Lists & Organization | 0/TBD | Not started | - |
| 8. Advanced SRS & Polish | 0/TBD | Not started | - |

---
*Roadmap created: 2026-02-15*
*Last updated: 2026-02-16 (Phase 4 complete)*
