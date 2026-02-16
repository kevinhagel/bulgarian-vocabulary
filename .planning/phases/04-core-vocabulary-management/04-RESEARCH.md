# Phase 4: Core Vocabulary Management - Research

**Researched:** 2026-02-16
**Domain:** Spring Boot REST API CRUD operations with LLM orchestration, DTO patterns, and validation
**Confidence:** HIGH

## Summary

Phase 4 implements CRUD operations for Bulgarian vocabulary management with LLM-powered metadata generation and reference data seeding. The architecture builds on Spring Boot 3.4.2 with Spring Data JPA for persistence, leveraging existing LLM orchestration services from Phase 2. The standard pattern is a 3-tier layered architecture: REST controller layer for HTTP endpoints, service layer for business logic and LLM orchestration integration, and repository layer for database access. Critical requirements include DTO-entity separation for security, validation groups for create/update scenarios, transactional service operations with cascading inflection management, and review workflow using the existing ReviewStatus enum. Reference vocabulary (interrogatives, pronouns, prepositions, conjunctions, numerals) is already seeded via Flyway migration V2__seed_reference_data.sql.

**Primary recommendation:** Use Java Records for DTOs with MapStruct for entity mapping, implement validation groups (OnCreate/OnUpdate) for context-specific validation, handle LLM orchestration results in service layer with @Transactional boundaries, and use @RestControllerAdvice for global exception handling with ProblemDetail responses.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.4.2 | Foundation framework | Latest stable, production-ready with Jakarta EE 10 |
| Spring Data JPA | 3.4.2 | Repository abstraction | Built-in pagination, sorting, filtering with Pageable |
| Spring Validation | 3.4.2 | Input validation | Jakarta Bean Validation 3.0 reference implementation |
| PostgreSQL JDBC | runtime | Database driver | Already configured in project for PostgreSQL 17.3 |
| Flyway | 10.21.0+ | Schema migrations | Already in use for schema + seed data versioning |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| MapStruct | 1.6.2+ | DTO mapping | Compile-time code generation, type-safe, zero-reflection overhead |
| Jackson | 2.18.2+ | JSON serialization | Default Spring Boot JSON processor, bidirectional relationship handling |
| Resilience4j | 2.2.0 | Circuit breaker (already configured) | LLM orchestration error handling, already in project |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| MapStruct | ModelMapper | ModelMapper uses reflection (slower), MapStruct is compile-time |
| Java Records | Traditional POJOs | Records provide immutability and conciseness for DTOs |
| Spring Validation | Custom validators | Standard annotations cover 95% of validation needs |

**Installation:**
```bash
# MapStruct addition to existing pom.xml
<properties>
    <mapstruct.version>1.6.2</mapstruct.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Architecture Patterns

### Recommended Project Structure
```
backend/src/main/java/com/vocab/bulgarian/
├── api/                       # NEW: REST API layer
│   ├── controller/            # REST controllers
│   ├── dto/                   # API DTOs (requests/responses)
│   └── mapper/                # MapStruct mappers
├── service/                   # NEW: Business logic layer
│   └── exception/             # Custom business exceptions
├── domain/                    # EXISTING: JPA entities
│   └── enums/
├── repository/                # EXISTING: Spring Data repositories
├── llm/                       # EXISTING: LLM orchestration
│   ├── service/
│   └── dto/
└── audio/                     # EXISTING: Audio services
```

### Pattern 1: Service Layer with LLM Orchestration Integration
**What:** Service layer coordinates business logic, integrates LLM results, and persists to database
**When to use:** For vocabulary creation where LLM processing precedes persistence

**Example:**
```java
// Source: Phase 2 LlmOrchestrationService + Phase 4 design
@Service
public class VocabularyService {
    private final LemmaRepository lemmaRepository;
    private final LlmOrchestrationService llmOrchestrationService;
    private final LemmaMapper lemmaMapper;

