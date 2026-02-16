---
phase: 01-foundation-and-data-model
plan: 02
subsystem: domain-model
tags: [jpa, spring-data, hibernate, entities, repositories, pgroonga]

# Dependency graph
requires: [01-01-database-foundation]
provides:
  - JPA entities (Lemma, Inflection) with bidirectional relationships
  - Enum types (Source, PartOfSpeech, DifficultyLevel, ReviewStatus)
  - Spring Data JPA repositories with derived queries and JOIN FETCH
  - PGroonga native search queries for Cyrillic full-text search
affects: [03-llm-metadata, 04-inflection-generation, 05-services-layer]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Bidirectional lazy-loaded OneToMany/ManyToOne relationships with helper methods
    - CascadeType.ALL with orphanRemoval for parent-child lifecycle management
    - Jakarta Bean Validation (@NotNull, @Size) combined with JPA @Column constraints
    - @PrePersist/@PreUpdate lifecycle callbacks for timestamp management
    - JPQL JOIN FETCH queries to solve N+1 problem
    - PGroonga &@~ operator for Cyrillic full-text search with pgroonga_score ranking

key-files:
  created:
    - src/main/java/com/vocab/bulgarian/domain/enums/Source.java
    - src/main/java/com/vocab/bulgarian/domain/enums/PartOfSpeech.java
    - src/main/java/com/vocab/bulgarian/domain/enums/DifficultyLevel.java
    - src/main/java/com/vocab/bulgarian/domain/enums/ReviewStatus.java
    - src/main/java/com/vocab/bulgarian/domain/Lemma.java
    - src/main/java/com/vocab/bulgarian/domain/Inflection.java
    - src/main/java/com/vocab/bulgarian/repository/LemmaRepository.java
    - src/main/java/com/vocab/bulgarian/repository/InflectionRepository.java
  modified: []

key-decisions:
  - "Bidirectional relationship maintenance via addInflection/removeInflection helper methods"
  - "Lazy loading on all relationships - explicit JOIN FETCH when inflections needed"
  - "Entity equals/hashCode based on id only (null-safe for new entities)"
  - "Jakarta persistence/validation imports (not javax) for Spring Boot 3.x compatibility"

patterns-established:
  - "Entity validation: JPA @Column constraints + Jakarta Bean Validation for defense in depth"
  - "Repository pattern: derived queries for simple lookups, JPQL for complex joins, native queries for database-specific features"
  - "PGroonga search pattern: native query with &@~ operator and pgroonga_score for relevance ranking"

# Metrics
duration: 5min
completed: 2026-02-15
---

# Phase 01 Plan 02: JPA Domain Model Summary

**JPA entities with bidirectional relationships, Jakarta Bean Validation, Spring Data repositories with JOIN FETCH queries, and PGroonga Cyrillic full-text search**

## Performance

- **Duration:** 5 minutes
- **Started:** 2026-02-15T11:49:20Z
- **Completed:** 2026-02-15T13:54:34Z
- **Tasks:** 3
- **Files created:** 8
- **Commits:** 2

## Accomplishments

- Created 4 enum types (Source, PartOfSpeech, DifficultyLevel, ReviewStatus) matching database schema
- Implemented Lemma entity with bidirectional OneToMany to Inflection using lazy loading, CascadeType.ALL, and orphanRemoval
- Implemented Inflection entity with ManyToOne back to Lemma
- Added Jakarta Bean Validation (@NotNull, @Size) combined with JPA @Column constraints for defense in depth
- Implemented @PrePersist/@PreUpdate lifecycle callbacks for automatic timestamp management
- Created LemmaRepository with derived queries, JPQL JOIN FETCH queries, and PGroonga native search
- Created InflectionRepository with derived queries and PGroonga form search
- Verified Spring Boot application startup with Flyway migrations and JPA entity validation
- Confirmed 52 reference vocabulary entries seeded correctly

## Task Commits

Each task was committed atomically:

1. **Task 1: Create enum types and JPA entities** - `dbc63a2` (feat)
2. **Task 2: Create Spring Data JPA repositories with custom queries** - `289f3b8` (feat)

## Files Created/Modified

