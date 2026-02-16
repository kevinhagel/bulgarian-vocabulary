# Phase 2: LLM Integration - Research

**Researched:** 2026-02-15
**Domain:** Spring AI, Ollama LLM integration, async processing, caching, circuit breaker patterns
**Confidence:** HIGH

## Summary

Phase 2 integrates Ollama LLM capabilities into the Spring Boot application for Bulgarian language processing (lemma detection, inflection generation, and metadata extraction). The standard stack combines **Spring AI 1.1.x** (with Spring Boot 3.x compatibility) for Ollama integration, **@Async with CompletableFuture** for non-blocking execution, **Caffeine** for high-performance local caching, and **Resilience4j** for circuit breaker protection. Spring AI provides auto-configuration and a fluent ChatClient API similar to WebClient, making LLM integration natural for Spring developers. Bulgarian language processing requires specialized models (BgGPT, OpenEuroLLM-Bulgarian) that understand Cyrillic morphology. The Mac Studio LAN setup is already configured in application-dev.yml (mac-studio.local:11434).

The architecture follows a service-layer pattern where LLM services orchestrate chat model calls, apply advisors (for memory, RAG patterns), cache responses, and return structured outputs via BeanOutputConverter. Async execution prevents request thread blocking, while circuit breakers protect against Ollama unavailability. Structured output validation is critical since non-OpenAI models may produce valid JSON that doesn't match the schema.

**Primary recommendation:** Use Spring AI starter with Ollama ChatClient, structure LLM responses as POJOs via BeanOutputConverter, cache at service layer with Caffeine (content-based keys), wrap calls with @Async CompletableFuture, and protect with Resilience4j @CircuitBreaker. Implement custom validation for LLM outputs (empty checks, morphology rules) since schema compliance isn't guaranteed with open models.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| spring-ai-starter-model-ollama | 1.1.x | Ollama integration with auto-config | Official Spring AI integration, provides ChatClient API, auto-configured beans, streaming support |
| spring-boot-starter-cache | 3.4.x | Declarative caching abstraction | Spring's standard caching abstraction, enables @Cacheable annotations |
| caffeine | Latest | High-performance local cache | Industry standard for Java caching, superior performance to Guava, used by Spring internally |
| resilience4j-spring-boot3 | 2.x | Circuit breaker and resilience patterns | De facto standard for resilience patterns in Spring Boot 3, replaces Netflix Hystrix |
| spring-boot-starter-validation | 3.4.x | Bean validation for DTOs | Standard Jakarta Bean Validation integration |

**Installation (Maven):**
```xml
<dependencies>
    <!-- Spring AI Ollama -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>

    <!-- Async and Caching -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- Circuit Breaker -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.0-M3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| spring-boot-starter-actuator | 3.4.x | Health checks, metrics, monitoring | Production deployments to expose circuit breaker metrics |
| jackson-databind | 3.4.x (via Spring Boot) | JSON serialization for structured outputs | Already included, used by BeanOutputConverter |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Caffeine | Redis | Caffeine: sub-millisecond local cache, no network overhead, simple setup. Redis: distributed cache for multi-instance deployments (not needed yet) |
| Resilience4j | Spring Retry alone | Resilience4j provides circuit breaker pattern (prevents cascading failures), Spring Retry only retries (can amplify failures) |
| Spring AI ChatClient | Direct OllamaApi calls | ChatClient: fluent API, portable across models, advisors. Direct API: more control but tightly coupled to Ollama |

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/vocab/bulgarian/
├── llm/
│   ├── config/
│   │   ├── AsyncConfig.java              # @EnableAsync, ThreadPoolTaskExecutor
│   │   ├── CacheConfig.java              # @EnableCaching, Caffeine beans
│   │   └── LlmConfig.java                # ChatClient bean configuration
│   ├── service/
│   │   ├── LemmaDetectionService.java    # Detect lemma from word form
│   │   ├── InflectionGenerationService.java # Generate all inflections
│   │   └── MetadataGenerationService.java   # Part of speech, category, difficulty
│   ├── dto/
│   │   ├── LemmaDetectionRequest.java
│   │   ├── LemmaDetectionResponse.java
│   │   ├── InflectionSet.java            # Structured output from LLM
│   │   └── LemmaMetadata.java            # Structured output from LLM
│   └── validation/
│       └── LlmOutputValidator.java       # Custom validation for LLM responses
```