    @Transactional
    public CompletableFuture<LemmaResponseDTO> createVocabularyEntry(CreateLemmaRequestDTO request) {
        // Step 1: LLM orchestration (async, returns LlmProcessingResult)
        return llmOrchestrationService.processNewWord(request.wordForm())
            .thenApply(llmResult -> {
                // Step 2: Map DTO + LLM result -> Entity
                Lemma lemma = lemmaMapper.toEntity(request, llmResult);
                lemma.setSource(Source.USER_ENTERED);
                lemma.setReviewStatus(llmResult.fullySuccessful()
                    ? ReviewStatus.PENDING
                    : ReviewStatus.NEEDS_CORRECTION);

                // Step 3: Persist with cascading inflections
                Lemma saved = lemmaRepository.save(lemma);

                // Step 4: Return response DTO
                return lemmaMapper.toResponseDTO(saved);
            });
    }
}
```

### Pattern 2: DTO Validation Groups for Create/Update
**What:** Use marker interfaces to apply different validation rules for create vs update operations
**When to use:** When same DTO is reused for multiple CRUD operations

**Example:**
```java
// Source: Spring Boot validation groups best practices 2026
// Marker interfaces
public interface OnCreate {}
public interface OnUpdate {}

// Request DTO with context-specific validation
public record UpdateLemmaRequestDTO(
    @NotNull(groups = OnUpdate.class)
    @Null(groups = OnCreate.class)
    Long id,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    @Size(min = 1, max = 100)
    String text,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    @Size(min = 1, max = 200)
    String translation,

    @Size(max = 5000)
    String notes
) {}

// Controller usage
@PutMapping("/{id}")
public ResponseEntity<LemmaResponseDTO> update(
    @PathVariable Long id,
    @Validated(OnUpdate.class) @RequestBody UpdateLemmaRequestDTO request
) {
    return ResponseEntity.ok(vocabularyService.updateVocabulary(id, request));
}
```

### Pattern 3: Global Exception Handling with @RestControllerAdvice
**What:** Centralized exception handling for consistent error responses
**When to use:** All REST APIs to avoid scattered exception handling

**Example:**
```java
// Source: Spring Boot error handling best practices 2026
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setTitle("Resource Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
```

### Pattern 4: Pagination and Filtering with Pageable
**What:** Spring Data's Pageable interface for pagination, sorting, and filtering
**When to use:** Browse/search endpoints returning large result sets

**Example:**
```java
// Source: Spring Boot pagination best practices 2026
// Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    Page<Lemma> findBySourceAndPartOfSpeech(
        Source source,
        PartOfSpeech partOfSpeech,
        Pageable pageable
    );
}

// Service
public Page<LemmaResponseDTO> searchVocabulary(
    Source source,
    PartOfSpeech partOfSpeech,
    int page,
    int size,
    String sortBy
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
    return lemmaRepository
        .findBySourceAndPartOfSpeech(source, partOfSpeech, pageable)
        .map(lemmaMapper::toResponseDTO);
}

// Controller
@GetMapping
public ResponseEntity<Page<LemmaResponseDTO>> search(
    @RequestParam(required = false) Source source,
    @RequestParam(required = false) PartOfSpeech partOfSpeech,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "text") String sort
) {
    return ResponseEntity.ok(vocabularyService.searchVocabulary(source, partOfSpeech, page, size, sort));
}
```

### Pattern 5: MapStruct DTO Mapping
**What:** Type-safe, compile-time DTO-entity mapping with custom logic
**When to use:** All DTO-entity conversions to avoid manual mapping boilerplate

**Example:**
```java
// Source: MapStruct Spring Boot 3 best practices 2026
@Mapper(componentModel = "spring")
public interface LemmaMapper {

    // Entity to Response DTO
    @Mapping(target = "inflectionCount", expression = "java(lemma.getInflections().size())")
    LemmaResponseDTO toResponseDTO(Lemma lemma);

    // Request DTO + LLM Result to Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "llmResult.lemmaDetection.lemma")
    @Mapping(target = "translation", source = "request.translation")
    @Mapping(target = "partOfSpeech", source = "llmResult.metadata.partOfSpeech")
    @Mapping(target = "category", source = "llmResult.metadata.category")
    @Mapping(target = "difficultyLevel", source = "llmResult.metadata.difficultyLevel")
    @Mapping(target = "inflections", source = "llmResult.inflections", qualifiedByName = "mapInflections")
    Lemma toEntity(CreateLemmaRequestDTO request, LlmProcessingResult llmResult);

