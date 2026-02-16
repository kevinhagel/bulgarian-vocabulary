---
phase: 04-core-vocabulary-management
plan: 01
subsystem: api-layer
tags: [dto, validation, mapstruct, exception-handling, repository, pagination]
completed: 2026-02-16

dependency_graph:
  requires:
    - domain-model (Phase 01)
    - enum-types (Phase 01)
    - LemmaRepository (Phase 01)
  provides:
    - API DTO layer with validation groups
    - MapStruct annotation processor
    - Global exception handling with RFC 7807 ProblemDetail
    - Paginated repository queries with multi-field filtering
  affects:
    - Phase 04 Plan 02 (service and controller layers depend on these DTOs)

tech_stack:
  added:
    - MapStruct 1.6.2 (compile-time DTO mapping)
    - Jakarta Validation groups (OnCreate, OnUpdate)
    - Spring Data Pagination (Page, Pageable)
  patterns:
    - Java Records for immutable DTOs
    - RFC 7807 ProblemDetail for REST errors
    - Validation groups for operation-specific validation
    - Derived query methods with Pageable support
    - Batch JOIN FETCH after pagination (N+1 prevention)

key_files:
  created:
    - backend/src/main/java/com/vocab/bulgarian/api/dto/OnCreate.java
    - backend/src/main/java/com/vocab/bulgarian/api/dto/OnUpdate.java
    - backend/src/main/java/com/vocab/bulgarian/api/dto/CreateLemmaRequestDTO.java
    - backend/src/main/java/com/vocab/bulgarian/api/dto/UpdateLemmaRequestDTO.java
    - backend/src/main/java/com/vocab/bulgarian/api/dto/InflectionUpdateDTO.java
    - backend/src/main/java/com/vocab/bulgarian/api/dto/LemmaResponseDTO.java
    - backend/src/main/java/com/vocab/bulgarian/api/dto/LemmaDetailDTO.java
    - backend/src/main/java/com/vocab/bulgarian/api/dto/InflectionDTO.java
    - backend/src/main/java/com/vocab/bulgarian/api/exception/GlobalExceptionHandler.java
  modified:
    - backend/pom.xml (added MapStruct dependency and annotation processor)
    - backend/src/main/java/com/vocab/bulgarian/repository/LemmaRepository.java (added pagination)

decisions:
  - key: "CreateLemmaRequestDTO uses wordForm field (not text)"
    rationale: "User input is any inflected form - service layer determines canonical lemma"
    impact: "Service must invoke LLM lemma detection before persisting"
  - key: "UpdateLemmaRequestDTO uses text field (not wordForm)"
    rationale: "Updating existing lemma means working with canonical form"
    impact: "No LLM processing needed for updates"
  - key: "Separate LemmaResponseDTO and LemmaDetailDTO"
    rationale: "List view needs inflectionCount, detail view needs full inflections"
    impact: "Avoids N+1 problem in list views, provides full data in detail view"
  - key: "Batch-load pattern with findByIdInWithInflections"
    rationale: "Pagination returns IDs only, then batch-load with JOIN FETCH"
    impact: "Enables pagination while avoiding N+1 queries"
  - key: "Seven paginated filter methods for all combinations"
    rationale: "Support browse by source, partOfSpeech, difficultyLevel individually and combined"
    impact: "Flexible filtering without complex query builder"
  - key: "GlobalExceptionHandler logs but doesn't expose Exception details"
    rationale: "Security - don't leak stack traces or sensitive data to clients"
    impact: "Generic 500 message for unexpected errors, full logging server-side"

metrics:
  duration_minutes: 3
  tasks_completed: 2
  files_created: 9
  files_modified: 2
  commits: 2
  commit_hashes: [6a70604, 76d67ad]
---

# Phase 04 Plan 01: API DTO Layer and Foundation Summary

**One-liner:** MapStruct-based DTO layer with validation groups, RFC 7807 exception handling, and paginated repository queries supporting multi-field filtering.

## Objective Achievement

Successfully established the complete DTO layer, build-time MapStruct integration, global exception handling, and enhanced repository with pagination/filtering for Phase 4 vocabulary CRUD operations.

## Implementation Details

### Task 1: MapStruct Dependency and API DTOs

**Deliverables:**
- Added MapStruct 1.6.2 to pom.xml with maven-compiler-plugin annotation processor configuration
- Created validation group marker interfaces (OnCreate, OnUpdate) for operation-specific validation
- Created 3 request DTOs as Java Records:
  - `CreateLemmaRequestDTO`: wordForm (any inflected form), translation, notes
  - `UpdateLemmaRequestDTO`: text (canonical lemma), translation, notes, inflections list
  - `InflectionUpdateDTO`: id (nullable), form, grammaticalInfo