### Pattern 1: Service Layer with ChatClient

**What:** Service layer orchestrates LLM calls using ChatClient fluent API, applies caching, async execution, and circuit breaker protection

**When to use:** All LLM integration scenarios in this phase

**Example:**
```java
// Source: Spring AI Documentation + verified patterns from search
@Service
@Validated
public class LemmaDetectionService {

    private final ChatClient chatClient;
    private final LlmOutputValidator validator;

    public LemmaDetectionService(ChatClient.Builder chatClientBuilder,
                                  LlmOutputValidator validator) {
        this.chatClient = chatClientBuilder.build();
        this.validator = validator;
    }

    @Async
    @Cacheable(value = "lemmaDetection", key = "#wordForm")
    @CircuitBreaker(name = "ollama", fallbackMethod = "detectLemmaFallback")
    public CompletableFuture<LemmaDetectionResponse> detectLemmaAsync(String wordForm) {
        BeanOutputConverter<LemmaDetectionResponse> converter =
            new BeanOutputConverter<>(LemmaDetectionResponse.class);

        String prompt = String.format("""
            Given the Bulgarian word "%s", identify its lemma (dictionary form).
            For verbs, use 1st person singular present tense.

            %s
            """, wordForm, converter.getFormat());

        LemmaDetectionResponse response = chatClient.prompt()
            .user(prompt)
            .call()
            .entity(LemmaDetectionResponse.class);

        // Critical: Validate LLM output (schema compliance not guaranteed)
        validator.validateLemmaDetection(response);

        return CompletableFuture.completedFuture(response);
    }

    private CompletableFuture<LemmaDetectionResponse> detectLemmaFallback(
            String wordForm, Exception ex) {
        // Circuit breaker fallback: return empty response with error flag
        return CompletableFuture.completedFuture(
            LemmaDetectionResponse.builder()
                .wordForm(wordForm)
                .detectionFailed(true)
                .build()
        );
    }
}
```

### Pattern 2: Structured Output with Validation

**What:** Use BeanOutputConverter to map LLM JSON responses to POJOs, then apply custom validation

**When to use:** All LLM responses that need structure (inflections, metadata)

**Example:**
```java
// Structured output DTO with validation annotations
public record InflectionSet(
    @NotBlank String lemma,
    @NotNull PartOfSpeech partOfSpeech,
    @NotEmpty List<Inflection> inflections
) {
    public record Inflection(
        @NotBlank String text,
        String grammaticalTags  // e.g., "1sg.pres.perf"
    ) {}
}

// Custom validator for LLM-specific rules
@Component
public class LlmOutputValidator {

    public void validateInflectionSet(InflectionSet set) {
        if (set == null) {
            throw new LlmValidationException("LLM returned null inflection set");
        }

        // Check for empty inflections (LLM failure)
        if (set.inflections().isEmpty()) {
            throw new LlmValidationException("LLM generated zero inflections");
        }

        // Check for duplicate inflections
        Set<String> unique = new HashSet<>();
        for (var inflection : set.inflections()) {
            if (!unique.add(inflection.text())) {
                throw new LlmValidationException(
                    "Duplicate inflection detected: " + inflection.text());
            }
        }

        // Bulgarian-specific: Check Cyrillic characters
        for (var inflection : set.inflections()) {
            if (!inflection.text().matches(".*[а-яА-Я].*")) {
                throw new LlmValidationException(
                    "Inflection contains no Cyrillic characters: " + inflection.text());
            }
        }
    }
}
```

### Pattern 3: Async Configuration with Custom Executor

**What:** Configure ThreadPoolTaskExecutor for @Async methods with proper pool sizing and rejection handling

**When to use:** Required for async LLM calls

**Example:**
```java
// Source: Spring Boot async best practices (verified 2026)
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "llmTaskExecutor")
    public ThreadPoolTaskExecutor llmTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool: based on LLM concurrency (Ollama can handle ~4-8 concurrent)
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);

        // Queue: buffer requests during spikes
        executor.setQueueCapacity(25);

        // Thread naming for debugging
        executor.setThreadNamePrefix("llm-async-");

        // Rejection policy: CallerRunsPolicy (caller thread handles overflow)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Graceful shutdown: wait for tasks to complete
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Async exception in method {}: {}", method.getName(), ex.getMessage(), ex);
    }
}
```