    @Named("mapInflections")
    default List<Inflection> mapInflections(InflectionSet inflectionSet) {
        if (inflectionSet == null) return List.of();
        return inflectionSet.inflections().stream()
            .map(entry -> {
                Inflection inf = new Inflection();
                inf.setForm(entry.text());
                inf.setGrammaticalInfo(entry.grammaticalTags());
                return inf;
            })
            .toList();
    }
}
```

### Anti-Patterns to Avoid
- **Returning entities from REST endpoints:** Exposes internal structure, risks sensitive data leakage, causes Jackson infinite recursion with bidirectional relationships
- **@Transactional on private methods:** Spring AOP proxies don't intercept private methods - transaction won't work
- **N+1 query problem:** Lazy-loading inflections in loop causes N+1 queries - use JOIN FETCH queries
- **Reusing same DTO for PUT and PATCH:** Blurs replace vs modify semantics, leads to confusing validation logic
- **Manual dependency instantiation:** Don't use `new` for services - breaks Spring DI and testing

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| DTO-entity mapping | Manual setters for 20+ fields | MapStruct @Mapper | Code generation at compile-time, type-safe, handles nested objects, no runtime reflection overhead |
| Pagination logic | Custom LIMIT/OFFSET SQL | Spring Data Pageable | Handles page calculation, sorting, total count queries, integrates with repositories seamlessly |
| Input validation | Manual if-else chains | Jakarta Bean Validation annotations | Declarative, standardized, integrates with Spring MVC error handling, supports custom validators |
| JSON bidirectional relationships | Custom serialization logic | Jackson @JsonManagedReference/@JsonBackReference | Solves infinite recursion, battle-tested, zero custom code |
| Global exception handling | Try-catch in every controller method | @RestControllerAdvice | Centralizes error handling, consistent error format, reduces boilerplate by 90% |
| Full-text search | LIKE '%term%' SQL | PGroonga &@~ operator (already configured) | Handles Cyrillic properly, full-text index performance, linguistic features |

**Key insight:** Spring Boot 3 and Spring Data JPA provide production-ready solutions for 95% of CRUD patterns. Custom implementations introduce bugs (edge cases, concurrency, performance) that framework authors already solved. MapStruct eliminates mapping boilerplate while maintaining type safety at compile time.

## Common Pitfalls

### Pitfall 1: Jackson Infinite Recursion with Bidirectional Relationships
**What goes wrong:** Returning Lemma entity with inflections list causes StackOverflowError during JSON serialization - Jackson follows Lemma -> Inflection -> Lemma -> Inflection infinitely
**Why it happens:** Bidirectional @OneToMany/@ManyToOne relationships create circular references that Jackson's default serializer can't handle
**How to avoid:**
  1. **PRIMARY SOLUTION:** Use DTOs instead of entities in API responses (enforces DTO-entity separation for security)
  2. **FALLBACK:** If entities must be serialized, use @JsonManagedReference (parent side) and @JsonBackReference (child side)
**Warning signs:** StackOverflowError in JSON serialization, "Infinite recursion" in logs, REST endpoint returns 500 error

### Pitfall 2: N+1 Query Problem with Lazy Loading
**What goes wrong:** Fetching all lemmas then accessing inflections in loop executes 1 query for lemmas + N queries for each lemma's inflections (performance death with 1000+ lemmas)
**Why it happens:** Default FetchType.LAZY on @OneToMany means inflections aren't loaded until accessed, triggering separate query per lemma
**How to avoid:** Use JOIN FETCH in JPQL queries or repository methods with @Query("SELECT DISTINCT l FROM Lemma l LEFT JOIN FETCH l.inflections")
**Warning signs:** Logs show hundreds of SELECT queries for single API call, slow response times (>2 seconds for 100 records), Hibernate SQL logs reveal pattern

**Code example:**
```java
// BAD: N+1 queries
List<Lemma> lemmas = lemmaRepository.findAll();
for (Lemma lemma : lemmas) {
    lemma.getInflections(); // Triggers separate query per lemma
}