- Created 3 response DTOs as Java Records:
  - `LemmaResponseDTO`: List view with inflectionCount (not full inflections)
  - `LemmaDetailDTO`: Detail view with full inflections list
  - `InflectionDTO`: Inflection representation for responses

**Key Design Choices:**
- Java Records for immutable DTOs (auto-generates constructor, getters, equals, hashCode)
- Jakarta Validation annotations with validation groups enable different rules for create vs update
- CreateLemmaRequestDTO.wordForm accepts user input in any form (will be processed by LLM)
- UpdateLemmaRequestDTO.text works with canonical lemma (no LLM processing needed)
- Separate list/detail DTOs optimize for different use cases (count vs full data)

### Task 2: Global Exception Handler and Paginated Repository

**Deliverables:**
- Created `GlobalExceptionHandler` with @RestControllerAdvice:
  - EntityNotFoundException → 404 with ProblemDetail (title, detail)
  - MethodArgumentNotValidException → 400 with field errors list
  - MethodArgumentTypeMismatchException → 400 with parameter name
  - Exception (catch-all) → 500 with generic message, full logging server-side
- Enhanced `LemmaRepository` with pagination support:
  - 7 derived query methods with Pageable parameter for filtering combinations
  - Individual filters: findBySource, findByPartOfSpeech, findByDifficultyLevel
  - Combined filters: all 2-way and 3-way combinations
  - Batch-load method: findByIdInWithInflections for N+1 prevention
- Preserved all existing repository methods (searchByText, findByIdWithInflections, etc.)

**Key Design Choices:**
- RFC 7807 ProblemDetail provides standardized REST error responses
- Security: catch-all handler logs full exception but returns generic message to client
- Pagination pattern: query returns Page with IDs, then batch-load with JOIN FETCH for inflections
- Seven filter methods cover all use cases without complex query builder

## Verification Results

All success criteria met:

1. ✅ `mvn compile -q` passes with zero errors
2. ✅ All 8 files exist in `backend/src/main/java/com/vocab/bulgarian/api/dto/`
3. ✅ MapStruct appears in pom.xml dependencies AND annotationProcessorPaths
4. ✅ LemmaRepository has both old methods and new Pageable methods
5. ✅ GlobalExceptionHandler has @RestControllerAdvice annotation

## Deviations from Plan

None - plan executed exactly as written.

## Technical Artifacts

**New Packages:**
- `com.vocab.bulgarian.api.dto` (8 files)
- `com.vocab.bulgarian.api.exception` (1 file)

**Build Configuration:**
- MapStruct 1.6.2 dependency
- maven-compiler-plugin 3.13.0 with annotationProcessorPaths

**Repository Enhancements:**
- 7 new paginated query methods
- 1 batch-load method for N+1 prevention

## Integration Points

**Upstream Dependencies:**
- Domain model (Lemma, Inflection entities from Phase 01)
- Enum types (Source, PartOfSpeech, DifficultyLevel, ReviewStatus from Phase 01)
- LemmaRepository (extended with pagination)

**Downstream Consumers:**
- Phase 04 Plan 02: Service layer will use DTOs for business logic
- Phase 04 Plan 02: Controller layer will accept request DTOs and return response DTOs
- MapStruct mappers will use these DTOs for entity-DTO conversion

## Next Steps

Plan 04-02 will implement:
- MapStruct mappers (entity ↔ DTO conversion)
- VocabularyService (CRUD business logic with LLM integration)
- VocabularyController (REST endpoints using these DTOs)

The foundation is now in place for clean separation between API contracts (DTOs) and domain model (entities).

## Self-Check: PASSED

✅ All created files exist:
- FOUND: backend/pom.xml (modified)
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/dto/OnCreate.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/dto/OnUpdate.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/dto/CreateLemmaRequestDTO.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/dto/UpdateLemmaRequestDTO.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/dto/InflectionUpdateDTO.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/dto/LemmaResponseDTO.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/dto/LemmaDetailDTO.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/dto/InflectionDTO.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/api/exception/GlobalExceptionHandler.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/repository/LemmaRepository.java (modified)

✅ All commits exist:
- FOUND: 6a70604 (Task 1: MapStruct and DTOs)
- FOUND: 76d67ad (Task 2: GlobalExceptionHandler and pagination)