### Pattern 4: Caffeine Cache Configuration with TTL

**What:** Configure Caffeine with time-based eviction for LLM response caching

**When to use:** Required for caching expensive LLM calls

**Example:**
```java
// Source: Caffeine Spring Boot best practices
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "lemmaDetection",
            "inflectionGeneration",
            "metadataGeneration"
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            // Expire after write: LLM responses are stable
            .expireAfterWrite(24, TimeUnit.HOURS)

            // Max size: prevent unbounded growth
            .maximumSize(10_000)

            // Metrics: record cache stats for monitoring
            .recordStats();
    }

    @Bean
    public CacheMetricsRegistrar cacheMetricsRegistrar(CacheManager cacheManager,
                                                        MeterRegistry registry) {
        // Expose cache metrics to Spring Boot Actuator
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                    caffeineCache.getNativeCache();
                CaffeineCacheMetrics.monitor(registry, nativeCache, cacheName);
            }
        });
        return new CacheMetricsRegistrar();
    }
}
```

### Pattern 5: Resilience4j Circuit Breaker Configuration

**What:** Configure circuit breaker to protect against Ollama unavailability

**When to use:** All external LLM calls

**Example:**
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      ollama:
        # Sliding window: monitor last 10 calls
        sliding-window-size: 10
        sliding-window-type: COUNT_BASED

        # Open circuit if 50% of calls fail
        failure-rate-threshold: 50

        # Wait 60s before attempting recovery (half-open)
        wait-duration-in-open-state: 60s

        # Test with 3 calls in half-open state
        permitted-number-of-calls-in-half-open-state: 3

        # Minimum calls before evaluating failure rate
        minimum-number-of-calls: 5

        # Expose metrics to Actuator
        register-health-indicator: true
