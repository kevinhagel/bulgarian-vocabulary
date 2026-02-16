---
phase: 01-foundation-and-data-model
plan: 01
subsystem: database
tags: [spring-boot, postgresql, pgroonga, flyway, jpa, docker-compose]

# Dependency graph
requires: []
provides:
  - Spring Boot 3.4.2 project with JPA, PostgreSQL, Flyway, Docker Compose integration
  - PostgreSQL database with PGroonga extension for Cyrillic full-text search
  - Lemmas and inflections tables with proper constraints and indexes
  - Reference vocabulary seed data (interrogatives, pronouns, prepositions, conjunctions, numerals)
  - Development profile with Ollama Mac Studio connectivity configuration
affects: [02-domain-model, 03-llm-metadata, 04-inflection-generation]

# Tech tracking
tech-stack:
  added:
    - spring-boot-starter-data-jpa 3.4.2
    - spring-boot-starter-web 3.4.2
    - spring-boot-starter-validation 3.4.2
    - postgresql driver (runtime)
    - flyway-core and flyway-database-postgresql
    - spring-boot-docker-compose (runtime)
    - groonga/pgroonga Docker image
  patterns:
    - Flyway strict chronological versioning (V1, V2, V3...)
    - Immutable migrations (marked IMMUTABLE, never modify after applied)
    - JPA validate mode (Flyway owns schema, Hibernate validates)
    - Lemma-based domain model (dictionary headword + inflections)
    - Source field distinguishes USER_ENTERED vs SYSTEM_SEED vocabulary

key-files:
  created:
    - pom.xml
    - compose.yaml
    - src/main/java/com/vocab/bulgarian/BulgarianVocabularyApplication.java
    - src/main/resources/application.yml
    - src/main/resources/application-dev.yml
    - src/main/resources/db/migration/V1__create_schema.sql
    - src/main/resources/db/migration/V2__seed_reference_data.sql
    - .gitignore
  modified: []

key-decisions:
  - "PGroonga extension chosen for Cyrillic full-text search (superior to PostgreSQL's built-in text search for Bulgarian)"
  - "Nullable LLM fields (part_of_speech, category, difficulty_level) - filled in Phase 2"
  - "ON DELETE CASCADE on inflections.lemma_id - deleting lemma deletes its inflections"
  - "Unique constraint on (text, source) - prevents duplicates within source type, allows same text across sources"
  - "review_status field for LLM metadata review workflow (PENDING, REVIEWED, NEEDS_CORRECTION)"

patterns-established:
  - "Flyway migrations: V1__create_schema.sql for structure, V2__seed_reference_data.sql for data"
  - "SYSTEM_SEED reference vocabulary marked with review_status='REVIEWED' (pre-verified)"
  - "Docker Compose auto-start via spring-boot-docker-compose dependency"
  - "Development profile (application-dev.yml) for environment-specific overrides"

# Metrics
duration: 2min
completed: 2026-02-15
---

# Phase 01 Plan 01: Database Foundation Summary

**Spring Boot 3.4.2 with PostgreSQL+PGroonga, Flyway migrations creating lemma/inflection schema, and 52 seeded reference vocabulary entries**

## Performance

- **Duration:** 2 minutes
- **Started:** 2026-02-15T11:44:21Z
- **Completed:** 2026-02-15T11:46:23Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Spring Boot project bootstrapped with all core dependencies (JPA, PostgreSQL, Flyway, Validation, Docker Compose)
- PostgreSQL database with PGroonga extension configured via Docker Compose
- Complete database schema with lemmas and inflections tables, PGroonga indexes, and proper constraints
- Reference vocabulary seeded: 8 interrogatives, 8 pronouns, 16 prepositions, 10 conjunctions, 10 numerals (52 total entries)
- Development environment configured for Ollama Mac Studio LAN connectivity

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Spring Boot project with Docker Compose and configuration** - `7bfa875` (feat)
2. **Task 2: Create Flyway migrations for schema and reference data** - `0bb4c3e` (feat)

## Files Created/Modified

- `pom.xml` - Maven project with Spring Boot 3.4.2, JPA, PostgreSQL, Flyway, Validation dependencies
- `compose.yaml` - PostgreSQL 16 with PGroonga extension (groonga/pgroonga:latest image)
- `src/main/java/com/vocab/bulgarian/BulgarianVocabularyApplication.java` - Spring Boot main application class
- `src/main/resources/application.yml` - Database connection, JPA validate mode, Flyway configuration, Docker Compose integration
- `src/main/resources/application-dev.yml` - Development profile with SQL logging and Ollama Mac Studio URL
- `src/main/resources/db/migration/V1__create_schema.sql` - Schema with lemmas/inflections tables, PGroonga indexes, constraints
- `src/main/resources/db/migration/V2__seed_reference_data.sql` - Reference vocabulary seed data (52 entries)
- `.gitignore` - Maven, IDE, OS, environment file ignores

## Decisions Made

1. **PGroonga for Cyrillic search:** Chosen over PostgreSQL's built-in full-text search for superior Bulgarian/Cyrillic support
2. **Nullable LLM fields:** part_of_speech, category, difficulty_level are nullable because LLM fills them in Phase 2
3. **CASCADE deletion:** inflections.lemma_id ON DELETE CASCADE ensures deleting a lemma removes its inflections
4. **Unique constraint on (text, source):** Prevents duplicate lemmas within same source type but allows same text as both USER_ENTERED and SYSTEM_SEED
5. **review_status field:** Tracks LLM metadata review workflow (PENDING, REVIEWED, NEEDS_CORRECTION)
6. **IMMUTABLE migrations:** V2 seed data marked immutable - future updates require new migrations (e.g., V5__update_pronouns.sql)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all tasks completed without issues.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for Phase 1 Plan 2 (Domain Model):**
- Database schema established with lemmas and inflections tables
- PGroonga indexes ready for Cyrillic full-text search
- Reference vocabulary seeded and ready for use
- JPA entities can now be created to map to existing schema

**No blockers or concerns.**

## Self-Check: PASSED

All files verified:
- FOUND: pom.xml
- FOUND: compose.yaml
- FOUND: src/main/java/com/vocab/bulgarian/BulgarianVocabularyApplication.java
- FOUND: src/main/resources/application.yml
- FOUND: src/main/resources/application-dev.yml
- FOUND: src/main/resources/db/migration/V1__create_schema.sql
- FOUND: src/main/resources/db/migration/V2__seed_reference_data.sql
- FOUND: .gitignore

All commits verified:
- FOUND: 7bfa875
- FOUND: 0bb4c3e

---
*Phase: 01-foundation-and-data-model*
*Completed: 2026-02-15*