// GOOD: Single query with JOIN FETCH
@Query("SELECT DISTINCT l FROM Lemma l LEFT JOIN FETCH l.inflections")
List<Lemma> findAllWithInflections();
```

### Pitfall 3: Blurring PUT vs PATCH Semantics
**What goes wrong:** Using same update method/DTO for both replace (PUT) and modify (PATCH) operations causes confusion about which fields are intentionally null vs missing from payload
**Why it happens:** Developers treat PUT as "update whatever fields are present" instead of "replace entire resource"
**How to avoid:**
  1. Separate endpoints: PUT /lemmas/{id} for full replacement, PATCH /lemmas/{id} for partial updates
  2. Separate DTOs: UpdateLemmaRequestDTO (all fields required) vs PatchLemmaRequestDTO (all fields optional)
  3. Document null vs absent distinction for PATCH (use Optional<T> or custom null-handling)
**Warning signs:** "Why did my field get set to null?" bug reports, validation logic with confusing nullable fields, lost updates in production

### Pitfall 4: Missing @Transactional Boundaries on Service Methods
**What goes wrong:** Service method creates Lemma entity, adds inflections via helper method, saves - but inflections aren't persisted or partial state saved on error
**Why it happens:** Without @Transactional, each repository call is its own transaction (auto-commit), no atomic guarantee across multiple operations
**How to avoid:** Mark service layer methods with @Transactional (read-only = false for write operations), let Spring manage transaction boundaries
**Warning signs:** Orphaned records, partial updates on errors, cascade operations don't work, foreign key violations

**Code example:**
```java
// BAD: No transaction boundary
public Lemma createLemma(CreateLemmaRequestDTO request) {
    Lemma lemma = new Lemma();
    lemma.setText(request.text());
    Lemma saved = lemmaRepository.save(lemma); // Transaction 1

    for (InflectionEntry entry : request.inflections()) {
        Inflection inf = new Inflection();
        inf.setLemma(saved);
        inflectionRepository.save(inf); // Transaction 2, 3, 4...
        // If one fails, previous succeed (partial state!)
    }
    return saved;
}

// GOOD: Single transaction
@Transactional
public Lemma createLemma(CreateLemmaRequestDTO request) {
    Lemma lemma = new Lemma();
    lemma.setText(request.text());
    for (InflectionEntry entry : request.inflections()) {
        Inflection inf = new Inflection();
        lemma.addInflection(inf); // Use bidirectional helper
    }
    return lemmaRepository.save(lemma); // Single transaction, cascade saves inflections
}
```

### Pitfall 5: Exposing Entities in API Responses
**What goes wrong:** Controller returns Lemma entity directly - exposes internal DB structure, risks sensitive data leakage, makes API contract fragile (DB schema changes break clients)
**Why it happens:** Seems faster to skip DTO mapping, developers trust "nothing sensitive in entities"
**How to avoid:** ALWAYS use DTOs for API responses - use MapStruct to eliminate mapping boilerplate, enforce DTO layer in code review
**Warning signs:** API responses contain audit fields (createdAt, updatedAt), internal IDs, or worse - password hashes, tokens; API breaks when DB schema changes; Jackson infinite recursion errors

### Pitfall 6: Ignoring Validation Groups for Create/Update
**What goes wrong:** Same DTO for create and update leads to awkward validation (ID should be null on create, required on update) - solved with if-else logic in service layer (messy)
**Why it happens:** Developers don't know about validation groups, or think reusing DTO is "cleaner"
**How to avoid:** Use validation groups (OnCreate.class, OnUpdate.class) with @Validated annotation in controller
**Warning signs:** Service layer has validation logic (should be at controller boundary), confusing @NotNull/@Nullable combinations, "ID shouldn't be in create request" bug reports

## Code Examples

Verified patterns from official sources and project context:

### Example 1: Complete CRUD Service Layer
```java
// Source: Spring Boot service layer pattern 2026
@Service
@Transactional(readOnly = true)
public class VocabularyService {
    private final LemmaRepository lemmaRepository;
    private final LlmOrchestrationService llmOrchestrationService;
    private final LemmaMapper lemmaMapper;