```

```java
// Usage in service (already shown in Pattern 1)
@CircuitBreaker(name = "ollama", fallbackMethod = "methodNameFallback")
public CompletableFuture<Response> callLlm(...) { ... }
```

### Anti-Patterns to Avoid

- **Blocking LLM calls in request thread:** Always use @Async with CompletableFuture to prevent thread exhaustion
- **Caching without content-based keys:** Don't use timestamp or random keys; use actual input content (e.g., wordForm) for cache hits
- **Trusting LLM schema compliance:** Non-OpenAI models produce valid JSON that may not match your schema; always validate
- **No circuit breaker on external calls:** Ollama failures can cascade without circuit breaker protection
- **Caching JPA entities:** Cache DTOs/records, not managed entities (causes detached entity issues)
- **Ignoring async executor sizing:** Default executor is unbounded; configure ThreadPoolTaskExecutor with proper limits

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| LLM client | Raw HTTP client with Ollama REST API | Spring AI ChatClient | Handles request/response mapping, streaming, retries, model portability |
| Structured output parsing | Manual JSON parsing with Jackson | BeanOutputConverter | Generates format instructions for LLM, handles deserialization, integrates with ChatClient |
| Caching layer | Custom HashMap with expiration logic | Spring Cache + Caffeine | Thread-safe, eviction policies (TTL, size), metrics, production-tested |
| Circuit breaker | Manual retry counters and failure tracking | Resilience4j @CircuitBreaker | Handles state transitions (closed/open/half-open), metrics, thread-safe |
| Async thread pools | Raw ExecutorService | ThreadPoolTaskExecutor | Spring lifecycle management, graceful shutdown, exception handlers |
| Bulgarian morphology rules | Hardcoded inflection templates | LLM generation + validation | Bulgarian has complex morphology (aspect, definiteness); LLMs handle exceptions better than rules |

**Key insight:** LLM integration has deceptive complexity in error handling (timeouts, malformed responses, model unavailability), caching (content-based keys, invalidation), and async orchestration (thread pool sizing, backpressure). Spring AI abstracts the easy parts (HTTP client), while Spring Boot patterns (caching, async, circuit breaker) handle the hard parts. Combining them properly avoids re-implementing production-hardened solutions.

## Common Pitfalls

### Pitfall 1: Circuit Breaker on Async Methods Without Proper Configuration

**What goes wrong:** Circuit breaker doesn't activate because async proxy doesn't trigger Resilience4j interceptor

**Why it happens:** Spring AOP proxies for @Async and @CircuitBreaker can conflict; order matters

**How to avoid:** Apply @CircuitBreaker on async methods, ensure Resilience4j AOP is configured, test circuit breaker state transitions

**Warning signs:** Circuit breaker metrics show zero transitions despite Ollama failures; fallback methods never execute

### Pitfall 2: Caching Async CompletableFuture Instead of Result

**What goes wrong:** Cache stores CompletableFuture wrapper, not the actual result; every call creates new future

**Why it happens:** @Cacheable on CompletableFuture<T> methods caches the future object, not T

**How to avoid:** Use synchronous method that returns T, then wrap in async caller OR use custom cache key resolver that extracts from CompletableFuture

**Warning signs:** Cache hit rate is 0% despite repeated calls with same input

### Pitfall 3: LLM JSON Schema Mismatch on Non-OpenAI Models

**What goes wrong:** BeanOutputConverter deserializes successfully but with wrong/missing fields

**Why it happens:** Ollama models (Mistral, Llama, BgGPT) have "JSON mode" but don't guarantee schema compliance like OpenAI

**How to avoid:** Implement custom validation after BeanOutputConverter; check for required fields, reasonable values, Bulgarian-specific rules

**Warning signs:** Inflection lists are empty, part of speech is null, difficulty level is missing despite "successful" deserialization

### Pitfall 4: Unbounded Async Thread Pool Exhaustion

**What goes wrong:** Too many concurrent LLM requests create thousands of threads, causing OutOfMemoryError

**Why it happens:** Default Spring async executor is unbounded; LLM calls are slow (2-10s), causing thread buildup

**How to avoid:** Configure ThreadPoolTaskExecutor with max pool size (8) and queue capacity (25); use CallerRunsPolicy for backpressure

**Warning signs:** Thread count grows unbounded in metrics; OOMError under load; response times degrade exponentially

### Pitfall 5: Mac Studio LAN Connectivity Failures

**What goes wrong:** Development MacBook can't reach Mac Studio Ollama server; connection timeouts

**Why it happens:** Firewall blocks port 11434, mDNS resolution fails (mac-studio.local), network routing issues

**How to avoid:** Test connectivity with `curl http://mac-studio.local:11434/api/tags`; configure firewall exception; use IP address fallback; set proper connection timeout in ChatClient

**Warning signs:** All LLM calls fail immediately with connection refused; circuit breaker opens permanently; application.yml has mac-studio.local but connection times out

### Pitfall 6: BgGPT Model Not Loaded in Ollama

**What goes wrong:** Ollama returns 404 or "model not found" error

**Why it happens:** BgGPT model must be explicitly pulled on Mac Studio: `ollama pull todorov/bggpt`

**How to avoid:** Document model setup in development guide; use Spring AI auto-pull configuration (when_missing strategy) in dev profile; implement fallback to multilingual model

**Warning signs:** 404 errors from Ollama API; error message "model 'bggpt' not found"; works with 'mistral' but not Bulgarian models

## Code Examples

Verified patterns from official sources:

### Fluent ChatClient API

```java
// Source: Spring AI ChatClient documentation
@Service
public class InflectionGenerationService {

    private final ChatClient chatClient;

    public InflectionGenerationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
            .defaultOptions(ChatOptions.builder()
                .model("todorov/bggpt:9b")  // Bulgarian-optimized model
                .temperature(0.3)           // Lower temp for deterministic morphology
                .build())
            .build();
    }

    public InflectionSet generateInflections(String lemma, PartOfSpeech pos) {
        BeanOutputConverter<InflectionSet> converter =
            new BeanOutputConverter<>(InflectionSet.class);

        return chatClient.prompt()
            .user(u -> u.text("""
                Generate ALL inflections for the Bulgarian {pos} "{lemma}".
                Include all persons, numbers, tenses, aspects, moods, and genders as applicable.

                {format}
                """)
                .param("pos", pos.name())
                .param("lemma", lemma)
                .param("format", converter.getFormat()))
            .call()
            .entity(InflectionSet.class);
    }
}
```

