# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-15)

**Core value:** Each vocabulary entry must accurately represent the lemma (dictionary headword) with all its inflections, English translation, and metadata, enabling effective study through audio playback and interactive learning tools.
**Current focus:** Phase 5 - Frontend Foundation and Vocabulary UI

## Current Position

Phase: 6 of 8 (Flashcards & Basic Study) - Planning complete, ready to execute
Plan: Phase 5 complete, Phase 6 planned (4 plans written)
Status: Phase 6 plans written and ready to execute
Last activity: 2026-02-17 — Planned Phase 6: Flashcards & Basic Study (4/4 plans written)

Progress: [████████████] 62.5%

## Performance Metrics

**Velocity:**
- Total plans completed: 11
- Average duration: 3.5 min
- Total execution time: 0.63 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01    | 2     | 7min  | 3.5min   |
| 02    | 2     | 7min  | 3.5min   |
| 03    | 2     | 4min  | 2.0min   |
| 04    | 2     | 5min  | 2.5min   |
| 05    | 3     | 16min | 5.3min  |

**Recent Trend:**
- Last 5 plans: 2min, 5min, 5.5min, 6min
- Trend: Frontend work (5-6 min/plan) vs backend (2-3 min/plan)

*Updated after each plan completion*

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 05 P02 | 5.5min | 2 tasks | 9 files |
| Phase 05 P03 | 6min | 2 tasks | 16 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Lemma-based domain model aligns with linguistic standards (1st person singular for Bulgarian verbs)
- Single Lemma entity with source field distinguishes user-entered vs system-seeded reference vocabulary
- JPA (Spring Data JPA) chosen for rich domain model with relationships
- Text detection extracts single words only (multi-word lemmas manually entered)
- Flyway seed data for reference vocabulary (interrogatives, pronouns, prepositions, conjunctions, numerals)
- PGroonga extension chosen for Cyrillic full-text search (superior to PostgreSQL's built-in text search for Bulgarian) (01-01)
- Nullable LLM fields (part_of_speech, category, difficulty_level) - filled in Phase 2 (01-01)
- ON DELETE CASCADE on inflections.lemma_id - deleting lemma deletes its inflections (01-01)
- Unique constraint on (text, source) - prevents duplicates within source type, allows same text across sources (01-01)
- review_status field for LLM metadata review workflow (PENDING, REVIEWED, NEEDS_CORRECTION) (01-01)
- Bidirectional relationship maintenance via helper methods (addInflection/removeInflection) (01-02)
- Lazy loading on all relationships with explicit JOIN FETCH when needed (01-02)
- Jakarta imports (not javax) for Spring Boot 3.x compatibility (01-02)
- Defense in depth validation: JPA @Column + Jakarta Bean Validation (01-02)
- Spring AI BOM 1.1.0-M3 milestone version provides ChatClient.Builder auto-configuration (02-01)
- Resilience4j 2.2.0 explicit version (not in Spring Boot BOM) for circuit breaker (02-01)
- Redis cache with 24h TTL and JSON serialization for structured DTO caching (02-01)
- CallerRunsPolicy for thread pool rejection to apply backpressure on overload (02-01)
- Dev environment uses Mac Studio LAN endpoint (mac-studio.local:11434) for Ollama (02-01)
- Bulgarian language expert system message enforces JSON-only responses (02-01)
- Two-layer async pattern: @Async wrapper calls synchronous @Cacheable method to avoid caching CompletableFuture wrappers (02-02)
- Cache key normalization: trim().toLowerCase() for case-insensitive deduplication (02-02)
- Fallback strategies: LemmaDetectionService returns failed() response, other services return null for graceful degradation (02-02)
- Bulgarian morphology minimums: VERB≥6, NOUN≥2, ADJECTIVE≥3 inflections (02-02)
- Cyrillic validation: regex .*[а-яА-Я].* ensures Bulgarian text content (02-02)
- Partial failure handling: orchestration tracks warnings list, continues with null inflections/metadata (02-02)
- Orchestration does NOT persist to DB: produces LlmProcessingResult DTO for Phase 4 vocabulary service (02-02)
- Audio thread pool sizing: 2-4 threads (vs 4-8 for LLM) due to I/O-bound nature of edge-tts process (03-01)
- Thread-safe content hashing: local MessageDigest instance per call (MessageDigest is NOT thread-safe) (03-01)
- Pipe separator in hash input prevents collisions between text+voice combinations (03-01)
- Fail-fast storage initialization via @PostConstruct ensures audio directory exists before any generation (03-01)
- Dev environment uses external SSD for audio storage (/Volumes/T7-NorthStar) for more space and faster I/O (03-01)
- [Phase 03]: ProcessBuilder with separate arguments prevents shell injection
- [Phase 03]: Temp file + atomic move prevents partial MP3 files from being served
- [Phase 03]: Process output consumption (BufferedReader loop) prevents buffer blocking
- [Phase 03]: 30-second timeout for edge-tts prevents indefinite hangs
- [Phase 03]: Cache key uses hash (not Cyrillic text) to avoid encoding issues in Redis
- [Phase 03]: Three-layer path security for REST endpoint: whitelist regex, path normalization, existence check
- [Phase 03]: Immutable content-hash filenames enable aggressive browser caching (max-age=31536000)
- [Phase 04]: CreateLemmaRequestDTO.wordForm accepts any inflected form (user input), UpdateLemmaRequestDTO.text works with canonical lemma
- [Phase 04]: Separate LemmaResponseDTO (list view with inflectionCount) and LemmaDetailDTO (detail view with full inflections) to avoid N+1 in lists
- [Phase 04]: Batch-load pattern: pagination returns IDs, then findByIdInWithInflections for JOIN FETCH
- [Phase 04]: Seven paginated filter methods for all combinations of source/partOfSpeech/difficultyLevel
- [Phase 04]: GlobalExceptionHandler logs full exceptions but returns generic 500 message (security)
- [Phase 04]: MapStruct default methods for complex entity mapping (LlmProcessingResult → Lemma with enum parsing)
- [Phase 04]: Async create endpoint returns CompletableFuture to prevent thread blocking during LLM processing
- [Phase 04]: Review status reset to PENDING on update (user edits may invalidate reviewed metadata)
- [Phase 04]: Accept inflectionCount=0 in paginated lists (lazy loading trade-off, detail view uses JOIN FETCH)
- [Phase 05]: Tailwind CSS 4 uses @theme CSS-based configuration instead of tailwind.config.js for simpler setup
- [Phase 05]: @ path alias for clean imports configured in vite.config.ts and tsconfig.app.json
- [Phase 05]: Vite proxy for /api requests to localhost:8080 avoids CORS issues during development
- [Phase 05]: TanStack Query with 5-minute stale time reduces backend load for infrequently-changing vocabulary data
- [Phase 05]: Sofia Sans font loaded via Google Fonts for excellent Bulgarian Cyrillic support
- [Phase 05]: AudioPlayer component uses SVG icons instead of emojis for cross-platform consistency
- [Phase 05]: POST /api/audio/generate endpoint returns CompletableFuture for async audio generation
- [Phase 05 P02]: 300ms debounce delay for search input balances responsiveness and API call reduction
- [Phase 05 P02]: Search requires 2+ characters to prevent expensive PGroonga queries for single character
- [Phase 05 P02]: Module-level Map<text, audioUrl> cache prevents re-generating audio across component re-renders
- [Phase 05 P02]: Conditional fetching: useSearchVocabulary for query >= 2 chars, useVocabulary for browse mode
- [Phase 05 P02]: Pagination shows page numbers with ellipsis for large sets (max 7 visible) for compact navigation
- [Phase 05 P03]: Use single VocabularyForm component for both create and edit modes (reduces duplication)
- [Phase 05 P03]: Show LLM processing indicator during create mutation (async backend processing)
- [Phase 05 P03]: Types barrel exports (@/types) created for cleaner imports and consistency

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-02-17 (planning)
Stopped at: Phase 6 planning complete — 4 plans written
Resume file: .planning/phases/06-flashcards-basic-study/06-01-PLAN.md
Next action: Execute 06-01-PLAN.md (V5 migration, SRS entities, SM-2 algorithm, study session API)

---
*State initialized: 2026-02-15*
*Last updated: 2026-02-16 (Plan 05-03 complete)*