    public VocabularyService(
        LemmaRepository lemmaRepository,
        LlmOrchestrationService llmOrchestrationService,
        LemmaMapper lemmaMapper
    ) {
        this.lemmaRepository = lemmaRepository;
        this.llmOrchestrationService = llmOrchestrationService;
        this.lemmaMapper = lemmaMapper;
    }

    @Transactional
    public CompletableFuture<LemmaResponseDTO> createVocabulary(CreateLemmaRequestDTO request) {
        // Integrate LLM orchestration from Phase 2
        return llmOrchestrationService.processNewWord(request.wordForm())
            .thenApply(llmResult -> {
                Lemma lemma = lemmaMapper.toEntity(request, llmResult);
                lemma.setSource(Source.USER_ENTERED);
                lemma.setReviewStatus(llmResult.fullySuccessful()
                    ? ReviewStatus.PENDING
                    : ReviewStatus.NEEDS_CORRECTION);

                Lemma saved = lemmaRepository.save(lemma);
                return lemmaMapper.toResponseDTO(saved);
            });
    }

    public LemmaDetailDTO getVocabularyById(Long id) {
        Lemma lemma = lemmaRepository.findByIdWithInflections(id)
            .orElseThrow(() -> new EntityNotFoundException("Lemma not found: " + id));
        return lemmaMapper.toDetailDTO(lemma);
    }

    public Page<LemmaResponseDTO> browseVocabulary(Pageable pageable) {
        return lemmaRepository.findAll(pageable)
            .map(lemmaMapper::toResponseDTO);
    }

    @Transactional
    public LemmaResponseDTO updateVocabulary(Long id, UpdateLemmaRequestDTO request) {
        Lemma lemma = lemmaRepository.findByIdWithInflections(id)
            .orElseThrow(() -> new EntityNotFoundException("Lemma not found: " + id));

        lemmaMapper.updateEntity(request, lemma);
        lemma.setReviewStatus(ReviewStatus.PENDING); // Re-review after edit

        Lemma updated = lemmaRepository.save(lemma);
        return lemmaMapper.toResponseDTO(updated);
    }

