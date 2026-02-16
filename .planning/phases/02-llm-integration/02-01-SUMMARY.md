---
phase: 02-llm-integration
plan: 01
subsystem: llm
tags: [spring-ai, ollama, redis, valkey, resilience4j, async, cache, bggpt]

# Dependency graph
requires:
  - phase: 01-foundation-and-data-model
    provides: Domain model entities (Lemma, Inflection) and enums (PartOfSpeech, DifficultyLevel, ReviewStatus)
provides:
  - Spring AI Ollama integration with ChatClient bean
  - Async execution infrastructure with dedicated LLM thread pool
  - Redis/Valkey cache with 24h TTL for LLM responses
  - Resilience4j circuit breaker for Ollama fault tolerance
  - Structured DTOs for lemma detection, inflection generation, and metadata
affects: [02-02, llm-service-implementation, api-layer]

# Tech tracking
tech-stack:
  added: [spring-ai-starter-model-ollama 1.1.0-M3, spring-boot-starter-data-redis, resilience4j-spring-boot3 2.2.0, spring-boot-starter-aop, spring-boot-starter-cache]
  patterns: [async LLM execution with ThreadPoolTaskExecutor, Redis cache-aside pattern for LLM responses, circuit breaker pattern for external LLM calls, structured LLM output with Jakarta validation]

key-files:
  created:
    - backend/src/main/java/com/vocab/bulgarian/llm/config/AsyncConfig.java
    - backend/src/main/java/com/vocab/bulgarian/llm/config/CacheConfig.java
    - backend/src/main/java/com/vocab/bulgarian/llm/config/LlmConfig.java
    - backend/src/main/java/com/vocab/bulgarian/llm/dto/LemmaDetectionResponse.java
    - backend/src/main/java/com/vocab/bulgarian/llm/dto/InflectionSet.java
    - backend/src/main/java/com/vocab/bulgarian/llm/dto/LemmaMetadata.java
  modified:
    - backend/pom.xml
    - backend/src/main/resources/application.yml
    - backend/src/main/resources/application-dev.yml

key-decisions:
  - "Spring AI BOM 1.1.0-M3 milestone version provides ChatClient.Builder auto-configuration"
  - "Resilience4j 2.2.0 explicit version (not in Spring Boot BOM) for circuit breaker"
  - "Redis cache with 24h TTL and JSON serialization for structured DTO caching"
  - "CallerRunsPolicy for thread pool rejection to apply backpressure on overload"
  - "Dev environment uses Mac Studio LAN endpoint (mac-studio.local:11434) for Ollama"
  - "Bulgarian language expert system message enforces JSON-only responses"

patterns-established:
  - "LLM configuration: ChatClient.Builder injected by Spring AI auto-config, wrapped with default system message"
  - "Async pattern: @EnableAsync with dedicated executor bean, AsyncConfigurer for exception handling"
  - "Cache pattern: @EnableCaching with RedisCacheManager, named caches per operation type"
  - "DTO pattern: Jakarta validation on records, static factory methods for failure cases"

# Metrics
duration: 3min
completed: 2026-02-15
---

# Phase 2 Plan 1: LLM Integration Foundation Summary

**Spring AI Ollama integration with async execution, Redis caching, circuit breaker resilience, and structured DTOs for Bulgarian lemma processing**

## Performance

- **Duration:** 3 min 6 sec
- **Started:** 2026-02-15T12:50:33Z
- **Completed:** 2026-02-15T12:53:39Z
- **Tasks:** 2
- **Files modified:** 11 (3 modified, 8 created)

## Accomplishments
- Spring AI Ollama starter integrated with BgGPT model (todorov/bggpt:9b) configured
- Async LLM execution with dedicated thread pool (core=4, max=8, queue=25)
- Redis/Valkey cache manager with 3 named caches and 24-hour TTL
- Resilience4j circuit breaker protecting Ollama calls (50% threshold, 60s open state)
- Three structured DTO records with Jakarta validation for all LLM operation types

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Spring AI, caching, and resilience dependencies to pom.xml** - `5e77146` (chore)
2. **Task 2: Configure Spring AI, async, cache, circuit breaker, and create DTO records** - `260a4db` (feat)

