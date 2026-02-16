# Project Research Summary

**Project:** Bulgarian Vocabulary Learning Application
**Domain:** Educational Technology - Language Learning (Morphologically Rich Language)
**Researched:** 2026-02-15
**Confidence:** HIGH

## Executive Summary

This is a vocabulary learning application for Bulgarian, a morphologically rich Slavic language with complex inflectional patterns. The project combines proven educational technology (spaced repetition flashcards) with modern AI capabilities (LLM-powered lemma detection and TTS audio generation). The recommended approach uses a full-stack architecture with Spring Boot 4.2.x backend integrating Ollama for local LLM processing, React 19 + Vite frontend with TanStack Query for state management, and PostgreSQL with specialized Bulgarian full-text search support.

The critical success factor is handling Bulgarian's linguistic complexity properly from day one. Unlike English vocabulary apps, this requires proper relational modeling of lemmas and their inflected forms, first-class Cyrillic support, and awareness that Bulgarian lacks infinitives (verbs are cited in 1st person singular). The architecture must account for expensive LLM operations through aggressive caching, async processing, and circuit breakers to prevent performance degradation.

Key risks include LLM hallucinations teaching incorrect Bulgarian grammar, TTS quality gaps affecting pronunciation learning, and spaced repetition systems breaking when users miss study days. These are mitigated through: validation pipelines for all LLM-generated content, native speaker TTS verification with smart caching strategies, and forgiving catch-up logic in the SRS algorithm. The roadmap should prioritize establishing the correct data model and core learning loop before adding advanced features like text parsing or gamification.

## Key Findings

### Recommended Stack

The research validates a modern full-stack approach leveraging Spring Boot 4.x ecosystem for AI integration and React 19 for frontend UX. All core technologies are at their latest stable versions with verified compatibility.

**Core technologies:**
- **Spring Boot 4.2.x**: Required for Spring AI 2.x compatibility, provides modern Java 25 runtime with virtual threads for efficient async LLM processing
- **Spring AI 2.0**: Official Spring framework for portable LLM integration across providers with auto-configuration for Ollama
- **PostgreSQL 16+ with PGroonga 4.0**: Robust relational storage with JSONB for morphological metadata and critical Bulgarian Cyrillic full-text search support
- **React 19 + TypeScript + Vite 6**: Modern frontend stack with near-instant HMR, excellent TypeScript integration, and 40% smaller bundles than webpack
- **TanStack Query 5.x + Zustand 4.x**: Industry-standard 2025 state management separating server state (React Query) from UI state (Zustand)
- **Edge TTS**: Free neural TTS for Bulgarian pronunciation without API keys or costs
- **Ollama**: Local LLM serving on separate hardware for lemma detection, inflection generation, and example sentences

**Critical version dependencies:** Spring AI 2.x requires Spring Boot 4.x (cannot mix), Vite 6.x requires Node.js 22+, PGroonga 4.0+ essential for Bulgarian Cyrillic search.

### Expected Features

**Must have (table stakes):**
- **Flashcard study mode** — Core vocabulary learning mechanism, users expect recognition and recall modes
- **Spaced repetition system** — Scientific retention technique, industry standard since Anki
- **Audio playback (TTS)** — Critical for Bulgarian pronunciation, listening is essential language skill
- **Word list management** — Basic organizational need, create/edit/delete collections
- **Example sentences** — Context improves retention vs. rote memorization
- **Progress tracking** — Users need visible progress to maintain motivation (43% quit without it)
- **Cyrillic support** — First-class rendering and input, many apps fail at non-Roman text