### ChatClient with Advisors (RAG Pattern)

```java
// Source: Spring AI Advisors documentation
@Service
public class MetadataGenerationService {

    private final ChatClient chatClient;

    public MetadataGenerationService(ChatClient.Builder chatClientBuilder,
                                      VectorStore vectorStore) {
        this.chatClient = chatClientBuilder
            // QuestionAnswerAdvisor: implements RAG pattern
            .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
            .build();
    }

    public LemmaMetadata generateMetadata(String lemma) {
        // Advisor retrieves similar lemmas from vector store
        // and includes them in system prompt for context
        return chatClient.prompt()
            .user("Determine part of speech, category, and difficulty (A1-C2) for: " + lemma)
            .call()
            .entity(LemmaMetadata.class);
    }
}
```

### Content-Based Cache Keys

```java
// Source: Spring Cache best practices
@Service
public class LemmaDetectionService {

    // Cache key is content hash, not timestamp or random
    @Cacheable(value = "lemmaDetection",
               key = "#wordForm",
               unless = "#result.detectionFailed")
    public LemmaDetectionResponse detectLemma(String wordForm) {
        // ... LLM call ...
    }

    // Invalidate cache when user corrects LLM output
    @CacheEvict(value = "lemmaDetection", key = "#wordForm")
    public void invalidateLemmaDetection(String wordForm) {
        // Cache entry removed
    }
}
```

### Validation After Structured Output

```java
// Source: Spring Boot validation best practices
@Component
public class LlmOutputValidator {

    private final Validator validator;

    public LlmOutputValidator(Validator validator) {
        this.validator = validator;
    }

    public void validateInflectionSet(InflectionSet set) {
        // Step 1: Jakarta Bean Validation (@NotNull, @NotBlank, etc.)
        Set<ConstraintViolation<InflectionSet>> violations = validator.validate(set);
        if (!violations.isEmpty()) {
            throw new LlmValidationException("Schema validation failed: " + violations);
        }

        // Step 2: Domain-specific validation (Bulgarian morphology rules)
        if (set.partOfSpeech() == PartOfSpeech.VERB) {
            // Expect at least 10 inflections for Bulgarian verbs
            if (set.inflections().size() < 10) {
                throw new LlmValidationException(
                    "Bulgarian verb should have at least 10 inflections, got " +
                    set.inflections().size());
            }
        }

        // Step 3: Cyrillic character validation
        for (var inflection : set.inflections()) {
            if (!containsCyrillic(inflection.text())) {
                throw new LlmValidationException(
                    "Inflection must contain Cyrillic: " + inflection.text());
            }
        }
    }

    private boolean containsCyrillic(String text) {
        return text.matches(".*[а-яА-Я].*");
    }
}
```

### CompletableFuture Composition

