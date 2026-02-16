# Phase 1: Foundation & Data Model - Context

**Gathered:** 2026-02-15
**Status:** Ready for planning

<domain>
## Phase Boundary

Database foundation and development environment for Bulgarian vocabulary learning — establishing the correct PostgreSQL schema to handle lemmas and inflections, JPA entities with proper relationships, and developer workflow for MacBook ↔ Mac Studio Ollama connectivity.

This phase delivers: PostgreSQL database schema, JPA domain model, Spring Data repositories, development environment, and PGroonga search infrastructure.

</domain>

<decisions>
## Implementation Decisions

### Migration strategy
- **Versioning:** Strict chronological (V1, V2, V3...) regardless of migration type
- **Reference data seeding:** Flyway migration (V2__seed_reference_data.sql) — versioned, automatic on fresh DB
- **Seed updates:** Immutable seed (V2 seeds once, updates are new migrations like V5__update_pronouns.sql)
- **Rollback approach:** Forward-only migrations — fixes go in new migrations, no undo scripts

### Entity design patterns
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

</decisions>

<specifics>
## Specific Ideas

No specific requirements — standard Spring Boot + JPA + PostgreSQL patterns apply.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 01-foundation-and-data-model*
*Context gathered: 2026-02-15*