**Should have (competitive differentiators):**
- **LLM-powered lemma detection** — Auto-identify word roots and generate Bulgarian inflections
- **Text parsing for vocabulary extraction** — Extract vocabulary from tutor materials (Elena's dialogues)
- **Tutor integration workflow** — Purpose-built for students with teachers, not self-study
- **Grammar-aware flashcards** — Display inflections, aspect pairs, grammatical context specific to Bulgarian
- **Context-aware categorization** — Auto-organize by lesson/theme from source material
- **Offline-first architecture** — Study without connectivity, sync when available

**Defer (v2+):**
- **Gamification** — Validate learning loop first before adding streaks/XP/badges to avoid optimizing wrong metrics
- **Multi-device cloud sync** — Start local-first, add backend complexity after validation
- **Tutor dashboard** — Build for one student first, scale when there are multiple
- **Native mobile apps** — PWA validates concept before platform-specific investment

### Architecture Approach

The architecture follows a layered monolith pattern with clear separation between presentation (React), REST API (Spring Boot controllers), service logic (business rules and LLM orchestration), and persistence (Spring Data JPA + PostgreSQL). External integrations (Ollama LLM, Edge TTS) are isolated behind service interfaces with async processing, circuit breakers, and aggressive caching to handle their slow/expensive nature.

**Major components:**

1. **Vocabulary Management Service** — Orchestrates lemma CRUD, coordinates LLM calls for metadata generation, manages inflection relationships via JPA repositories
2. **LLM Integration Service** — Wraps Ollama API with Spring AI, implements caching by prompt hash, async execution with @Async, circuit breaker pattern for resilience, prompt engineering for Bulgarian-specific tasks
3. **TTS Audio Service** — Integrates Edge TTS for Bulgarian pronunciation, file-based caching strategy with SHA-256 naming, background audio generation queue, serves via CDN/static hosting
4. **Study Session Service** — Implements spaced repetition algorithm with forgiveness logic, tracks user progress and retention, generates flashcard sequences with morphological awareness
5. **Text Analysis Service** — Extracts vocabulary from Bulgarian text via regex, batch LLM processing for parallel lemma detection, deduplication against existing vocabulary, review UI for user confirmation

**Key architectural patterns:**
- Repository pattern with Spring Data JPA for data access abstraction
- Async service integration with circuit breaker for LLM calls (prevents thread exhaustion)
- Cache-aside pattern for LLM responses and TTS audio (30s → <1s improvement)
- React Query for server state management with optimistic updates
- Zustand for lightweight client UI state (flashcard index, show answer)

### Critical Pitfalls

1. **Morphological data stored as flat strings** — Bulgarian's rich inflection system (gender, number, definiteness, aspect, tense) requires proper relational modeling from day one. Flat strings prevent querying patterns like "all perfective verbs" and force duplication. Use separate Lemmas and Inflections tables with JSONB for flexible metadata. Recovery from this mistake is HIGH cost (full data migration). Address in Foundation phase.

2. **LLM hallucinations treated as ground truth** — Ollama models generate plausible but incorrect Bulgarian grammar (wrong aspects, genders, inflections). Never trust LLM output without validation. Implement verification queue for all generated content, flag as "unverified" until human review, use LLMs for detection/generation but not canonical grammar rules. Address in Foundation + MVP phases with native speaker validation.

3. **Spaced repetition breaks with inconsistency** — Life happens, users miss days, reviews pile up (500+ cards due), "snowball of shame" causes abandonment. Implement forgiving catch-up logic from day one: cap daily reviews (max 100), gradually reintroduce overdue cards, reset intervals for 30+ day overdue. Design for real human behavior, not perfect adherence. Address in MVP phase.

4. **TTS quality gaps ruin pronunciation learning** — Bulgarian TTS can have incorrect stress (critical), poor consonant clusters, wrong ъ (schwa) pronunciation. Validate with native speakers before launch, pre-generate and cache common vocabulary, have fallback provider, monitor cache hit rates (target >95%). Address in Foundation phase.

5. **Learning words in isolation (no context)** — Users memorize "чета = read" but can't use it (wrong prepositions, missing collocations). Make example sentences mandatory, show word in 2-3 contexts, LLM-generated sentences need human review for naturalness. Address in Foundation (data model requires sentences) and MVP (sentence generation).

6. **Ignoring Bulgarian's lack of infinitive** — Bulgarian cites verbs in 1st person singular ("чета" not "to read"). English-centric designs confuse learners. Use 1st person as lemma, UI shows "чета (I read)", conjugation tables start from 1st person. Address in Foundation (data model) and MVP (UI conventions).

7. **Premature gamification obscures learning effectiveness** — Streaks/XP/leaderboards added too early prevent measuring actual retention. Users chase points instead of learning. Launch MVP without gamification, validate retention first, add after learning loop proven effective. Address by deferring to Growth phase post-MVP.

## Implications for Roadmap

Based on research, the roadmap should follow a foundation-first approach establishing correct data models and core integrations before building user-facing features. Bulgarian's linguistic complexity and LLM/TTS integration requirements demand more upfront architecture work than typical CRUD apps.

### Phase 1: Foundation & Data Model
**Rationale:** Must establish correct schema for Bulgarian morphology before any vocabulary data exists. Changing core schema later requires expensive migration and risks data loss. PostgreSQL with proper lemma/inflection separation and JSONB metadata is non-negotiable from day one.

**Delivers:**
- PostgreSQL schema with Flyway migrations (lemmas, inflections, categories, word_lists, study_sessions)
- JPA entities with proper relationships (@OneToMany for inflections)
- Spring Data JPA repositories with basic CRUD
- PGroonga extension configured for Bulgarian full-text search
- Development environment (Docker Compose: PostgreSQL, Ollama)

**Addresses:**
- Pitfall #1 (morphological data modeling)
- Pitfall #6 (1st person lemma conventions)
- Foundation for table stakes features (word list management)

**Avoids:** Flat string storage, infinitive-based verb models, schema redesign pain later

### Phase 2: LLM Integration & Caching
**Rationale:** LLM integration is the most complex technical risk. Validating Ollama connectivity, prompt engineering for Bulgarian, and caching strategy before building user features prevents rework. Async patterns and circuit breakers must be correct from start or performance will tank.

**Delivers:**
- Spring AI 2.0 Ollama integration with auto-configuration
- LLM Service with async @Async processing and CompletableFuture
- Spring Cache configuration for LLM responses (semantic caching by prompt hash)
- Circuit breaker and retry logic (prevents cascading failures)
- Prompt templates for Bulgarian lemma detection and inflection generation
- Basic REST endpoint for testing LLM vocabulary creation

**Uses:**
- Spring Boot 4.2.x + Spring AI 2.0 from STACK.md
- Async service pattern from ARCHITECTURE.md

**Addresses:**
- Pitfall #2 (LLM hallucinations — establishes validation pipeline)
- Differentiator feature: LLM-powered lemma detection
- Performance trap: synchronous LLM calls blocking threads

**Avoids:** Treating LLM as ground truth, blocking request threads, no cache strategy

### Phase 3: TTS Audio Generation
**Rationale:** Audio is table stakes feature but independent of LLM work, can proceed in parallel. Edge TTS integration needs validation with native speaker before generating thousands of audio files. Caching strategy must work before making audio generation automatic.

**Delivers:**
- Edge TTS integration (Python microservice or Node.js library)
- Audio generation service with file-based caching (SHA-256 naming)
- Audio storage setup (local disk for MVP, S3-ready for production)
- REST endpoint for audio serving (GET /api/audio/{lemmaId})
- Background job queue for async audio generation
- HTML5 audio player component in React

**Uses:**
- Edge TTS from STACK.md
- Cache-aside pattern from ARCHITECTURE.md

**Addresses:**
- Table stakes: Audio playback for pronunciation
- Pitfall #4 (TTS quality validation)
- Performance trap: generating TTS on every review

**Avoids:** Storing audio as PostgreSQL BLOBs, no caching, vendor lock-in without fallback

### Phase 4: Core Vocabulary Management (MVP Backend)
**Rationale:** With data model, LLM, and TTS foundation in place, build the core CRUD operations that connect them. This delivers the backend for manual vocabulary entry before automating with text parsing.

**Delivers:**
- VocabularyController REST endpoints (CRUD for lemmas)
- LemmaService orchestrating LLM + TTS + persistence
- DTO layer (separate from entities) with MapStruct mappers
- Input validation with Bean Validation (JSR-380)
- Example sentence storage and management
- Basic search endpoint (leveraging PGroonga)
- Pagination support (Pageable) for large vocabularies

**Implements:**
- Lemma Service architecture component
- Repository pattern with proper fetch joins (avoid N+1)

**Addresses:**
- Table stakes: Word list management, manual vocabulary entry
- Pitfall #5 (require example sentences in data model)
- Anti-pattern: exposing entities directly in REST APIs

**Avoids:** N+1 query problems, missing example sentences, no validation

### Phase 5: Frontend Foundation & Vocabulary UI
**Rationale:** Backend is functional, now build modern React frontend with proper state management patterns from day one. TanStack Query + Zustand separation is easier to establish initially than refactor later.

**Delivers:**
- Vite 6 + React 19 + TypeScript project setup
- Tailwind CSS 4.1 configuration for styling
- React Router 7 for navigation
- TanStack Query 5.x setup with API client (Axios)
- Zustand 4.x stores for UI state
- Common components (Button, Input, Card) with Headless UI
- Vocabulary management page (list, add, edit, delete)
- Cyrillic input support and large clear fonts
- Mobile-responsive layout (PWA-ready)

**Uses:**
- React 19 + Vite + TanStack Query + Zustand from STACK.md
- State management pattern from ARCHITECTURE.md

**Addresses:**
- Table stakes: Mobile-responsive web app, Cyrillic support
- Anti-pattern: mixing server and client state in single store
- UX pitfall: poor Cyrillic font rendering

**Avoids:** CRA (deprecated), Redux complexity, prop-types (use TypeScript)

### Phase 6: Flashcard Study Mode (Core Learning Loop)
**Rationale:** This is the moment of truth — does the core learning loop work? All previous phases enable this. Must be excellent before adding advanced features or we're building on broken foundation.

**Delivers:**
- StudyController endpoints (fetch flashcards for word list)
- Basic spaced repetition algorithm (start simple, can enhance later)
- Study session tracking (reviews completed, accuracy)
- Flashcard component with flip animation
- Audio playback integration in flashcards
- Study progress display (streak, words studied, accuracy)
- Recognition mode (see Bulgarian, recall English)
- Study session UI state management (Zustand)

**Implements:**
- Study Session Service architecture component
- React Query + Zustand pattern for study flow

**Addresses:**
- Table stakes: Flashcard study mode, progress tracking, spaced repetition
- Integrates: Audio (Phase 3), Vocabulary (Phase 4-5)

**Avoids:** Overwhelming users with 500+ cards due, no progress visibility

### Phase 7: SRS Enhancement & Forgiveness
**Rationale:** Basic SRS from Phase 6 needs real-world hardening. Users will miss days, this phase makes the system forgiving and sustainable for long-term use.

**Delivers:**
- Sophisticated spaced repetition algorithm (FSRS or SM-2 with tuning)
- Forgiveness logic (cap daily reviews, gradual catch-up)
- Review session limits (max 100 cards)
- Priority-based review order (hardest first, or newest)
- "Review vacation" mode (pause new cards without breaking streak)
- Alert system (>50 cards due tomorrow warning)
- Analytics for detecting at-risk users (missed 3+ days)

**Addresses:**
- Pitfall #3 (SRS breaks with inconsistency)
- User retention (prevent "snowball of shame" abandonment)

**Avoids:** Unforgiving algorithms, review pile-up, user guilt mechanics

### Phase 8: Text Analysis & Bulk Import
**Rationale:** Manual entry is validated and working. Now add the differentiating feature that leverages Elena's tutor materials. This is complex (batch LLM processing) but high value for the specific use case.

**Delivers:**
- TextController for text analysis endpoint
- Text extraction service (regex-based Bulgarian word splitting)
- Batch LLM processing (parallel async calls for lemma detection)
- Deduplication logic (check existing vocabulary before creating)
- Frontend text input component (paste dialog)
- Word extraction review UI (show detected lemmas, check/uncheck)
- Bulk vocabulary creation endpoint
- Category auto-detection from text context (optional LLM feature)

**Uses:**
- LLM service from Phase 2 (batch async pattern)
- Text Analysis Service architecture component

**Addresses:**
- Differentiator: Text parsing for vocabulary extraction
- Differentiator: Tutor integration workflow
- Use case: Elena provides dialog printouts for study

**Avoids:** Sequential LLM calls (use parallel), no review before bulk creation

### Phase 9: Polish & Production Ready
**Rationale:** Core app is functional, now add operational requirements, user feedback mechanisms, and prepare for real-world use.

**Delivers:**
- User feedback mechanism (report incorrect inflections/translations)
- LLM output verification queue for human review
- Enhanced search (stemming, fuzzy matching)
- Recall mode flashcards (typing practice)
- Word categorization and tagging
- Offline PWA support (service workers, IndexedDB cache)
- Spring Boot Actuator endpoints (health checks, metrics)
- Monitoring and logging (detect LLM/TTS failures)
- Performance testing (5000+ words, 100+ daily reviews)
- Production deployment guide

**Addresses:**
- Pitfall #2 (validation pipeline for LLM hallucinations)
- "Looks done but isn't" checklist items
- Security: rate limiting on LLM features

**Avoids:** Shipping without validation mechanisms, no monitoring

### Phase Ordering Rationale

- **Foundation first (Phases 1-3):** Database schema, LLM integration, and TTS are architectural foundations that are expensive to change later. Getting these right before building features prevents rework.

- **Backend before frontend (Phase 4 before 5):** Backend API contracts need to stabilize before frontend consumes them. Allows backend testing with Postman before UI complexity.

- **Core loop validation before automation (Phase 6-7 before 8):** Manual vocabulary entry + flashcard study must work excellently before adding text parsing automation. Prevents building automation for broken workflow.

- **Features before polish (Phase 8 before 9):** Text parsing is differentiating feature that justifies the tool vs. generic flashcard apps. Polish items (offline, monitoring) are important but not value propositions.

- **Defer gamification entirely:** Not in Phase 9 because it should only be added after retention metrics are validated (Growth phase post-launch).

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2 (LLM Integration):** Prompt engineering for Bulgarian is trial-and-error intensive. May need `/gsd:research-phase` for optimal prompt templates, response parsing strategies, error handling patterns.
- **Phase 7 (SRS Algorithm):** Implementing FSRS or tuning SM-2 for Bulgarian morphological complexity may require algorithm research if simple approach proves insufficient.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Data Model):** PostgreSQL schema design is well-documented, JPA entity patterns are standard
- **Phase 4 (CRUD REST API):** Spring Boot REST controllers with validation are established patterns
- **Phase 5 (React + Vite):** Modern React setup is well-documented with official guides
- **Phase 6 (Flashcards):** UI component with state management follows standard React patterns

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All technologies verified with official documentation, version compatibility confirmed, Spring AI 2.0 + Spring Boot 4.2 + Ollama integration validated via recent 2025 tutorials |
| Features | HIGH | Feature landscape researched across 30+ language learning apps, table stakes vs. differentiators validated, Bulgarian-specific needs identified from learner feedback and app reviews |
| Architecture | HIGH | Patterns verified via Baeldung and Spring official guides, React Query + Zustand confirmed as 2025 standard (41% usage), async LLM patterns documented in multiple sources |
| Pitfalls | MEDIUM-HIGH | Critical pitfalls identified from Anki community, language learning research, and morphologically rich language papers; some Bulgarian-specific issues inferred from learner complaints rather than direct developer experience |