```java
// Source: Spring Boot async best practices
@Service
public class VocabularyOrchestrationService {

    private final LemmaDetectionService lemmaDetection;
    private final InflectionGenerationService inflectionGeneration;
    private final MetadataGenerationService metadataGeneration;

    // Compose multiple async LLM calls
    public CompletableFuture<CompleteVocabularyEntry> processNewWord(String wordForm) {
        // Step 1: Detect lemma (async)
        CompletableFuture<LemmaDetectionResponse> lemmaFuture =
            lemmaDetection.detectLemmaAsync(wordForm);

        // Step 2: Generate inflections and metadata in parallel (both depend on lemma)
        return lemmaFuture.thenCompose(lemma -> {
            CompletableFuture<InflectionSet> inflectionsFuture =
                inflectionGeneration.generateInflectionsAsync(lemma.getLemmaText(), lemma.getPartOfSpeech());

            CompletableFuture<LemmaMetadata> metadataFuture =
                metadataGeneration.generateMetadataAsync(lemma.getLemmaText());

            // Combine both results
            return inflectionsFuture.thenCombine(metadataFuture,
                (inflections, metadata) -> CompleteVocabularyEntry.builder()
                    .lemma(lemma.getLemmaText())
                    .inflections(inflections)
                    .metadata(metadata)
                    .reviewStatus(ReviewStatus.PENDING)  // User must review LLM output
                    .build()
            );
        });
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| LangChain4j (Java port of Python LangChain) | Spring AI native | 2024-2025 | Spring AI integrates naturally with Spring ecosystem (auto-config, actuator, caching); LangChain4j is viable alternative but not idiomatic Spring |
| OpenAI-only LLM integration | Model-portable ChatClient API | Spring AI 1.0+ | Single API works with Ollama, OpenAI, Anthropic, Azure OpenAI; swap models via configuration |
| Netflix Hystrix circuit breaker | Resilience4j | ~2018-2020 | Hystrix deprecated, Resilience4j is modern replacement with better Spring Boot 3 integration |
| Synchronous LLM calls | Async with CompletableFuture + reactive advisors | 2024+ | Non-blocking execution prevents thread exhaustion; reactive patterns enable backpressure |
| Manual JSON parsing | BeanOutputConverter with schema | Spring AI 1.0+ | LLM generates schema-conforming JSON automatically; structured output is first-class citizen |
| Prompt strings | Prompt templates with placeholders | Spring AI 1.0+ | Parameterized prompts reduce error-prone string concatenation |

**Deprecated/outdated:**
- **AsyncResult:** Deprecated in Spring 6.x, use CompletableFuture for async methods
- **Netflix Hystrix:** No longer maintained, use Resilience4j for circuit breaker patterns
- **Guava Cache:** Inferior performance to Caffeine, which is now Spring's recommended in-memory cache

## Bulgarian Language LLM Considerations

### Specialized Models for Bulgarian

| Model | Size | Strengths | When to Use |
|-------|------|-----------|-------------|
| **BgGPT** (todorov/bggpt) | 2B, 9B, 27B | Built on Gemma 2, trained on 85B Bulgarian tokens, superior cultural/linguistic capabilities | Primary model for Bulgarian morphology; use 9B for Mac Studio M2 |
| **OpenEuroLLM-Bulgarian** (jobautomation/OpenEuroLLM-Bulgarian) | Based on Gemma3 | Fine-tuned for fluent Bulgarian responses | Alternative if BgGPT unavailable |
| **LLaMAX3-8B-Alpaca** (mannix/llamax3-8b-alpaca) | 8B | Multilingual (100+ languages including Bulgarian) | Fallback for general-purpose tasks |

### Bulgarian Morphology Challenges

Bulgarian verbs have complex morphology:
- **Aspect:** perfective vs. imperfective (двойки)
- **Tense:** present, past aorist, past imperfect, future, conditional
- **Person/Number:** 1sg, 2sg, 3sg, 1pl, 2pl, 3pl
- **Mood:** indicative, imperative, conditional

LLMs are better than rule-based systems for handling exceptions, irregular verbs, and multi-word lemmas (e.g., "казвам се" = to be named).

**Validation strategy:** Since LLMs can hallucinate inflections, implement:
1. Count validation (verbs should have 20+ forms)
2. Cyrillic character validation
3. Duplicate detection
4. Empty field checks
5. User review workflow (ReviewStatus.PENDING)

## Mac Studio Setup Considerations

**Hardware:** Mac Studio M2 with 32GB-192GB RAM can run 7B-13B models efficiently, 27B at viable speeds with 64GB+

**Network:** Development MacBook connects to Mac Studio over LAN (already configured as mac-studio.local:11434 in application-dev.yml)

**Configuration:**
```yaml
# application-dev.yml (already exists)
ai:
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://mac-studio.local:11434}
    chat:
      options:
        model: todorov/bggpt:9b  # Bulgarian-optimized
        temperature: 0.3         # Lower for deterministic morphology
        num-ctx: 2048            # Context window
```

**Best practices:**
- Pre-pull models on Mac Studio: `ollama pull todorov/bggpt:9b`
- Configure connection timeout: 30s (LLM inference can be slow)
- Use OLLAMA_MAX_RAM environment variable to limit memory
- Monitor with Ollama metrics: `curl http://mac-studio.local:11434/api/ps`

## Open Questions

1. **BgGPT model selection: 2B vs 9B vs 27B**
   - What we know: Mac Studio M2 can run 9B efficiently, 27B requires 64GB+ RAM
   - What's unclear: Which size provides best accuracy for Bulgarian morphology without excessive latency
   - Recommendation: Start with 9B (balance of accuracy and speed), benchmark against user corrections