    @Transactional
    public void deleteVocabulary(Long id) {
        if (!lemmaRepository.existsById(id)) {
            throw new EntityNotFoundException("Lemma not found: " + id);
        }
        lemmaRepository.deleteById(id); // Cascade deletes inflections
    }
}
```

### Example 2: REST Controller with Validation Groups
```java
// Source: Spring Boot REST API best practices 2026
@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {
    private final VocabularyService vocabularyService;

    @PostMapping
    public CompletableFuture<ResponseEntity<LemmaResponseDTO>> create(
        @Validated(OnCreate.class) @RequestBody CreateLemmaRequestDTO request
    ) {
        return vocabularyService.createVocabulary(request)
            .thenApply(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LemmaDetailDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vocabularyService.getVocabularyById(id));
    }

    @GetMapping
    public ResponseEntity<Page<LemmaResponseDTO>> browse(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "text") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(vocabularyService.browseVocabulary(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LemmaResponseDTO> update(
        @PathVariable Long id,
        @Validated(OnUpdate.class) @RequestBody UpdateLemmaRequestDTO request
    ) {
        return ResponseEntity.ok(vocabularyService.updateVocabulary(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vocabularyService.deleteVocabulary(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Example 3: PGroonga Cyrillic Full-Text Search
```java
// Source: Existing LemmaRepository + Phase 1 PGroonga configuration
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    // PGroonga full-text search for Bulgarian Cyrillic
    @Query(value = """
        SELECT * FROM lemmas
        WHERE text &@~ :searchQuery
        ORDER BY pgroonga_score(tableoid, ctid) DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Lemma> searchByText(@Param("searchQuery") String searchQuery);

    // Filtering with pagination
    Page<Lemma> findBySourceAndPartOfSpeech(
        Source source,
        PartOfSpeech partOfSpeech,
        Pageable pageable
    );

    Page<Lemma> findByDifficultyLevel(DifficultyLevel level, Pageable pageable);

    // JOIN FETCH to avoid N+1
    @Query("SELECT DISTINCT l FROM Lemma l LEFT JOIN FETCH l.inflections WHERE l.id = :id")
    Optional<Lemma> findByIdWithInflections(@Param("id") Long id);
}
```

### Example 4: DTO Examples with Validation
```java
// Source: Spring Boot DTO validation best practices 2026

// Marker interfaces for validation groups
public interface OnCreate {}
public interface OnUpdate {}

// Create request DTO
public record CreateLemmaRequestDTO(
    @NotBlank(groups = OnCreate.class)
    @Size(min = 1, max = 100)
    String wordForm,  // User-entered Bulgarian word (any form)

    @NotBlank(groups = OnCreate.class)
    @Size(min = 1, max = 200)
    String translation,

    @Size(max = 5000)
    String notes
) {}

// Update request DTO
public record UpdateLemmaRequestDTO(
    @NotNull(groups = OnUpdate.class)
    Long id,

    @NotBlank(groups = OnUpdate.class)
    @Size(min = 1, max = 100)
    String text,  // Canonical lemma text

    @NotBlank(groups = OnUpdate.class)
    @Size(min = 1, max = 200)
    String translation,

    @Size(max = 5000)
    String notes,

    List<InflectionUpdateDTO> inflections
) {}

// Response DTO (summary for lists)
public record LemmaResponseDTO(
    Long id,
    String text,
    String translation,
    PartOfSpeech partOfSpeech,
    String category,
    DifficultyLevel difficultyLevel,
    Source source,
    ReviewStatus reviewStatus,
    int inflectionCount,
    LocalDateTime createdAt
) {}

// Detail DTO (includes inflections)
public record LemmaDetailDTO(
    Long id,
    String text,
    String translation,
    String notes,
    PartOfSpeech partOfSpeech,
    String category,
    DifficultyLevel difficultyLevel,
    Source source,
    ReviewStatus reviewStatus,
    List<InflectionDTO> inflections,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

public record InflectionDTO(
    Long id,
    String form,
    String grammaticalInfo
) {}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| @ControllerAdvice with custom POJOs | @RestControllerAdvice with ProblemDetail (RFC 7807) | Spring Boot 3.0 (Nov 2022) | Standardized error format, auto-enabled by default |
| ModelMapper for DTO mapping | MapStruct compile-time generation | MapStruct 1.5+ (2022) | Zero reflection, compile-time safety, 5-10x faster |
| javax.validation.* | jakarta.validation.* (Jakarta EE 10) | Spring Boot 3.0 (Nov 2022) | Namespace change (javax -> jakarta), must update imports |
| Separate @Transactional(readOnly=true) on class + @Transactional on write methods | Single @Transactional(readOnly=true) on class, override on write methods | Spring Framework 5.3+ | Cleaner, read-only by default for query methods |
| HATEOAS mandatory for REST maturity | Pragmatic REST without HATEOAS for internal APIs | 2024-2026 trend | HATEOAS adds complexity, use only if client discovery needed |

**Deprecated/outdated:**
- javax.persistence.* annotations: Replaced by jakarta.persistence.* in Spring Boot 3.0+ (Jakarta EE transition)
- @JsonIgnoreProperties for bidirectional relationships: Still works, but @JsonManagedReference/@JsonBackReference is more explicit
- Manual Pageable construction: Use PageRequest.of() instead of new PageRequest() (deprecated in Spring Data 2.0)

## Open Questions

1. **Review workflow automation**
   - What we know: ReviewStatus enum exists (PENDING, REVIEWED, NEEDS_CORRECTION), review_status field in database
   - What's unclear: Should Phase 4 include manual review endpoints (approve/reject), or defer to Phase 8?
   - Recommendation: Implement basic review status updates (PATCH /api/vocabulary/{id}/review-status) in Phase 4 for manual correction workflow, defer automated validation to Phase 8

2. **Multi-word lemma handling**
   - What we know: Requirement VOCAB-02 requires multi-word lemma support (e.g., "казвам се", "искам да")
   - What's unclear: Should LLM orchestration auto-detect multi-word lemmas, or require explicit user flag?
   - Recommendation: Allow user to explicitly enter multi-word lemmas (bypass LLM detection for these cases), add isMultiWord flag or detect spaces in wordForm input

3. **Category taxonomy**
   - What we know: category field is free-text VARCHAR(50), LLM generates suggestions
   - What's unclear: Should categories be constrained to predefined list (food, travel, etc.) or remain free-form?
   - Recommendation: Keep free-form for Phase 4 (LLM flexibility), add category management UI in Phase 5, potentially normalize to enum in Phase 8

4. **Reference vocabulary editability**
   - What we know: System-seeded vocabulary has source=SYSTEM_SEED, distinguished from user entries
   - What's unclear: Can users edit/delete system-seeded vocabulary, or is it read-only?
   - Recommendation: Make system-seeded vocabulary read-only (prevent deletion), allow editing only for correction workflow with review status tracking

## Sources

### Primary (HIGH confidence)
- Existing project code: backend/src/main/java/com/vocab/bulgarian/ (domain entities, repositories, LLM services already implemented in Phases 1-3)
- Database schema: backend/src/main/resources/db/migration/V1__create_schema.sql and V2__seed_reference_data.sql
- Project pom.xml: Spring Boot 3.4.2, Spring Data JPA, Jakarta Validation already configured
- Roadmap: .planning/ROADMAP.md - Phase 4 requirements and success criteria

### Secondary (MEDIUM confidence)
- [Spring Boot CrudRepository Guide 2026](https://thelinuxcode.com/spring-boot-crudrepository-with-example-a-deep-practical-guide-for-2026/) - CRUD patterns
- [Service Layer Pattern in Spring Boot](https://foojay.io/today/service-layer-pattern-in-java-with-spring-boot/) - 3-tier architecture
- [Spring Boot DTO Validation Complete Guide](https://medium.com/@sibinraziya/spring-boot-dto-validation-complete-guide-to-custom-nested-validation-7119c36bd66d) - Validation patterns
- [Entity To DTO Conversion for Spring REST API - Baeldung](https://www.baeldung.com/entity-to-and-from-dto-for-a-java-spring-application) - DTO separation
- [MapStruct Spring Boot Integration](https://blog.nashtechglobal.com/mapstruct-and-spring-boot-integration-a-quick-guide/) - Mapper setup
- [Spring Boot Validation Groups Guide](https://codefarm0.medium.com/mastering-validation-groups-in-spring-boot-a-practical-guide-with-real-world-scenarios-internals-bd32a5f04198) - OnCreate/OnUpdate patterns
- [Spring Boot Global Exception Handling](https://simplifiedlearningblog.com/spring-boot-global-exception-handling-problem-detail/) - @RestControllerAdvice with ProblemDetail
- [REST API Pagination in Spring - Baeldung](https://www.baeldung.com/rest-api-pagination-in-spring) - Pageable patterns
- [Spring Boot CRUD Updates Guide 2026](https://thelinuxcode.com/spring-boot-crud-updates-that-dont-rot-put-patch-validation-concurrency-and-tests-2026/) - PUT vs PATCH semantics
- [N+1 Problem in Hibernate and Spring Data JPA - Baeldung](https://www.baeldung.com/spring-hibernate-n1-problem) - JOIN FETCH solutions
- [Jackson Bidirectional Relationships - Baeldung](https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion) - Infinite recursion fixes

### Tertiary (LOW confidence)
- [5 Subtle Spring Boot Anti-Patterns](https://medium.com/@ujjawalr/5-subtle-spring-boot-anti-patterns-beginners-accidentally-use-e005375326ed) - Common mistakes (needs verification in official docs)
- [Spring HATEOAS Best Practices](https://devot.team/blog/spring-hateoas) - HATEOAS patterns (optional for this phase)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Existing project dependencies verified in pom.xml, Spring Boot 3.4.2 with Spring Data JPA
- Architecture patterns: HIGH - Verified via official Spring documentation and Baeldung guides, service layer pattern is industry standard
- DTO mapping: MEDIUM-HIGH - MapStruct is widely adopted, but project doesn't have it yet (needs addition to pom.xml)
- Pitfalls: HIGH - Sourced from official Spring/Baeldung documentation on N+1 queries, Jackson infinite recursion, transaction management
- Review workflow: MEDIUM - ReviewStatus enum exists, but implementation details need clarification during planning

**Research date:** 2026-02-16
**Valid until:** 30 days (Spring Boot stable, patterns mature)