**Overall confidence:** HIGH

The stack, features, and architecture research all reached HIGH confidence through verification with official sources, current documentation (2025-2026), and multiple corroborating references. Pitfalls research is MEDIUM-HIGH because while the technical patterns are well-documented, some Bulgarian-specific issues are inferred from user feedback rather than first-hand developer accounts. The main gap is real-world validation of Edge TTS quality for Bulgarian — this requires testing with native speakers during Phase 3.

### Gaps to Address

**LLM prompt effectiveness for Bulgarian:** Research identified that Ollama can handle Bulgarian but didn't validate specific prompt patterns for lemma detection, inflection generation, or aspect pair identification. During Phase 2 implementation, expect iterative prompt engineering with native speaker validation. Mitigation: build prompt testing harness early, log all LLM outputs for review.

**Edge TTS Bulgarian voice quality:** Research confirmed Edge TTS supports Bulgarian with neural voices, but quality assessment requires native speaker evaluation. During Phase 3, test with Elena (the tutor) to validate stress patterns, consonant clusters, and ъ pronunciation before committing to bulk audio generation. Mitigation: have Google Cloud TTS or Amazon Polly as fallback provider.

**Spaced repetition algorithm tuning for Bulgarian:** Research identified need for forgiveness logic but didn't determine optimal intervals for morphologically complex languages. English-tuned SM-2 parameters may not transfer. During Phase 7, may need to research FSRS algorithm papers or consult spaced repetition communities. Mitigation: start with conservative intervals, collect retention data, adjust based on user performance.