2. **Cache invalidation strategy when LLM improves**
   - What we know: Cached responses persist for 24 hours, but LLM may give better results after model update
   - What's unclear: How to invalidate cache when switching models or improving prompts
   - Recommendation: Use cache key that includes model name + prompt version hash

3. **Handling multi-word lemmas in cache keys**
   - What we know: Bulgarian has multi-word lemmas ("казвам се", "искам да")
   - What's unclear: How to normalize for cache keys (spaces, capitalization, order)
   - Recommendation: Use lowercase + trimmed spaces as cache key; validate normalization in tests

4. **Fallback behavior when all Bulgarian models fail**
   - What we know: Circuit breaker provides fallback, but what should fallback return?
   - What's unclear: Should we fall back to multilingual model (LLaMAX3) or return error?
   - Recommendation: Return error with detectionFailed flag; user must retry or enter manually

## Sources

### Primary (HIGH confidence)

- **Spring AI GitHub README** - https://github.com/spring-projects/spring-ai - Spring AI overview, Ollama integration, version compatibility
- **Spring AI Ollama Chat Model Documentation** - https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html - Configuration properties, auto-configuration, structured outputs, function calling
- **How to Build Async Methods with CompletableFuture in Spring** - https://oneuptime.com/blog/post/2026-01-30-spring-completablefuture-async/view - Async best practices for Spring Boot 3
- **How to Implement Circuit Breakers with Resilience4j in Spring** - https://oneuptime.com/blog/post/2026-01-25-circuit-breakers-resilience4j-spring/view - Circuit breaker configuration and patterns
- **Caffeine Cache with Spring Boot** - https://howtodoinjava.com/spring-boot/spring-boot-caffeine-cache/ - Caffeine configuration with TTL and eviction
- **Spring AI ChatClient Fluent API** - https://www.baeldung.com/spring-ai-chatclient - ChatClient builder pattern and usage
- **A Guide to Spring AI Advisors** - https://www.baeldung.com/spring-ai-advisors - Advisor patterns (RAG, memory, safeguards)
- **A Guide to Structured Output in Spring AI** - https://www.baeldung.com/spring-artificial-intelligence-structure-output - BeanOutputConverter usage and validation

### Secondary (MEDIUM confidence)

- **Best Local LLMs for Mac in 2026** - https://www.insiderllm.com/guides/best-local-llms-mac-2026/ - Mac Studio performance and model recommendations
- **BgGPT Ollama Model** - https://ollama.com/todorov/bggpt - Bulgarian language model details
- **OpenEuroLLM-Bulgarian Ollama Model** - https://ollama.com/jobautomation/OpenEuroLLM-Bulgarian - Alternative Bulgarian model
- **Spring Boot Async Executor Management with ThreadPoolTaskExecutor** - https://medium.com/trendyol-tech/spring-boot-async-executor-management-with-threadpooltaskexecutor-f493903617d - ThreadPoolTaskExecutor configuration
- **Spring Validation in the Service Layer** - https://www.baeldung.com/spring-service-layer-validation - Service layer validation patterns
- **Smarter Caching in AI Apps: Building Semantic Caching with Spring Boot and Ollama** - https://medium.com/javarevisited/smarter-caching-in-ai-apps-building-semantic-caching-with-spring-boot-and-ollama-dca47a0338e2 - Semantic caching patterns

### Tertiary (LOW confidence)

- **Transformer-Based Language Models for Bulgarian** - https://aclanthology.org/2023.ranlp-1.77.pdf - Bulgarian NLP research, marked for validation (academic paper, not production guide)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Spring AI is official Spring project, Resilience4j is de facto standard, Caffeine is Spring-recommended
- Architecture: HIGH - Patterns verified from official Spring AI docs and Spring Boot best practices (2026)
- Pitfalls: MEDIUM - Based on community experience (Medium articles, Stack Overflow), not all officially documented
- Bulgarian LLM: MEDIUM - BgGPT and OpenEuroLLM models exist on Ollama hub, but production usage is newer (2024-2025)

**Research date:** 2026-02-15
**Valid until:** ~60 days (Spring AI is stable 1.1.x, but LLM landscape evolves quickly)