## Files Created/Modified

**Created:**
- `backend/src/main/java/com/vocab/bulgarian/llm/config/AsyncConfig.java` - ThreadPoolTaskExecutor with backpressure via CallerRunsPolicy
- `backend/src/main/java/com/vocab/bulgarian/llm/config/CacheConfig.java` - RedisCacheManager with 3 named caches (lemmaDetection, inflectionGeneration, metadataGeneration)
- `backend/src/main/java/com/vocab/bulgarian/llm/config/LlmConfig.java` - ChatClient bean with Bulgarian language expert system message
- `backend/src/main/java/com/vocab/bulgarian/llm/dto/LemmaDetectionResponse.java` - Lemma detection response with failed() factory method
- `backend/src/main/java/com/vocab/bulgarian/llm/dto/InflectionSet.java` - Inflection set with nested InflectionEntry record
- `backend/src/main/java/com/vocab/bulgarian/llm/dto/LemmaMetadata.java` - Metadata response aligned with domain enums

**Modified:**
- `backend/pom.xml` - Added Spring AI BOM, Ollama starter, Redis, cache, Resilience4j, AOP dependencies
- `backend/src/main/resources/application.yml` - Added Spring AI, Redis, cache, and circuit breaker configuration
- `backend/src/main/resources/application-dev.yml` - Overrode Ollama URL for Mac Studio LAN access

## Decisions Made

- **Spring AI milestone version (1.1.0-M3):** Required for ChatClient.Builder auto-configuration; milestone repo added to pom.xml
- **Resilience4j explicit version (2.2.0):** Not managed by Spring Boot BOM, requires explicit version declaration
- **Redis cache type with JSON serialization:** GenericJackson2JsonRedisSerializer for structured DTO caching with 24h TTL
- **Async executor backpressure:** CallerRunsPolicy prevents queue overflow by running tasks on caller thread when queue full
- **Dev environment Ollama URL:** Mac Studio at mac-studio.local:11434 over LAN for development

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added explicit version for Resilience4j dependency**
- **Found during:** Task 1 (dependency resolution)
- **Issue:** `resilience4j-spring-boot3` missing version, not managed by Spring Boot BOM
- **Fix:** Added explicit version `2.2.0` to dependency declaration
- **Files modified:** backend/pom.xml
- **Verification:** `mvn dependency:resolve` succeeded
- **Committed in:** 5e77146 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Auto-fix necessary for dependency resolution. No scope creep.

## Issues Encountered

None - compilation and verification passed on first attempt after dependency version fix.

## User Setup Required

**Redis/Valkey server required for cache functionality.**

Environment variables:
- `REDIS_HOST` (default: localhost)
- `REDIS_PORT` (default: 6379)
- `OLLAMA_BASE_URL` (default: http://localhost:11434, dev: http://mac-studio.local:11434)

Verification:
```bash
# Verify Redis connection
redis-cli ping  # Should return PONG

# Verify Ollama connection (dev)
curl http://mac-studio.local:11434/api/tags  # Should list models including todorov/bggpt:9b
```

## Next Phase Readiness

- LLM configuration foundation complete, ready for service implementation
- ChatClient bean ready for injection into service classes
- Cache, async, and circuit breaker infrastructure ready for use
- DTOs ready for structured output conversion with BeanOutputConverter
- Next plan (02-02) can implement LLM service classes using this infrastructure

## Self-Check: PASSED

All claimed files verified:
- ✓ AsyncConfig.java
- ✓ CacheConfig.java
- ✓ LlmConfig.java
- ✓ LemmaDetectionResponse.java
- ✓ InflectionSet.java
- ✓ LemmaMetadata.java

All claimed commits verified:
- ✓ 5e77146 (Task 1)
- ✓ 260a4db (Task 2)

---
*Phase: 02-llm-integration*
*Completed: 2026-02-15*