**PGroonga full-text search performance:** Research confirmed PGroonga supports Bulgarian Cyrillic and offers parallel indexing, but performance characteristics at scale (5000+ vocabulary entries, complex queries) are untested. During Phase 1, need to benchmark index build times and query performance. Mitigation: design schema to allow Elasticsearch migration if PGroonga proves insufficient.

**Offline PWA capabilities:** Research noted offline-first architecture is valued but didn't detail conflict resolution strategies for vocabulary edits made offline. During Phase 9, need to research IndexedDB sync patterns and last-write-wins vs. operational transform strategies. Mitigation: start with read-only offline mode (flashcard review), defer edit sync to post-MVP.

## Sources

### Primary (HIGH confidence)

**Stack & Integration:**
- Spring AI GitHub Repository (github.com/spring-projects/spring-ai) — Version compatibility, Ollama integration patterns
- Spring AI Releases (v2.0.0-M2 verified) — Spring Boot 4.x requirements
- Using Ollama with Spring AI - Piotr's TechBlog (2025/03) — Integration tutorial
- Spring Boot 4 Migration Guide (Moderne.ai) — Version upgrade considerations
- PGroonga 4.0.0 Release Notes (postgresql.org) — Bulgarian Cyrillic support, parallel indexing
- React State Management in 2026 - Developer Way — TanStack Query + Zustand pattern
- Edge TTS Ultimate Guide (VideoSDK) — Bulgarian TTS capabilities
- Vite Advanced Guide 2025 (CodeParrot) — React 19 + Vite 6 setup