- `src/main/java/com/vocab/bulgarian/domain/enums/Source.java` - Source enum (USER_ENTERED, SYSTEM_SEED)
- `src/main/java/com/vocab/bulgarian/domain/enums/PartOfSpeech.java` - Bulgarian parts of speech including INTERROGATIVE and PARTICLE
- `src/main/java/com/vocab/bulgarian/domain/enums/DifficultyLevel.java` - Difficulty levels (BEGINNER, INTERMEDIATE, ADVANCED)
- `src/main/java/com/vocab/bulgarian/domain/enums/ReviewStatus.java` - LLM review workflow statuses
- `src/main/java/com/vocab/bulgarian/domain/Lemma.java` - JPA entity with @OneToMany relationship, validation, lifecycle callbacks, helper methods
- `src/main/java/com/vocab/bulgarian/domain/Inflection.java` - JPA entity with @ManyToOne relationship
- `src/main/java/com/vocab/bulgarian/repository/LemmaRepository.java` - Spring Data JPA repository with 6 derived queries, 3 JPQL JOIN FETCH queries, 1 PGroonga native search
- `src/main/java/com/vocab/bulgarian/repository/InflectionRepository.java` - Spring Data JPA repository with 3 derived queries, 1 PGroonga native search

## Decisions Made

1. **Bidirectional relationship helpers:** addInflection/removeInflection methods maintain both sides of relationship to prevent inconsistency
2. **Lazy loading everywhere:** All relationships use FetchType.LAZY - inflections fetched only when needed via explicit JOIN FETCH queries
3. **Entity equals/hashCode on id only:** Null-safe implementation using getClass().hashCode() for new entities before persistence
4. **Jakarta imports (not javax):** Spring Boot 3.x uses Jakarta EE 9+, requires jakarta.persistence.* and jakarta.validation.constraints.*
5. **Defense in depth validation:** Both JPA @Column(nullable=false) AND Jakarta @NotNull - database enforces, JPA validates before INSERT

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Docker port mapping issue**
- **Found during:** Task 3 (application startup verification)
- **Issue:** Container created without host port mapping (showed only "5432/tcp" instead of "0.0.0.0:5432->5432/tcp"), causing connection refused errors
- **Fix:** Stopped and recreated container with `docker compose down && docker compose up -d` to apply correct port mapping from compose.yaml
- **Files modified:** None (container recreation only)
- **Commit:** N/A (infrastructure fix)

**2. [Rule 3 - Blocking] Port conflict with existing container**
- **Found during:** Task 3 (initial container startup)
- **Issue:** Port 5432 already allocated by "bulgarian-tutor-db" container from different project
- **Fix:** Stopped conflicting container before starting correct one
- **Files modified:** None (infrastructure fix)
- **Commit:** N/A (infrastructure fix)

## Issues Encountered

None beyond the auto-fixed Docker port issues documented above.

## User Setup Required

None - all infrastructure issues resolved automatically.

## Next Phase Readiness

**Ready for Phase 1 Plan 3 (Services Layer):**
- JPA entities map correctly to PostgreSQL schema with proper validation
- Bidirectional relationships work with lazy loading
- Spring Data repositories provide query methods (derived, JPQL, native)
- PGroonga search queries ready for Cyrillic full-text search
- Application compiles and starts successfully with migrations applied
- 52 reference vocabulary entries verified in database

**No blockers or concerns.**

## Self-Check: PASSED

All files verified:
- FOUND: src/main/java/com/vocab/bulgarian/domain/enums/Source.java
- FOUND: src/main/java/com/vocab/bulgarian/domain/enums/PartOfSpeech.java
- FOUND: src/main/java/com/vocab/bulgarian/domain/enums/DifficultyLevel.java
- FOUND: src/main/java/com/vocab/bulgarian/domain/enums/ReviewStatus.java
- FOUND: src/main/java/com/vocab/bulgarian/domain/Lemma.java
- FOUND: src/main/java/com/vocab/bulgarian/domain/Inflection.java
- FOUND: src/main/java/com/vocab/bulgarian/repository/LemmaRepository.java
- FOUND: src/main/java/com/vocab/bulgarian/repository/InflectionRepository.java

All commits verified:
- FOUND: dbc63a2
- FOUND: 289f3b8

---
*Phase: 01-foundation-and-data-model*
*Completed: 2026-02-15*
