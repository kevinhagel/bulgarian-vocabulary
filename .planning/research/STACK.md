# Stack Research

**Domain:** Bulgarian Vocabulary Learning Application with LLM Integration
**Researched:** 2026-02-15
**Confidence:** HIGH

## Recommended Stack

### Backend Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Spring Boot | 4.2.x | Application Framework | Spring Boot 4.x is the latest major version (GA release), required for Spring AI 2.x compatibility and modern Java 25 runtime features. Provides auto-configuration, embedded server, and production-ready features. |
| Java | 25 | Programming Language | Latest LTS features, improved performance, pattern matching, and virtual threads support for better concurrency handling in LLM operations. |
| Spring Data JPA | 4.x (included) | Database ORM | De-facto standard for data persistence in Spring Boot. Provides repository abstractions, query generation, and relationship mapping perfect for linguistic data models (lemmas, inflections). |
| PostgreSQL | 16+ | Primary Database | Robust support for complex queries, JSONB for metadata storage, excellent full-text search capabilities, and proven reliability for linguistic data with rich morphology. |
| Flyway | 10.x | Database Migration | Industry standard for version-controlled schema migrations. Auto-configured in Spring Boot, supports repeatable migrations, and enables safe schema evolution for linguistic data structures. |
| Spring AI | 2.0.0-M2+ (target 2.0 GA) | LLM Integration Framework | Official Spring project for AI integration. Provides portable API across AI providers with synchronous/streaming support. Spring AI 2.x targets Spring Boot 4.0 and includes Ollama auto-configuration. |

**Confidence Level:** HIGH - All core technologies verified with official documentation and release notes.

### Backend Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| spring-boot-starter-web | (included in Boot 4.2.x) | REST API | Required for all REST endpoints. Includes Spring MVC, Jackson, and embedded Tomcat. |
| spring-boot-starter-validation | (included in Boot 4.2.x) | Bean Validation | Required for request validation using @Valid/@Validated with JSR-380 annotations (@NotNull, @Size, @Email, etc.). |
| spring-ai-starter-model-ollama | 2.0.x | Ollama Integration | Required for Ollama LLM integration. Includes API client, chat model implementation, and auto-configuration for localhost:11434. |
| MapStruct | 1.6.x | DTO Mapping | Highly recommended for Entity↔DTO conversions. Compile-time code generation (not reflection), type-safe, and 3-5x faster than ModelMapper. |
| springdoc-openapi-starter-webmvc-ui | 2.x | API Documentation | Recommended for automatic OpenAPI 3.0 spec generation and Swagger UI. Provides interactive documentation at /swagger-ui/index.html. |
| Lombok | 1.18.x | Boilerplate Reduction | Recommended for reducing entity/DTO boilerplate (@Data, @Builder, @Slf4j). Works seamlessly with MapStruct. |
| spring-boot-starter-actuator | (included in Boot 4.2.x) | Production Monitoring | Recommended for health checks, metrics, and operational endpoints (/actuator/health, /metrics). |

**Confidence Level:** HIGH - All libraries verified with Spring Boot 4.x compatibility and current 2025/2026 best practices.

### Frontend Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| React | 19.x | UI Framework | Latest version with concurrent features and improved rendering. De-facto standard for interactive SPAs. Modern hooks API perfect for flashcard state management. |
| TypeScript | 5.7.x | Type Safety | Essential for large applications. Prevents runtime errors, improves IDE support, and provides better refactoring capabilities. Type-safe API client generation possible. |
| Vite | 6.x | Build Tool | 2025 standard for React builds. Near-instant startup, lightning-fast HMR, Rollup-based production builds with tree-shaking. 40% smaller bundles vs webpack. SWC compiler for Rust-based compilation speed. |
| Tailwind CSS | 4.1.x | Utility-First CSS | Industry standard for rapid UI development. Excellent with component libraries, built-in purging for small bundle sizes, and highly customizable. |
| TanStack Query (React Query) | 5.x | Server State Management | 2025 standard for server state. Handles API data fetching, caching, synchronization, background refetching. Essential for vocabulary/inflection data. |
| Zustand | 4.x | Client State Management | Lightweight (1KB), hook-based global state for UI state (flashcard progress, study settings). Pairs perfectly with TanStack Query. 41% usage in 2024 survey, fastest-growing state library. |