**Features & UX:**
- Best Vocabulary Learning Apps 2026 (Brighterly) — 15 app comparison, table stakes features
- Best Language Learning Apps 2026 (italki) — Feature landscape analysis
- Anki vs Duolingo comparison (SpeakAda, DuolingoGuides) — Competitor feature analysis
- Best Anki Alternatives 2026 (Goodoff) — 7 flashcard app reviews
- Best Apps to Learn Bulgarian (Preply, Ling-app, Langoly) — Bulgarian-specific challenges

**Architecture:**
- Spring Boot React CRUD (Baeldung, SpringBootTutorial) — Full-stack patterns
- Introduction to Spring AI (Baeldung) — LLM integration architecture
- Spring AI Ollama Integration (Layer5) — Local LLM patterns
- REST Pagination in Spring (Baeldung) — Pageable pattern
- Frontend System Design 2026 (SystemDesignHandbook) — React architecture

### Secondary (MEDIUM confidence)

**Pitfalls & Best Practices:**
- Spaced Repetition Guide (TrustWrites) — Common mistakes, inconsistent study habits
- Effectively Using Anki Flashcards (louisrli.github.io) — Recognition vs. production, isolation learning
- 2025 Language Learning Lessons (Lingoda) — Tool overload, AI over-reliance
- 13 Vocabulary Instruction Mistakes (GianfrancoConti) — Shallow processing, context gaps
- Ollama Caching Strategies (MarkAICode) — 300% performance improvement patterns

**Linguistic Data:**
- Building Multilingual Vocabulary Database (ResearchGate) — Morphological data patterns
- Lemma Dilemma paper (arXiv) — Lemma generation without domain-specific training
- Processing Morphologically Rich Languages (MIT Press) — Parsing and segmentation challenges
- LemmInflect GitHub — Lemmatization and inflection modeling

### Tertiary (LOW confidence, needs validation)

- LLM performance for morphologically complex languages (arXiv) — Data sparsity issues for Bulgarian
- Building Long-Running TTS Pipelines (vadim.blog) — Architecture patterns for audio generation
- UX Case Study: Duolingo (UsabilityGeek) — Gamification patterns (but defer for this project)

---
*Research completed: 2026-02-15*
*Ready for roadmap: yes*
