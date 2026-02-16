# Phase 1: Foundation & Data Model - Research

**Researched:** 2026-02-15
**Domain:** Spring Boot 3.x + PostgreSQL + JPA + Docker Compose
**Confidence:** HIGH

## Summary

Phase 1 establishes the database foundation for a Bulgarian vocabulary learning application using Spring Boot 3.x with Spring Data JPA, PostgreSQL with PGroonga extension for Cyrillic full-text search, and Flyway for database migrations. The research confirms this is a well-trodden path with mature tooling and established patterns.

The core technical challenge is modeling Bulgarian morphology correctly: a Lemma entity (dictionary headword like "говоря" - 1st person singular) with a one-to-many relationship to Inflection entities (all conjugated/declined forms). User decisions mandate DTOs for API separation, lazy loading for performance, and forward-only Flyway migrations with immutable seed data.

The MacBook ↔ Mac Studio development workflow requires exposing Ollama on port 11434 via network settings and firewall configuration—a straightforward network setup, not a containerization problem.

**Primary recommendation:** Use Spring Boot 3.4+ with built-in Docker Compose support, implement bidirectional @OneToMany/@ManyToOne relationships with lazy loading and orphanRemoval for parent-child semantics, seed reference vocabulary via Flyway V2 migration, and configure PGroonga in V1 migration for Cyrillic search. Avoid unidirectional @OneToMany (causes extra join tables), avoid CascadeType.REMOVE without orphanRemoval (doesn't clean up detached children), and never expose JPA entities directly through REST APIs.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Migration strategy:**
- **Versioning:** Strict chronological (V1, V2, V3...) regardless of migration type
- **Reference data seeding:** Flyway migration (V2__seed_reference_data.sql) — versioned, automatic on fresh DB
- **Seed updates:** Immutable seed (V2 seeds once, updates are new migrations like V5__update_pronouns.sql)
- **Rollback approach:** Forward-only migrations — fixes go in new migrations, no undo scripts

**Entity design patterns:**
- **Domain model richness:** Hybrid approach — critical invariants in entities (e.g., Lemma can't have null text), complex workflows in services
- **Validation strategy:** Both layers — JPA annotations (@NotNull, @Size, @Column) + Jakarta Bean Validation (@Valid in controllers) for defense in depth
- **API design:** DTOs (Data Transfer Objects) — separate request/response classes, entities not exposed directly to API
- **Relationship loading:** Lazy loading for Lemma ↔ Inflection — fetch inflections only when accessed, better performance for list queries

### Claude's Discretion

- Infrastructure setup (Docker Compose orchestration, MacBook ↔ Mac Studio networking, environment variable management)
- Search capabilities (PGroonga configuration depth, indexing strategy for Cyrillic, initial search features)
- Specific JPA cascade rules and orphan removal behavior
- Exact database constraints and indexes beyond core relationships
- Development tooling and IDE configuration

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.

</user_constraints>

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.4+ (latest: 3.5.3) | Application framework | Industry standard for Java microservices, mature ecosystem, production-ready defaults |
| Spring Data JPA | (via Spring Boot BOM) | Data access abstraction | Eliminates repository boilerplate, automatic query derivation, declarative transactions |
| PostgreSQL | 16+ | Relational database | Rock-solid ACID compliance, excellent JSON support, rich extension ecosystem |
| PGroonga | 4.0.5+ | Full-text search extension | Only PostgreSQL extension supporting all languages including Cyrillic with zero ETL |
| Flyway | (via Spring Boot BOM) | Database migrations | Version control for database schema, automatic migration on startup, team collaboration |
| Hibernate Validator | (via Spring Boot BOM) | Bean validation implementation | Jakarta Bean Validation 3.0 reference implementation, integrates with Spring MVC |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Docker Compose | 2.x+ | Container orchestration | Development environment setup (PostgreSQL + PGroonga in container) |
| Spring Boot Docker Compose Module | (via Spring Boot) | Auto-start containers | Spring Boot 3.1+ feature—automatically manages docker compose up/down during development |
| MapStruct | 1.6+ | Entity ↔ DTO mapping | Optional but recommended for large projects with complex DTOs (alternative: manual mapping) |
| Testcontainers | 1.19+ | Integration testing | Spin up real PostgreSQL container for tests (deferred to later phase, mentioned for completeness) |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| PostgreSQL + PGroonga | Elasticsearch | PGroonga simpler for single-language focus (Bulgarian), no separate cluster needed, direct SQL queries |
| Flyway | Liquibase | Flyway has simpler SQL-first approach, better for teams familiar with SQL, no XML overhead |
| Spring Data JPA | jOOQ or JDBC Template | JPA better for rich domain models with relationships, jOOQ better for complex queries or read-heavy workloads |
| Docker Compose | Kubernetes | Docker Compose sufficient for local development, Kubernetes overkill for single-developer setup |

**Installation:**

Maven `pom.xml`:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.2</version> <!-- or latest 3.x -->
</parent>

<dependencies>
    <!-- Spring Boot JPA Starter (includes Spring Data JPA, Hibernate, Jakarta Persistence API) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Flyway Database Migrations -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- Flyway PostgreSQL-specific support -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- Bean Validation (included in spring-boot-starter-web, but explicit for clarity) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Docker Compose support (Spring Boot 3.1+) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-docker-compose</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## Architecture Patterns

### Recommended Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/vocab/bulgarian/
│   │       ├── domain/              # JPA entities
│   │       │   ├── Lemma.java
│   │       │   ├── Inflection.java
│   │       │   └── enums/           # Part of speech, difficulty, etc.
│   │       ├── repository/          # Spring Data JPA repositories
│   │       │   ├── LemmaRepository.java
│   │       │   └── InflectionRepository.java
│   │       ├── dto/                 # Request/Response DTOs
│   │       │   ├── request/
│   │       │   │   └── CreateLemmaRequest.java
│   │       │   └── response/
│   │       │       ├── LemmaResponse.java
│   │       │       └── LemmaDetailResponse.java
│   │       ├── mapper/              # DTO ↔ Entity mapping (optional MapStruct)
│   │       │   └── LemmaMapper.java
│   │       ├── service/             # Business logic
│   │       │   └── LemmaService.java
│   │       └── web/                 # REST controllers (deferred to Phase 5)
│   └── resources/
│       ├── db/migration/            # Flyway migrations (V1__, V2__, etc.)
│       │   ├── V1__create_schema.sql
│       │   └── V2__seed_reference_data.sql
│       ├── application.yml          # Spring Boot configuration
│       └── compose.yaml             # Docker Compose (Spring Boot 3.1+ auto-detects)
└── test/
    └── java/                        # Integration tests (deferred)
```

**Key principles:**
- **Domain layer** (entities) knows nothing about DTOs or web layer
- **Service layer** owns DTO ↔ Entity mapping logic
- **Controller layer** (Phase 5) only sees DTOs, never entities
- **Flyway migrations** in `src/main/resources/db/migration/` with strict V1, V2, V3 versioning

### Pattern 1: Bidirectional One-to-Many with Lazy Loading

**What:** Parent (Lemma) has collection of children (Inflections), child has reference to parent. Lazy loading defers child collection loading until accessed.

**When to use:** Parent-child relationships where you frequently query parents without children (e.g., list all lemmas without inflections for browse view).

**Example:**

```java
@Entity
@Table(name = "lemmas")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String text;  // e.g., "говоря" (1st person singular)

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;  // USER_ENTERED or SYSTEM_SEED

    // Bidirectional relationship: one lemma has many inflections
    // mappedBy = "lemma" means Inflection is the owning side (has foreign key)
    // fetch = LAZY means inflections loaded only when accessed (default, but explicit)
    // cascade = ALL means persist/merge/remove operations cascade to inflections
    // orphanRemoval = true means removing inflection from collection deletes it from DB
    @OneToMany(
        mappedBy = "lemma",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Inflection> inflections = new ArrayList<>();

    // Helper method to maintain bidirectional relationship
    public void addInflection(Inflection inflection) {
        inflections.add(inflection);
        inflection.setLemma(this);
    }

    public void removeInflection(Inflection inflection) {
        inflections.remove(inflection);
        inflection.setLemma(null);
    }

    // Getters/setters...
}

@Entity
@Table(name = "inflections")
public class Inflection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String form;  // e.g., "говориш" (2nd person singular)

    @Column(length = 50)
    private String grammaticalInfo;  // e.g., "2nd person singular present"

    // Owning side of relationship: many inflections belong to one lemma
    // fetch = LAZY means lemma loaded only when accessed (ALWAYS use LAZY for @ManyToOne)
    // @JoinColumn specifies foreign key column name
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    // Getters/setters...
}
```

**Source:** Based on [Spring Data JPA Best Practices](https://medium.com/javaguides/best-practices-for-spring-data-jpa-the-ultimate-guide-c2a84a4cd45e) and [Thorben Janssen's Best Practices for Many-To-One](https://thorben-janssen.com/best-practices-many-one-one-many-associations-mappings/)

### Pattern 2: DTO-First API Design

**What:** Controllers and services communicate via DTOs (records), not entities. Service layer owns mapping logic.

**When to use:** Always for REST APIs. Prevents accidental data leaks, decouples API contract from database schema.

**Example:**

```java
// Request DTO (data coming FROM client)
public record CreateLemmaRequest(
    @NotBlank String text,
    @NotBlank String translation,
    String notes
) {}

// Response DTO (data going TO client)
public record LemmaResponse(
    Long id,
    String text,
    String translation,
    String source,
    int inflectionCount  // Calculated field, not in entity
) {}

// Detailed response DTO (includes child entities)
public record LemmaDetailResponse(
    Long id,
    String text,
    String translation,
    String notes,
    List<InflectionResponse> inflections
) {}

public record InflectionResponse(
    String form,
    String grammaticalInfo
) {}

// Service layer owns mapping
@Service
@Transactional
public class LemmaService {
    private final LemmaRepository lemmaRepository;

    public LemmaResponse createLemma(CreateLemmaRequest request) {
        // Map DTO -> Entity
        Lemma lemma = new Lemma();
        lemma.setText(request.text());
        lemma.setTranslation(request.translation());
        lemma.setNotes(request.notes());
        lemma.setSource(Source.USER_ENTERED);

        Lemma saved = lemmaRepository.save(lemma);

        // Map Entity -> DTO
        return new LemmaResponse(
            saved.getId(),
            saved.getText(),
            saved.getTranslation(),
            saved.getSource().name(),
            saved.getInflections().size()  // Lazy collection accessed here
        );
    }

    public LemmaDetailResponse getLemmaDetail(Long id) {
        // Fetch with inflections using JOIN FETCH to avoid N+1 problem
        Lemma lemma = lemmaRepository.findByIdWithInflections(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lemma not found"));

        // Map Entity -> Detailed DTO
        List<InflectionResponse> inflectionDtos = lemma.getInflections().stream()
            .map(i -> new InflectionResponse(i.getForm(), i.getGrammaticalInfo()))
            .toList();

        return new LemmaDetailResponse(
            lemma.getId(),
            lemma.getText(),
            lemma.getTranslation(),
            lemma.getNotes(),
            inflectionDtos
        );
    }
}
```

**Source:** Based on [Spring Boot Best Practices: Use DTOs Instead of Entities](https://medium.com/javarevisited/spring-boot-best-practices-use-dtos-instead-of-entities-in-api-responses-e23fc69e45a4) and [Bell-SW DTO Guide](https://bell-sw.com/blog/ultimate-guide-to-using-dtos-with-spring-boot/)

### Pattern 3: Repository with Custom Query Methods

**What:** Spring Data JPA repositories extend JpaRepository and define custom queries via method naming or @Query annotation.

**When to use:** When derived query methods (findBy, existsBy) aren't sufficient or you need to optimize with JOIN FETCH.

**Example:**

```java
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    // Derived query method (Spring Data generates query from method name)
    List<Lemma> findBySourceOrderByTextAsc(Source source);

    // Custom query with JOIN FETCH to avoid N+1 problem
    @Query("SELECT l FROM Lemma l LEFT JOIN FETCH l.inflections WHERE l.id = :id")
    Optional<Lemma> findByIdWithInflections(@Param("id") Long id);

    // Full-text search using PGroonga (native query, Phase 1 sets up infrastructure)
    @Query(value = """
        SELECT * FROM lemmas
        WHERE text &@~ :searchQuery
        ORDER BY pgroonga_score(tableoid, ctid) DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Lemma> searchByText(@Param("searchQuery") String searchQuery);
}
```

**Source:** Based on [Spring Data JPA Repository Patterns](https://protsenko.dev/spring-data-jpa-best-practices-repositories-design-guide/) and [Mastering Repository Pattern](https://naveen-metta.medium.com/mastering-the-repository-pattern-with-spring-data-jpa-advanced-techniques-9f387e301b45)

### Pattern 4: Flyway Migration Versioning

**What:** Sequential version numbers (V1, V2, V3), descriptive names, immutable files, forward-only changes.

**When to use:** Always for database schema changes and reference data seeding.

**Example:**

```sql
-- V1__create_schema.sql
CREATE EXTENSION IF NOT EXISTS pgroonga;

CREATE TABLE lemmas (
    id BIGSERIAL PRIMARY KEY,
    text VARCHAR(100) NOT NULL,
    translation VARCHAR(200) NOT NULL,
    notes TEXT,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inflections (
    id BIGSERIAL PRIMARY KEY,
    lemma_id BIGINT NOT NULL REFERENCES lemmas(id) ON DELETE CASCADE,
    form VARCHAR(100) NOT NULL,
    grammatical_info VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- PGroonga index for Bulgarian Cyrillic full-text search
CREATE INDEX idx_lemmas_text_pgroonga ON lemmas USING pgroonga(text);

-- Standard indexes for foreign keys and queries
CREATE INDEX idx_inflections_lemma_id ON inflections(lemma_id);
CREATE INDEX idx_lemmas_source ON lemmas(source);
```

```sql
-- V2__seed_reference_data.sql
-- Seed interrogatives (immutable: never modify this file, create V5__update_pronouns.sql instead)
INSERT INTO lemmas (text, translation, source) VALUES
('кой', 'who', 'SYSTEM_SEED'),
('какво', 'what', 'SYSTEM_SEED'),
('кога', 'when', 'SYSTEM_SEED'),
('къде', 'where', 'SYSTEM_SEED'),
('защо', 'why', 'SYSTEM_SEED'),
('как', 'how', 'SYSTEM_SEED');

-- Seed pronouns
INSERT INTO lemmas (text, translation, source) VALUES
('аз', 'I', 'SYSTEM_SEED'),
('ти', 'you (informal)', 'SYSTEM_SEED'),
('той', 'he', 'SYSTEM_SEED'),
('тя', 'she', 'SYSTEM_SEED');
-- ... etc
```

**Naming convention:** `V{VERSION}__{DESCRIPTION}.sql`
- VERSION: Sequential integer (1, 2, 3, NOT 1.0, 1.1)
- DESCRIPTION: Snake_case description (e.g., create_schema, seed_reference_data)

**Source:** Based on [Flyway Naming Patterns Matter](https://www.red-gate.com/blog/database-devops/flyway-naming-patterns-matter) and [Flyway Versioned Migrations](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html)

### Anti-Patterns to Avoid

- **Unidirectional @OneToMany:** Creates extra join table, poor query performance. Use bidirectional @OneToMany/@ManyToOne instead. ([Source](https://thorben-janssen.com/best-practices-many-one-one-many-associations-mappings/))
- **Eager fetching by default:** Causes N+1 problems, loads unnecessary data. Use LAZY and explicit JOIN FETCH when needed. ([Source](https://www.baeldung.com/spring-hibernate-n1-problem))
- **Returning entities from controllers:** Leaks database schema, risks exposing sensitive fields. Use DTOs. ([Source](https://medium.com/javarevisited/spring-boot-best-practices-use-dtos-instead-of-entities-in-api-responses-e23fc69e45a4))
- **CascadeType.REMOVE without orphanRemoval:** Doesn't clean up orphaned children when removed from collection. Use orphanRemoval = true for parent-child relationships. ([Source](https://www.baeldung.com/jpa-cascade-remove-vs-orphanremoval))
- **Modifying existing Flyway migrations:** Breaks checksum validation. Create new migration for fixes. ([Source](https://www.red-gate.com/blog/database-devops/flyway-naming-patterns-matter))

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Database migrations | SQL scripts run manually or shell scripts | Flyway (or Liquibase) | Version tracking, automatic rollback detection, team collaboration, checksum validation |
| Repository CRUD boilerplate | DAO classes with JDBC | Spring Data JPA repositories | Auto-implements findById, save, delete, findAll; query derivation from method names |
| Entity ↔ DTO mapping | Manual field copying in service layer | MapStruct (or manual in small projects) | Compile-time code generation, type safety, maintainability at scale |
| Database connection pooling | Manual connection management | HikariCP (Spring Boot default) | Production-tested connection pooling, leak detection, performance metrics |
| Full-text search tokenization | Custom regex parsing for Cyrillic | PGroonga | Proper Unicode support, stemming, relevance scoring, production-tested for all languages |
| Bean validation | Manual null checks and regex in service layer | Jakarta Bean Validation (@NotNull, @Size, @Pattern) | Declarative, reusable, automatic integration with Spring MVC |

**Key insight:** Spring Boot provides production-ready defaults for infrastructure concerns (connection pooling, transaction management, validation). Reinventing these creates maintenance burden and subtle bugs (connection leaks, deadlocks, Unicode edge cases). Focus domain logic, not plumbing.

## Common Pitfalls

### Pitfall 1: N+1 Query Problem with Lazy Loading

**What goes wrong:** Fetching list of lemmas, then accessing inflections in a loop triggers one query per lemma (1 query for lemmas + N queries for inflections).

**Why it happens:** Lazy loading defers collection loading until accessed. Loop iteration triggers one SELECT per parent entity.

**How to avoid:**
- Use JOIN FETCH in repository method: `SELECT l FROM Lemma l LEFT JOIN FETCH l.inflections`
- Use @EntityGraph for declarative fetch strategy
- Consider DTO projections for read-only queries

**Warning signs:**
- Hibernate logs show dozens/hundreds of SELECT statements for single operation
- Performance degrades linearly with dataset size
- Database connection pool exhaustion under load

**Example fix:**

```java
// BAD: Triggers N+1 queries
List<Lemma> lemmas = lemmaRepository.findAll();
for (Lemma lemma : lemmas) {
    System.out.println(lemma.getInflections().size()); // SELECT per lemma!
}

// GOOD: Single query with JOIN FETCH
@Query("SELECT l FROM Lemma l LEFT JOIN FETCH l.inflections")
List<Lemma> findAllWithInflections();
```

**Source:** [N+1 Problem in Hibernate and Spring Data JPA](https://www.baeldung.com/spring-hibernate-n1-problem), [Understanding and Solving N+1 Select Problem](https://codefarm0.medium.com/understanding-and-solving-the-n-1-select-problem-in-jpa-907c940ad6d7)

### Pitfall 2: Missing Helper Methods in Bidirectional Relationships

**What goes wrong:** Adding inflection to lemma's collection without setting inflection's lemma field (or vice versa) causes inconsistent state and orphaned records.

**Why it happens:** JPA doesn't automatically synchronize both sides of bidirectional relationship.

**How to avoid:** Implement helper methods in parent entity that maintain both sides of relationship.

**Warning signs:**
- Inflections saved but lemma_id is null
- Collection contains child but child doesn't reference parent
- Orphaned records appear in database after deletes

**Example fix:**

```java
// BAD: Only sets one side
lemma.getInflections().add(inflection); // inflection.lemma is still null!

// GOOD: Helper method maintains both sides
public class Lemma {
    public void addInflection(Inflection inflection) {
        inflections.add(inflection);
        inflection.setLemma(this);  // Synchronize both sides
    }

    public void removeInflection(Inflection inflection) {
        inflections.remove(inflection);
        inflection.setLemma(null);  // Synchronize both sides
    }
}
```

**Source:** [Spring Data JPA Best Practices](https://medium.com/javaguides/best-practices-for-spring-data-jpa-the-ultimate-guide-c2a84a4cd45e), [Entity Relationships in Spring Data JPA](https://blog.stackademic.com/entity-relationships-in-spring-data-jpa-what-the-tutorials-dont-tell-you-a86977aafd5a)

### Pitfall 3: Exposing JPA Entities in REST API Responses

**What goes wrong:** Jackson serialization triggers lazy loading exceptions (LazyInitializationException), exposes internal database structure, risks leaking sensitive fields, creates tight coupling between API and persistence layer.

**Why it happens:** Entities designed for persistence, not API contracts. Lazy collections accessed outside transaction scope fail.

**How to avoid:**
- Always use DTOs for controller request/response
- Map entities to DTOs in service layer (inside transaction boundary)
- Keep DTOs simple (Java records work well)

**Warning signs:**
- `LazyInitializationException: could not initialize proxy`
- API response includes unexpected fields (timestamps, version columns)
- Changing database schema breaks API clients
- Circular reference errors in JSON serialization

**Example fix:**

```java
// BAD: Controller returns entity directly
@GetMapping("/{id}")
public Lemma getLemma(@PathVariable Long id) {
    return lemmaRepository.findById(id).orElseThrow();
    // Throws LazyInitializationException if inflections accessed outside transaction!
}

// GOOD: Controller returns DTO
@GetMapping("/{id}")
public LemmaResponse getLemma(@PathVariable Long id) {
    return lemmaService.getLemma(id);  // Service maps entity -> DTO inside transaction
}
```

**Source:** [Spring Boot Best Practices: Use DTOs](https://www.javaguides.net/2025/02/use-dtos-instead-of-entities-in-api.html), [What Is a DTO?](https://igventurelli.io/what-is-a-dto-and-why-you-shouldnt-return-your-entities-in-spring-boot/)

### Pitfall 4: Flyway Migration Checksum Failures After Manual Edits

**What goes wrong:** Modifying existing migration file (V1, V2, etc.) after it's been applied causes checksum validation error on next startup. Application fails to start.

**Why it happens:** Flyway stores checksums of applied migrations in `flyway_schema_history` table. File modification changes checksum, triggering validation error.

**How to avoid:**
- **Never modify applied migrations** (treat as immutable)
- Create new migration for fixes (V3__fix_lemma_constraints.sql)
- Use forward-only migration strategy (no undo scripts)
- Test migrations on fresh database before committing

**Warning signs:**
- `FlywayValidateException: Migration checksum mismatch`
- Application fails to start in CI/CD or teammate's environment
- Different checksum errors between environments

**Example fix:**

```sql
-- BAD: Editing V1__create_schema.sql after it's applied
-- ALTER TABLE lemmas ADD COLUMN new_field VARCHAR(50);  -- DON'T DO THIS!

-- GOOD: Create new migration
-- V3__add_new_field_to_lemmas.sql
ALTER TABLE lemmas ADD COLUMN new_field VARCHAR(50);
```

**Recovery (last resort):**
- Option 1: `flyway.repair()` to update checksums (loses validation safety)
- Option 2: Drop database and recreate from scratch (development only)
- Option 3: Manually update flyway_schema_history checksum (dangerous)

**Source:** [Flyway Naming Patterns Matter](https://www.red-gate.com/blog/database-devops/flyway-naming-patterns-matter), [Flyway Recommended Practices](https://documentation.red-gate.com/fd/recommended-practices-150700352.html)

### Pitfall 5: Docker Networking Confusion (MacBook → Mac Studio Ollama)

**What goes wrong:** Attempting to connect to Ollama running on Mac Studio using `localhost` or Docker internal networking fails.

**Why it happens:** Ollama runs on Mac Studio (separate physical machine), not in Docker. Need LAN networking, not Docker bridge network.

**How to avoid:**
- Run Ollama natively on Mac Studio (NOT in Docker on Mac—no GPU passthrough)
- Expose Ollama API on LAN: Settings → "Expose Ollama to the network"
- Allow port 11434 in macOS firewall on Mac Studio
- Connect from MacBook using Mac Studio's LAN IP (e.g., `http://192.168.1.28:11434`)
- Store Mac Studio IP in environment variable or Spring profile

**Warning signs:**
- Connection refused errors when calling Ollama API
- `localhost:11434` works on Mac Studio but not from MacBook
- Docker network isolation preventing LAN access

**Example configuration:**

```yaml
# application-dev.yml (MacBook development profile)
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://192.168.1.28:11434}  # Mac Studio LAN IP
```

**Testing connectivity:**
```bash
# From MacBook, verify Mac Studio Ollama is accessible
curl http://192.168.1.28:11434/api/tags
```

**Source:** [Turn Your Mac into an AI Server: Ollama and Remote Access](https://medium.com/@jsenick/turn-your-mac-into-an-ai-server-ollama-deepseek-and-secure-remote-access-aeae11d492e6), [On-Prem AI in Company Network](https://www.genexio.net/blog/2025/09/16/on-prem-ai-in-the-company-network-docker-portainer-node-red-on-raspberry-pi-llama-on-mac-studio-and-filemaker-odata-integration/)

## Code Examples

Verified patterns from official sources and expert articles:

### Bidirectional One-to-Many with Orphan Removal

```java
// Parent entity
@Entity
@Table(name = "lemmas")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String text;

    @NotNull
    @Column(nullable = false, length = 200)
    private String translation;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;

    // Bidirectional relationship with cascade and orphan removal
    @OneToMany(
        mappedBy = "lemma",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Inflection> inflections = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods to maintain bidirectional relationship
    public void addInflection(Inflection inflection) {
        inflections.add(inflection);
        inflection.setLemma(this);
    }

    public void removeInflection(Inflection inflection) {
        inflections.remove(inflection);
        inflection.setLemma(null);
    }

    // Getters and setters...
}

// Child entity
@Entity
@Table(name = "inflections")
public class Inflection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String form;

    @Column(name = "grammatical_info", length = 50)
    private String grammaticalInfo;

    // Many-to-one side (owning side with foreign key)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and setters...
}
```

**Source:** [Understanding cascade and orphanRemoval in JPA](https://medium.com/@samrat.alam/understanding-cascade-and-orphanremoval-in-jpa-the-definitive-guide-with-parent-child-design-1502ecc81fcd), [JPA CascadeType.REMOVE vs orphanRemoval](https://www.baeldung.com/jpa-cascade-remove-vs-orphanremoval)

### Spring Boot Docker Compose Integration

```yaml
# compose.yaml (Spring Boot 3.1+ auto-detects this file in project root)
services:
  postgres:
    image: groonga/pgroonga:latest  # PostgreSQL with PGroonga pre-installed
    container_name: bulgarian-vocab-db
    environment:
      POSTGRES_DB: bulgarian_vocab
      POSTGRES_USER: vocab_user
      POSTGRES_PASSWORD: vocab_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U vocab_user -d bulgarian_vocab"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres-data:
```

```yaml
# application.yml
spring:
  application:
    name: bulgarian-vocabulary

  datasource:
    url: jdbc:postgresql://localhost:5432/bulgarian_vocab
    username: vocab_user
    password: vocab_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles schema, Hibernate only validates
    show-sql: true  # Development only
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true

  docker:
    compose:
      enabled: true  # Auto-start containers on startup (Spring Boot 3.1+)
      file: compose.yaml
```

**Source:** [Docker Compose Support in Spring Boot](https://www.baeldung.com/docker-compose-support-spring-boot), [Spring Boot 3.4 Docker Compose Features](https://www.danvega.dev/blog/2025/09/17/spring-boot-3-features)

### Repository with JOIN FETCH to Avoid N+1

```java
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    // Derived query method
    List<Lemma> findBySourceOrderByTextAsc(Source source);

    boolean existsByTextIgnoreCase(String text);

    // Custom query with JOIN FETCH (solves N+1 problem)
    @Query("""
        SELECT l FROM Lemma l
        LEFT JOIN FETCH l.inflections
        WHERE l.id = :id
        """)
    Optional<Lemma> findByIdWithInflections(@Param("id") Long id);

    // Multiple entities with JOIN FETCH (returns distinct parents)
    @Query("""
        SELECT DISTINCT l FROM Lemma l
        LEFT JOIN FETCH l.inflections
        WHERE l.source = :source
        ORDER BY l.text ASC
        """)
    List<Lemma> findBySourceWithInflections(@Param("source") Source source);

    // PGroonga full-text search (native query)
    @Query(value = """
        SELECT * FROM lemmas
        WHERE text &@~ :searchQuery
        ORDER BY pgroonga_score(tableoid, ctid) DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Lemma> searchByText(@Param("searchQuery") String searchQuery);
}
```

**Source:** [JPA EntityGraphs: Solution to N+1 Query Problem](https://medium.com/geekculture/jpa-entitygraphs-a-solution-to-n-1-query-problem-e29c28abe5fb), [Fixing N+1 Query Problem with Fetch Joins](https://www.prometheanz.com/blog/jpa-n-plus-one-query-problem-solution)

### Bean Validation with DTOs

```java
// Request DTO with validation constraints
public record CreateLemmaRequest(
    @NotBlank(message = "Lemma text is required")
    @Size(min = 1, max = 100, message = "Lemma text must be between 1 and 100 characters")
    String text,

    @NotBlank(message = "Translation is required")
    @Size(max = 200, message = "Translation must not exceed 200 characters")
    String translation,

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    String notes
) {}

// Controller validates request with @Valid
@RestController
@RequestMapping("/api/lemmas")
public class LemmaController {
    private final LemmaService lemmaService;

    @PostMapping
    public ResponseEntity<LemmaResponse> createLemma(
            @Valid @RequestBody CreateLemmaRequest request) {  // @Valid triggers validation
        LemmaResponse response = lemmaService.createLemma(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

// Global exception handler for validation errors
@RestControllerAdvice
public class ValidationExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }
}
```

**Source:** [Guide to Field Validation with Jakarta Validation](https://agussyahrilmubarok.medium.com/guide-to-field-validation-with-jakarta-validation-in-spring-8c9eca68022e), [Java Bean Validation Basics](https://www.baeldung.com/java-validation)

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Spring Boot 2.x with javax.* packages | Spring Boot 3.x with jakarta.* packages | November 2022 | Namespace change (javax.persistence → jakarta.persistence), requires Java 17+ |
| Manual Docker container management | Spring Boot Docker Compose module | May 2023 (Spring Boot 3.1) | Auto-starts containers during development, eliminates manual `docker compose up` |
| Unidirectional @OneToMany | Bidirectional @OneToMany/@ManyToOne | Long-standing best practice | Avoids extra join tables, better query performance |
| FetchType.EAGER by default | FetchType.LAZY everywhere + explicit JOIN FETCH | Ongoing evolution | Prevents N+1 problems, explicit about data loading |
| Entities in API responses | DTOs (Java records preferred) | Java 14 introduced records (2020) | Decoupling, security, immutability, concise syntax |
| Liquibase XML migrations | Flyway SQL migrations (or Liquibase YAML) | Preference shift ~2018 | SQL-first approach more transparent, easier for SQL-fluent teams |
| Spring Data JPA 2.x | Spring Data JPA 3.x | Spring Data 2022.0.0 (November 2022) | Aligned with Spring Boot 3.x, Jakarta EE 9+ support |

**Deprecated/outdated:**
- **javax.persistence.\* imports:** Use jakarta.persistence.* (Spring Boot 3.x requires Jakarta EE 9+)
- **Hibernate 5.x:** Use Hibernate 6.x (bundled with Spring Boot 3.x, performance improvements, Jakarta migration)
- **FetchType.EAGER for @ManyToOne:** Default is still EAGER per JPA spec, but best practice is explicit LAZY
- **@EntityGraph without type:** Use `type = EntityGraph.EntityGraphType.FETCH` (FETCH treats unlisted as LAZY, LOAD respects defaults)
- **Flyway undo migrations:** Forward-only migrations preferred (undo scripts rarely work in production, create new migration for fixes)

## Open Questions

1. **PGroonga Index Tuning for Bulgarian**
   - What we know: PGroonga supports Cyrillic out-of-the-box, `&@~` operator for full-text search
   - What's unclear: Optimal index configuration for Bulgarian morphology (stemming, case-folding), performance with 5000+ lemmas
   - Recommendation: Start with default PGroonga index (`CREATE INDEX USING pgroonga(text)`), benchmark with realistic data, tune if needed (Phase 4 implementation)

2. **MapStruct vs Manual Mapping**
   - What we know: MapStruct generates compile-time mapping code, eliminates boilerplate
   - What's unclear: Overhead worth it for ~5 entity types? Learning curve for team?
   - Recommendation: Start with manual mapping (simple for Phase 1), evaluate MapStruct in Phase 4 when DTO complexity increases

3. **Hibernate Second-Level Cache**
   - What we know: Can reduce database queries for frequently accessed reference data (interrogatives, pronouns)
   - What's unclear: Complexity vs benefit for single-user application, cache invalidation strategy
   - Recommendation: Defer to Phase 8 (optimization phase), profile first to confirm need

4. **Docker Compose Lifecycle Management**
   - What we know: Spring Boot 3.1+ auto-starts containers via `spring-boot-docker-compose` dependency
   - What's unclear: Behavior when Mac Studio (Ollama) is unreachable, graceful degradation strategy
   - Recommendation: Accept startup failure if Ollama unreachable (fail-fast for development), implement circuit breaker in Phase 2 for resilience

## Sources

### Primary (HIGH confidence)

- **Spring Data JPA Official Documentation** - [Spring Data JPA Reference](https://docs.spring.vmware.com/spring-data-jpa-distribution/docs/3.0.13/reference/html/index.html)
- **Flyway Official Documentation** - [Versioned Migrations](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html)
- **Baeldung Tutorials** - [Docker Compose Support in Spring Boot](https://www.baeldung.com/docker-compose-support-spring-boot), [JPA CascadeType.REMOVE vs orphanRemoval](https://www.baeldung.com/jpa-cascade-remove-vs-orphanremoval), [N+1 Problem](https://www.baeldung.com/spring-hibernate-n1-problem)
- **PGroonga Official Site** - [PGroonga: Make PostgreSQL fast full text search](https://pgroonga.github.io/)
- **Jakarta Bean Validation Specification** - [Jakarta Bean Validation 3.0](https://jakarta.ee/specifications/bean-validation/3.0/)

### Secondary (MEDIUM confidence)

- **Spring Boot Release Notes** - [Spring Boot 3.4 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.4-Release-Notes)
- **Expert Blogs** - Thorben Janssen's [Best Practices for Many-To-One and One-To-Many](https://thorben-janssen.com/best-practices-many-one-one-many-associations-mappings/), Vlad Mihalcea's [Best Spring Data JpaRepository](https://vladmihalcea.com/best-spring-data-jparepository/)
- **Medium Articles (verified with official sources)** - [Spring Boot Best Practices: Use DTOs](https://medium.com/javarevisited/spring-boot-best-practices-use-dtos-instead-of-entities-in-api-responses-e23fc69e45a4), [Understanding cascade and orphanRemoval](https://medium.com/@samrat.alam/understanding-cascade-and-orphanremoval-in-jpa-the-definitive-guide-with-parent-child-design-1502ecc81fcd)
- **Maven Repository** - [Spring Boot Dependencies](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-dependencies)

### Tertiary (LOW confidence - community wisdom, needs validation in Phase 1 implementation)

- **Ollama Networking** - [Turn Your Mac into an AI Server: Ollama and Remote Access](https://medium.com/@jsenick/turn-your-mac-into-an-ai-server-ollama-deepseek-and-secure-remote-access-aeae11d492e6) (community guide, verify Mac Studio firewall settings during setup)
- **PGroonga Docker Setup** - [GitHub pgroonga/docker](https://github.com/pgroonga/docker) (official repo but minimal documentation, expect to reference PGroonga docs for index configuration)

## Metadata

**Confidence breakdown:**
- Standard stack: **HIGH** - Spring Boot + JPA + PostgreSQL is industry-standard Java stack, confirmed via official docs and Maven Central
- Architecture patterns: **HIGH** - Patterns sourced from official Spring Data JPA docs, Baeldung tutorials, and recognized experts (Thorben Janssen, Vlad Mihalcea)
- Pitfalls: **HIGH** - Common pitfalls documented extensively across multiple sources (N+1, DTO importance, Flyway immutability), verified in official docs
- PGroonga specifics: **MEDIUM** - Official documentation exists, Cyrillic support confirmed, but Bulgarian-specific tuning requires experimentation
- MacBook ↔ Mac Studio networking: **MEDIUM** - Ollama networking straightforward (LAN + firewall), but specific Mac Studio setup needs validation during Phase 1 implementation

**Research date:** 2026-02-15
**Valid until:** 2026-06-15 (4 months - Spring Boot ecosystem stable, minor version updates expected but patterns remain constant)

**Notes for planner:**
- User decisions are locked (see User Constraints section) - planner MUST respect these
- Infrastructure setup (Docker Compose, networking) is Claude's discretion - planner should provide specific implementation tasks
- PGroonga index configuration is Claude's discretion - planner should include experimentation/tuning task
- Phase 1 delivers database schema and entities only (no REST controllers - deferred to Phase 5)