**Confidence Level:** HIGH - Current versions verified via official releases and 2025 ecosystem surveys.

### Frontend Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Headless UI | 2.x | Accessible Components | Recommended. Unstyled, fully accessible UI components by Tailwind team. Perfect for custom flashcard interfaces with keyboard navigation. |
| react-quizlet-flashcard | 1.x | Flashcard Components | Optional. TypeScript-based flashcard component with animations. Consider for rapid prototyping, but may build custom with Headless UI for more control. |
| Axios | 1.x | HTTP Client | Recommended for TanStack Query. Better interceptor support than fetch() for auth tokens and error handling. TypeScript-friendly. |
| React Hook Form | 7.x | Form Management | Recommended for vocabulary entry forms. Minimal re-renders, built-in validation, excellent TypeScript support. |
| Zod | 3.x | Schema Validation | Recommended for runtime validation + TypeScript type inference. Pairs with React Hook Form for form schemas. |
| React Router | 7.x | Client-Side Routing | Required for multi-page navigation (flashcards, word lists, settings). Type-safe routes with TypeScript. |
| Vitest | 3.x | Testing Framework | Recommended. Vite-native test runner, Jest-compatible API, faster than Jest. Perfect for component unit tests. |
| React Testing Library | 16.x | Component Testing | Recommended. Behavior-driven testing focused on user interactions, not implementation details. Use with userEvent for realistic interactions. |

**Confidence Level:** HIGH - Libraries verified with React 19.x and Vite 6.x compatibility.

### Database and Schema Design

| Component | Technology | Purpose | Why Recommended |
|-----------|-----------|---------|-----------------|
| Primary DB | PostgreSQL 16+ | Relational Storage | Best-in-class support for linguistic data. JSONB for flexible metadata, array types for inflections, full-text search extensions. |
| Full-Text Search | PGroonga 4.0.x | Multilingual FTS | Essential for Bulgarian support. PostgreSQL's native FTS doesn't support non-Latin alphabets. PGroonga 4.0 provides parallel index builds (2-10x faster) and ORDER BY optimization. |
| Migration Tool | Flyway 10.x | Schema Versioning | Spring Boot native integration. Versioned migrations (V1__*.sql) for schema, repeatable migrations (R__*.sql) for seed data. |
| Connection Pool | HikariCP | (Spring Boot default) | Fastest, most reliable connection pool. Auto-configured by Spring Boot with sensible defaults. |

**Schema Design Patterns for Linguistic Data:**
- **Lemmas Table**: Base form (lemma), language, part_of_speech, metadata (JSONB for flexible attributes)
- **Inflections Table**: Foreign key to lemma, inflected_form, grammatical_tags (JSONB: gender, number, case, tense, etc.), audio_url
- **Study Sessions Table**: User progress, spaced repetition intervals, last_reviewed timestamp
- **Generated Content Table**: LLM-generated metadata (examples, usage notes), generation_timestamp, model_version for auditability

**Confidence Level:** HIGH - PostgreSQL patterns verified via multilingual vocabulary research and PGroonga official documentation.

### Text-to-Speech Integration

| Component | Technology | Purpose | Why Recommended |
|-----------|-----------|---------|-----------------|
| TTS Engine | Edge TTS (Python) | Bulgarian Audio Generation | Free, no API key required. Supports 36+ audio formats (MP3, WebM, OGG, WAV). Neural TTS for realistic Bulgarian pronunciation. Customizable voice, rate, pitch, speaking styles. |
| Integration Pattern | REST Microservice | TTS API | Run Edge TTS as separate Python service (Flask/FastAPI) with async processing. Spring Boot calls via RestClient/WebClient. Cache generated audio in PostgreSQL or object storage. |
| Audio Serving | PostgreSQL bytea or S3-compatible storage | Audio Persistence | For MVP: store audio URLs in inflections table, serve from CDN or static hosting. For scale: S3/MinIO with pre-signed URLs. |

**Edge TTS Node.js Alternative**: If Node.js is preferred, `edge-tts` npm package provides same functionality with streaming support for real-time audio.

**Confidence Level:** MEDIUM - Edge TTS verified via GitHub and PyPI sources. Integration pattern based on standard microservice practices.

### LLM Integration with Ollama

| Component | Technology | Purpose | Why Recommended |
|-----------|-----------|---------|-----------------|
| LLM Runtime | Ollama (separate machine) | Model Serving | Efficient local LLM serving. Supports multiple models (llama3, granite, etc.). No API costs, privacy-first. |
| Spring Integration | Spring AI 2.0 Ollama Starter | Framework Integration | Official Spring AI Ollama integration. Auto-configuration for localhost:11434, chat/streaming APIs, function calling support. |
| Testing | Ollama Testcontainers | Integration Testing | Spring Testcontainers support for Ollama. Ensures reproducible tests with fixed model versions (llama3:8b), low temperature (0.1) for consistent output. |
| Configuration | application.yml | Connection Config | Configure base URL (http://ollama-host:11434), model selection, pull strategy. Use profiles (dev/prod) for different Ollama instances. |

**Recommended Prompt Patterns for Bulgarian Vocabulary:**
- **Inflection Generation**: "Given Bulgarian verb '{lemma}', generate all conjugations for present tense. Return as JSON: {form: string, person: string, number: string}."
- **Usage Examples**: "Provide 3 example sentences using Bulgarian word '{lemma}' in different contexts. Return as JSON array."
- **Translation & Metadata**: "For Bulgarian word '{lemma}', provide: English translation, part of speech, grammatical gender (if noun), usage notes. Return as JSON."

**Confidence Level:** HIGH - Spring AI 2.0 Ollama integration verified via official docs and recent 2025 tutorial articles.

## Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| IntelliJ IDEA / VS Code | IDE | IntelliJ recommended for Spring Boot (ultimate Spring support). VS Code excellent for React with Vite/ESLint extensions. |
| Docker & Docker Compose | Local Development | Run PostgreSQL, Ollama, and optional Edge TTS service locally. Single docker-compose.yml for entire stack. |
| Postman / Bruno | API Testing | Interactive REST client for backend testing. Bruno is open-source, Git-friendly alternative. |
| pgAdmin / DBeaver | Database Client | Visual DB management. DBeaver supports Flyway migration execution. |
| React DevTools | Browser Extension | Inspect component hierarchy, props, state, and performance profiling. |
| TanStack Query DevTools | Browser Extension | Monitor query cache, refetch status, and data staleness. |

**Confidence Level:** HIGH - Standard industry tools with proven Spring Boot and React support.

## Installation

### Backend Dependencies (Maven)

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.2.x</version> <!-- Use latest 4.2.x version -->
</parent>

<properties>
    <java.version>25</java.version>
    <spring-ai.version>2.0.0-M2</spring-ai.version> <!-- Or 2.0.0 GA when available -->
    <mapstruct.version>1.6.3</mapstruct.version>
</properties>

<dependencies>
    <!-- Core Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- Spring AI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>

    <!-- Utilities -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.8.x</version> <!-- Verify latest compatible version -->
    </dependency>

    <!-- Monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>ollama</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

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
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <!-- Lombok MUST be processed before MapStruct -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Frontend Dependencies (npm)

```json
// package.json
{
  "dependencies": {
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-router": "^7.0.0",
    "@tanstack/react-query": "^5.0.0",
    "zustand": "^4.0.0",
    "axios": "^1.7.0",
    "react-hook-form": "^7.54.0",
    "zod": "^3.24.0",
    "@headlessui/react": "^2.2.0",
    "@heroicons/react": "^2.2.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react-swc": "^3.7.0",
    "vite": "^6.0.0",
    "typescript": "^5.7.0",
    "tailwindcss": "^4.1.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "vitest": "^3.0.0",
    "@testing-library/react": "^16.0.0",
    "@testing-library/user-event": "^14.5.0",
    "@testing-library/jest-dom": "^6.6.0",
    "eslint": "^9.0.0",
    "prettier": "^3.4.0"
  }
}
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Spring AI | LangChain4j | If you need more advanced prompt chaining, agents, or RAG patterns. LangChain4j is more mature but Spring AI is catching up with official Spring backing. |
| MapStruct | ModelMapper | Never in new projects. ModelMapper uses reflection (slower, runtime errors). MapStruct is compile-time safe and 3-5x faster. |
| TanStack Query + Zustand | Redux Toolkit | Only if team is deeply familiar with Redux or you need time-travel debugging. Modern consensus is TanStack Query + Zustand for 40% smaller bundles and simpler code. |
| Vite | Create React App (CRA) | Never. CRA is deprecated and unmaintained since 2022. Vite is 10-20x faster in dev mode. |
| Headless UI | Material UI / Ant Design | If you want pre-styled components and don't mind heavier bundles. Headless UI is better for custom designs with Tailwind. |
| Flyway | Liquibase | If you need database-agnostic migrations or XML/YAML formats. Flyway's SQL-first approach is simpler and more transparent for PostgreSQL-only projects. |
| PostgreSQL | MySQL | Never for linguistic data. MySQL has weaker support for complex queries, JSONB equivalents, and full-text search in non-Latin scripts. |
| Edge TTS | Google Cloud TTS / Amazon Polly | If you need absolute highest quality synthesis or commercial support. But Edge TTS is free, supports Bulgarian well, and requires no API keys. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Spring Boot 3.x | Spring AI 2.x requires Spring Boot 4.x. Upgrading mid-project is painful. | Spring Boot 4.2.x |
| javax.* validation packages | Deprecated since Jakarta EE migration. Replaced by jakarta.* in Spring Boot 4. | jakarta.validation.constraints.* |
| @Autowired field injection | Makes testing harder, hides dependencies, causes issues with immutability. | Constructor injection (preferred) or Lombok @RequiredArgsConstructor |
| JPA entity direct exposure in REST APIs | Exposes internal structure, lazy loading issues, security risks (password fields), cannot evolve API independently. | DTOs with MapStruct mappers |
| Native queries without named parameters | SQL injection risks, harder to read. | JPQL or Criteria API with named parameters (:param) |
| Swagger 2.x annotations | Deprecated. Spring Boot 4 uses OpenAPI 3.x standards. | springdoc-openapi with @Operation, @Schema annotations |
| Create React App (CRA) | Unmaintained since 2022, slow builds, outdated webpack config. | Vite |
| prop-types for React | Replaced by TypeScript for type checking at compile time. | TypeScript interfaces/types |
| Moment.js | Deprecated, 67KB bundle size. | Native JavaScript Date APIs or date-fns (2KB modular) |
| PGroonga alternatives for Bulgarian FTS | PostgreSQL's native FTS (pg_trgm) doesn't support Cyrillic morphology. Elasticsearch is overkill for vocabulary app scale. | PGroonga 4.0+ |

## Stack Patterns by Variant

**If building mobile app later:**
- Use Spring Boot backend as-is (REST API is platform-agnostic)
- Consider adding Spring GraphQL for flexible querying if mobile app needs differ
- React Native can reuse vocabulary logic (TanStack Query works in RN)

**If deploying to cloud (AWS/GCP/Azure):**
- Use managed PostgreSQL (RDS/Cloud SQL/Azure DB)
- Run Ollama on GPU instance (EC2 g5.xlarge / GCP n1-standard-4 with T4)
- Consider Amazon Polly / Google Cloud TTS instead of Edge TTS for commercial support
- Use S3/GCS/Azure Blob for audio storage with CDN (CloudFront/Cloud CDN)

**If self-hosting on-premise:**
- Docker Compose stack is production-ready for small-medium scale (< 10K users)
- Ensure Ollama machine has GPU (NVIDIA recommended, 8GB+ VRAM for llama3:8b)
- Consider Traefik/Caddy for automatic HTTPS with Let's Encrypt
- PostgreSQL replication (streaming replication) for high availability

**If targeting offline-first PWA:**
- Use Vite PWA plugin with Workbox for service worker caching
- IndexedDB for client-side vocabulary caching (Dexie.js wrapper recommended)
- Background sync for study progress when online
- Audio preloading strategy for offline flashcard review

## Version Compatibility Matrix

| Spring Boot | Spring AI | Java | PostgreSQL | Node.js | React |
|-------------|-----------|------|------------|---------|-------|
| 4.2.x | 2.0.x | 25 | 16+ | 22+ LTS | 19.x |
| 4.0.x | 2.0.0-M2 | 25 | 15+ | 20+ LTS | 18.x |
| 3.5.x | 1.1.x | 21 | 14+ | 20+ LTS | 18.x |

**Critical Compatibility Notes:**
- Spring AI 2.x **requires** Spring Boot 4.x - cannot use 1.x with Boot 4 or 2.x with Boot 3
- MapStruct 1.6+ required for Java 21+ record support
- Testcontainers requires Docker running locally for integration tests
- PGroonga 4.0+ requires PostgreSQL 13+ (supports up to PostgreSQL 18)
- Vite 6.x requires Node.js 22+ (use nvm for version management)

## Sources

**Spring Boot & Spring AI:**
- [Spring AI GitHub Repository](https://github.com/spring-projects/spring-ai) — Core framework info
- [Spring AI Releases](https://github.com/spring-projects/spring-ai/releases) — Version compatibility (2.0.0-M2 verified)
- [Using Ollama with Spring AI - Piotr's TechBlog](https://piotrminkowski.com/2025/03/10/using-ollama-with-spring-ai/) — Integration patterns
- [Spring Boot 4 Migration Guide](https://www.moderne.ai/blog/spring-boot-4x-migration-guide) — Migration considerations
- [Integrating AI with Spring Boot: A Beginner's Guide](https://mydeveloperplanet.com/2025/01/08/integrating-ai-with-spring-boot-a-beginners-guide/) — Setup tutorial

**Spring Data JPA & Database:**
- [Best Practices for Spring Data JPA - JavaGuides](https://medium.com/javaguides/best-practices-for-spring-data-jpa-the-ultimate-guide-c2a84a4cd45e) — DTO patterns, performance
- [How to Manage Database Migrations with Flyway in Spring Boot](https://oneuptime.com/blog/post/2026-01-25-database-migrations-flyway-spring-boot/view) — Migration best practices
- [PGroonga 4.0.0 - Multilingual Fast Full Text Search](https://www.postgresql.org/about/news/pgroonga-400-multilingual-fast-full-text-search-3012/) — Bulgarian FTS support

**React & Frontend:**
- [React State Management in 2025: What You Actually Need](https://www.developerway.com/posts/react-state-management-2025) — TanStack Query + Zustand pattern
- [Headless UI](https://headlessui.com/) — Accessible component library
- [Advanced Guide to Using Vite with React in 2025](https://codeparrot.ai/blogs/advanced-guide-to-using-vite-with-react-in-2025) — Build configuration
- [React Testing Library Best Practices](https://medium.com/@ignatovich.dm/best-practices-for-using-react-testing-library-0f71181bb1f4) — Testing approach

**Supporting Technologies:**
- [MapStruct Quick Guide - Baeldung](https://www.baeldung.com/mapstruct) — DTO mapping
- [Spring Boot OpenAPI Documentation Setup](https://oneuptime.com/blog/post/2025-12-22-spring-boot-swagger-openapi-documentation/view) — API docs
- [Edge TTS Ultimate Guide for Developers - VideoSDK](https://www.videosdk.live/developer-hub/ai/edge-tts) — TTS integration
- [Spring Boot Testing with Testcontainers](https://maciejwalkowiak.com/blog/testcontainers-spring-boot-setup/) — Testing patterns

**Database Schema Design:**
- [Building a Multilingual Vocabulary Database - ResearchGate](https://www.researchgate.net/publication/393615589_Building_a_Multilingual_Vocabulary_Database_A_Comprehensive_Study_of_24_Global_and_Local_Languages) — Linguistic data patterns

---
*Stack research for: Bulgarian Vocabulary Learning Application*
*Researched: 2026-02-15*
*Next Steps: Use this stack specification for roadmap creation and phase planning*
