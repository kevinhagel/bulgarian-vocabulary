# Code Review — Bulgarian Vocabulary Tutor

**Date:** 2026-02-23
**Scope:** Domain model · Spring backend · Vault/security · Ollama/LLM pipeline · React frontend · Docker Compose readiness
**Method:** Four parallel specialist agents covering all major layers

---

## Executive Summary

The overall architecture is sound and well-considered. The dual-model Ollama approach (BgGPT for morphology, Qwen for sentences), the Vault-based secret management, the virtual-thread async pipeline, and the nginx + Spring Boot split are all good decisions. The codebase shows real engineering discipline.

That said, there are **5 critical bugs** that are silently broken right now — the most impactful being that `@Cacheable` and `@CircuitBreaker` are completely inoperative due to Spring AOP self-invocation, meaning every LLM call hits Ollama with no caching and no circuit protection. Fix the criticals first.

---

## Critical Issues

### C1 — `@Cacheable` + `@CircuitBreaker` Never Fire (AOP Self-Invocation)

**Severity:** CRITICAL | **Confidence:** 95%
**Files:** `llm/service/LemmaDetectionService.java:51–55`, `InflectionGenerationService.java:51–55`, `MetadataGenerationService.java:51–55`, `SentenceGenerationService.java:45–50`

The `@Async` public method in each LLM service calls the inner `@Cacheable`/`@CircuitBreaker` method via `this.method(...)`. Spring AOP cannot intercept `this.` calls — only calls through the Spring proxy. Result: **caching is completely bypassed** (every word hits Ollama on every call), and **circuit breakers never open** (Ollama failures are unprotected).

```java
// LemmaDetectionService.java
@Async("llmTaskExecutor")
public CompletableFuture<LemmaDetectionResponse> detectLemmaAsync(...) {
    LemmaDetectionResponse response = detectLemma(...);  // direct this. call — bypasses proxy
    return CompletableFuture.completedFuture(response);
}

@Cacheable(...)
@CircuitBreaker(...)
LemmaDetectionResponse detectLemma(...) {  // package-private + self-invoked = AOP dead
```

**Fix:** Extract the `@Cacheable`/`@CircuitBreaker` methods into a separate `@Service` bean, inject it, and call through the injected reference. Or use `@Lazy @Autowired` self-injection (same pattern already used correctly in `SentenceService.afterCommit()`).

---

### C2 — Actuator Endpoints Fully Unauthenticated

**Severity:** CRITICAL | **Confidence:** 95%
**Files:** `config/SecurityConfig.java:34`, `backend/src/main/resources/application.yml:121–124`

```java
.requestMatchers("/actuator/**").permitAll()
```
```yaml
management:
  endpoint:
    health:
      show-details: always
```

`/actuator/health` exposes full database, Valkey, and Ollama connectivity state to any unauthenticated caller. `/actuator/prometheus` exposes all internal metric labels including request paths and error rates. `/actuator/metrics` allows arbitrary metric enumeration.

**Fix:** Change `show-details: when-authorized`. Restrict `/actuator/prometheus` and `/actuator/metrics` behind authentication, or protect them at the nginx layer by IP allowlist. Only `/actuator/health` needs to be publicly accessible.

---

### C3 — OAuth2 Exception Leaks Rejected User's Email

**Severity:** CRITICAL | **Confidence:** 88%
**File:** `config/SecurityConfig.java:62`

```java
throw new OAuth2AuthenticationException("Email not authorized: " + email);
```

The exception message propagates into the OAuth2 error redirect's `error_description` parameter. A rejected user's email address (PII) appears in the browser's URL bar, history, and potentially referrer headers.

**Fix:**
```java
throw new OAuth2AuthenticationException("access_denied");
```

---

### C4 — `optional:vault://` Silently Swallows Vault Failures

**Severity:** CRITICAL | **Confidence:** 85%
**File:** `backend/src/main/resources/application.yml:10`

With `spring.config.import: optional:vault://`, if Vault is unreachable or the token file is missing at startup, the application continues with unresolved `${GOOGLE_CLIENT_ID}` and `${GOOGLE_CLIENT_SECRET}` placeholders. Spring will only fail later with a confusing `BeanCreationException` when OAuth2 auto-configuration tries to instantiate the client — not at startup with a clear "cannot reach Vault" message.

**Fix:** Remove `optional:` — change to `spring.config.import: vault://`. A failed Vault connection should be an immediate, clear startup failure, not a silent degradation.

---

### C5 — Audio Cache Is an Unbounded Memory Leak

**Severity:** CRITICAL | **Confidence:** 85%
**File:** `frontend/src/components/audio/AudioPlayButton.tsx:5`

```ts
const audioCache = new Map<string, string>();  // module-level, never evicted
```

This map accumulates audio URL strings for every word and sentence played, for the entire browser session, with no size cap or TTL. Additionally, `onplay`/`onended`/`onerror` callbacks set state on potentially unmounted components (navigate away during audio generation). There is no `useEffect` cleanup to stop playback or revoke the audio element.

**Fix:** Move the cache into a `useRef` with a bounded LRU eviction (e.g., cap at 50 entries). Add a `useEffect` cleanup that calls `audio.pause()` and revokes any pending object URL on unmount.

---

## High Severity Issues

### H1 — `ollama-sentence` Circuit Breaker Missing From Production Config

**Severity:** HIGH | **Confidence:** 90%
**Files:** `application.yml:91–101`, `application-dev.yml:36–43`

`SentenceGenerationService` uses `@CircuitBreaker(name = "ollama-sentence")`. This is configured in `application-dev.yml` but entirely absent from the base `application.yml`. In any non-dev deployment, Resilience4j falls back to built-in defaults (100-call window, 50% threshold, 60s open) — different from what was tested and likely not what is intended.

**Fix:** Add an `ollama-sentence` stanza to the base `application.yml`.

---

### H2 — Side Effect Inside `queryFn` in AdminDashboard

**Severity:** HIGH | **Confidence:** 85%
**File:** `frontend/src/features/admin/AdminDashboard.tsx:147`

```ts
queryFn: () => api.get<AdminStats>('/admin/stats').then(r => {
  setLastUpdated(new Date());   // ← side effect inside queryFn
  return r.data;
}),
```

TanStack Query may invoke `queryFn` multiple times (Strict Mode double-invocation, background refetch, cache replay). A `queryFn` must be a pure data-fetching function. `setLastUpdated` fires even when the result is served from cache without a real network call.

**Fix:** Derive the timestamp from `query.state.dataUpdatedAt`, or move `setLastUpdated` to the `onSuccess` option.

---

### H3 — N+1 Queries in `WordListService.getAllLists()`

**Severity:** HIGH | **Confidence:** 95%
**File:** `lists/service/WordListService.java:49–56`

```java
return listRepo.findAllOrderByName().stream()
    .map(wl -> new WordListSummaryDTO(
        wl.getId(), wl.getName(),
        listRepo.countLemmasByListId(wl.getId()),  // ← 1 query per list
        wl.getCreatedAt()))
    .toList();
```

For N word lists this fires N+1 queries. `renameList()` at line 81 has the same unnecessary count call.

**Fix:** Add a JPQL query returning `(id, name, count, createdAt)` tuples with `GROUP BY` in a single query.

---

### H4 — `existsByTextAndSource` Doesn't Match DB Uniqueness Constraint

**Severity:** HIGH | **Confidence:** 87%
**Files:** `repository/LemmaRepository.java:34`, `db/migration/V11__allow_homographs.sql`

Migration V11 changed the unique constraint from `(text, source)` to `(text, source, COALESCE(notes, ''))`. The repository still exposes `existsByTextAndSource(text, source)`, which returns `true` for two lemmas that the DB now considers distinct (same text+source, different notes). Any duplicate-guard code using this method will incorrectly block legitimate insertions.

**Fix:** Update to `existsByTextAndSourceAndNotes` (or equivalent) to match the actual DB constraint, or add a separate `countByTextAndSource` query that the caller can use for looser checks.

---

### H5 — Java Compiler Targets Release 21 Despite Declared Java 25

**Severity:** HIGH | **Confidence:** 88%
**File:** `backend/pom.xml:22,182` (confirmed by both Security and LLM reviewers)

```xml
<java.version>25</java.version>    <!-- line 22 -->
...
<release>21</release>              <!-- maven-compiler-plugin, line 182 -->
```

The declared runtime is Java 25, but bytecode is compiled to Java 21. Any Java 22–25 language features will fail to compile. The CLAUDE.md, application.yml comments, and `java.version` property all say 25 — the compiler plugin disagrees.

**Fix:** Change to `<release>${java.version}</release>` so it stays in sync automatically.

---

## Important Issues

### I1 — Two `OllamaApi` Beans Created; Registered Bean Is Unused

**File:** `config/OllamaConfig.java:39–55` | **Confidence:** 88%

`@Bean public OllamaApi ollamaApi(...)` creates and registers one instance. Then `ollamaChatModel(...)` constructs a *second* independent `OllamaApi` with its own `RestClient` and wires that into the model — ignoring the registered bean entirely. The registered bean is wasted and any auto-configuration injecting `OllamaApi` by type gets the wrong instance.

**Fix:** Add `OllamaApi ollamaApi` as a parameter to `ollamaChatModel(...)` and use the injected bean instead of constructing a new one inline.

---

### I2 — BgGPT Context Window (2048) Too Small for Full Verb Paradigms

**File:** `application.yml:76`, `application-dev.yml:22` | **Confidence:** 80%

The `bggpt-vocab` Modelfile caps context at 2048 tokens. The inflection generation prompt alone is ~34 lines, plus a full Bulgarian verb conjugation (25–30 forms) in the response. Complex verbs risk hitting the ceiling mid-JSON, causing `JsonProcessingException` — which the circuit breaker (when C1 is fixed) will count as a failure and potentially open prematurely. The Qwen client for sentence generation correctly uses 4096 tokens.

**Fix:** Increase `num-ctx` to at least 4096 in the BgGPT Modelfile. The M4 Max GPU has sufficient VRAM headroom.

---

### I3 — Temperature 0.3 Applied to Deterministic Morphological Tasks

**File:** `application.yml:77`, `application-dev.yml:23` | **Confidence:** 80%

Temperature 0.3 is applied globally to the BgGPT client, which handles lemma detection and inflection generation — both tasks with exactly one correct answer. Non-zero temperature introduces randomness into deterministic linguistic outputs. The Qwen sentence client correctly uses 0.7 for creative generation.

**Fix:** Set the BgGPT client temperature to 0.0. If metadata generation needs a different temperature, split it into its own `ChatClient` bean.

---

### I4 — `translations` Cache Not Registered in `CacheConfig`

**Files:** `llm/config/CacheConfig.java:45–49`, `llm/translation/TranslationService.java:48` | **Confidence:** 90%

`CacheConfig` registers four named caches. `TranslationService` uses `@Cacheable(value = "translations")`, which is not among them. It falls back to `RedisCacheManager`'s dynamic default — works today, but silently breaks if `disableCacheCreation()` is ever added, and the intent is opaque.

**Fix:** Add `"translations"` to the `cacheConfigurations` map in `CacheConfig`.

---

### I5 — `@NotBlank` on Nullable `lemma` Field Contradicts `failed()` Factory

**File:** `llm/dto/LemmaDetectionResponse.java:11,20` | **Confidence:** 85%

```java
@NotBlank private final String lemma;   // annotated non-null
public static LemmaDetectionResponse failed(String wordForm) {
    return new LemmaDetectionResponse(wordForm, null, null, true);  // lemma = null
}
```

The `failed()` factory explicitly passes `null` for a field annotated `@NotBlank`. The immediate call path checks `detectionFailed()` first so it doesn't crash, but the annotation is misleading and would cause confusion (or failures) if validation were run on failed responses, e.g., after a cache round-trip deserialisation.

**Fix:** Remove `@NotBlank` from `lemma`. Add a conditional constraint or `@Nullable` annotation to document the contract accurately.

---

### I6 — CSRF Disabled for All `/api/**`

**File:** `config/SecurityConfig.java:49–51` | **Confidence:** 85%

```java
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
```

This disables CSRF protection for every API endpoint including `/api/admin/**` and `/api/auth/logout`. For a cookie-session SPA, the correct approach is `CookieCsrfTokenRepository.withHttpOnlyFalse()` — the frontend reads the token from a cookie and sends it as `X-XSRF-TOKEN` on write requests.

**Fix:** Replace the blanket ignore with `CookieCsrfTokenRepository.withHttpOnlyFalse()` and configure the frontend accordingly. At minimum, protect the admin endpoints.

---

### I7 — `allowedEmails` Comparison Not Normalised on Both Sides

**File:** `config/SecurityConfig.java:61,67` | **Confidence:** 80%

```java
if (!allowedEmails.contains(email.toLowerCase()))  // input normalised
...
if (email.equalsIgnoreCase(adminEmail))            // comparison is case-insensitive
```

The allowlist check normalises the incoming email to lowercase before `contains()`, but doesn't guarantee the Vault-sourced list values are lowercase. If a Vault value contains uppercase (e.g., `Kevin.Hagel@gmail.com`), a legitimate user is blocked.

**Fix:** Normalise the injected list to lowercase in a `@PostConstruct` method, or use a `TreeSet` with case-insensitive comparator.

---

### I8 — `defaultSuccessUrl("/", true)` Redirects to Backend Root After OAuth2 Login

**File:** `config/SecurityConfig.java:39` | **Confidence:** 83%

After Google OAuth2 login, Spring redirects the browser to `/` on the backend port, not back to the React SPA at `hagelbg.dyndns-ip.com`. This currently works because nginx probably catches the request, but the intent is fragile — the `true` flag forces the redirect regardless of the saved request URL, breaking deep-link restoration after login.

**Fix:** Use `defaultSuccessUrl("https://hagelbg.dyndns-ip.com", true)` or implement a custom `AuthenticationSuccessHandler` that redirects to the frontend origin.

---

### I9 — pgAdmin and Redis Commander Have Weak Default Passwords

**File:** `docker-compose.yml:47,89` | **Confidence:** 82%

```yaml
PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_PASSWORD:-admin}
HTTP_PASSWORD=${REDIS_COMMANDER_PASSWORD:-admin123}
```

If these aren't set in `.env`, the services start with `admin`/`admin123`. Both have full database/cache access. They are LAN-accessible at `192.168.1.10:5050` and `:8082`.

**Fix:** Use the `:?` required syntax (like `POSTGRES_PASSWORD` already does) to force these to be set, or confirm they are already explicitly set in `.env`.

---

### I10 — `containsCyrillic()` Regex Incomplete and Recompiled Per Call

**File:** `llm/validation/LlmOutputValidator.java:201–206` | **Confidence:** 82%

```java
return text.matches(".*[а-яА-Я].*");
```

`String.matches()` compiles a new `Pattern` on every invocation (called per inflection entry). The range `[а-яА-Я]` also misses some edge-case Cyrillic characters (e.g., `ї`, `ё`). The Unicode property escape is both more correct and pre-compilable.

**Fix:**
```java
private static final Pattern CYRILLIC = Pattern.compile(".*\\p{InCyrillic}.*", Pattern.DOTALL);
// then: return CYRILLIC.matcher(text).matches();
```

---

### I11 — `ProgressService` and `StudySessionService` Load Full ID List for `.size()`

**Files:** `study/service/ProgressService.java:50`, `study/service/StudySessionService.java:193` | **Confidence:** 88%

```java
long newCards = srsStateRepository.findLemmaIdsWithoutSrsState().size();
```

This materialises a full `List<Long>` in the JVM just to count it. A scalar `COUNT` query would do the same work at the DB level.

**Fix:** Add a `countLemmaIdsWithoutSrsState()` repository method and use it in both places.

---

### I12 — Triple DB Round-Trips in `updateReviewStatus` and `flagVocabulary`

**File:** `service/VocabularyService.java:213–226,278–289` | **Confidence:** 85%

Both methods load the lemma with `findById()`, save it, then reload with `findByIdWithInflections()` to build the response DTO — three queries where one or two suffice.

**Fix:** Use `findByIdWithInflections()` in the initial load; the entity is already dirty-tracked after `save()`, so the final reload is unnecessary.

---

### I13 — `any` Types Bypass Strict TypeScript in Form and List

**Files:** `features/vocabulary/components/VocabularyForm.tsx:9,30,31,36,39`, `VocabularyList.tsx:76–79,222` | **Confidence:** 85%

`tsconfig.app.json` has `"strict": true`, but `VocabularyForm` uses `onSubmit: (data: any)` and multiple `as any` casts; `VocabularyList` casts the query result to `any` to handle the union of `PaginatedResponse<LemmaResponseDTO>` and `LemmaResponseDTO[]`. Backend DTO changes will not be caught by the compiler.

**Fix:** Define a `SearchResult = PaginatedResponse<LemmaResponseDTO> | LemmaResponseDTO[]` discriminated union with a type guard. Type the form submit handler against the actual Zod-inferred type.

---

### I14 — `formatEnumLabel` Duplicated in Three Files

**Files:** `VocabularyCard.tsx:14`, `VocabularyDetail.tsx:21`, `VocabularyFilters.tsx:17` | **Confidence:** 85%

Identical implementation copy-pasted into three components. Should live in `utils/` alongside `grammarFormatter.ts`.

---

### I15 — `ReactQueryDevtools` Unconditionally Imported in Production Build

**File:** `frontend/src/main.tsx:5,13` | **Confidence:** 80%

`ReactQueryDevtools` is in `devDependencies` but unconditionally imported. Vite's tree-shaking may remove the panel render in production, but the package code is still bundled. For a private single-user app the impact is minor, but it's not best practice.

**Fix:** Wrap in a conditional: `{import.meta.env.DEV && <ReactQueryDevtools />}`.

---

## Low Severity / Code Quality

### L1 — `List<Lemma>` for `@ManyToMany` Should Be `Set<Lemma>`
**File:** `lists/domain/WordList.java:25` — Using `List` for a `@ManyToMany` association is non-standard JPA practice and can cause Hibernate warnings with pagination. The DB constraint prevents actual duplicates. Change to `Set<Lemma>`.

### L2 — Redundant `existsById` Before `deleteById`
**File:** `service/VocabularyService.java:197–203` — `existsById` + `deleteById` is three queries. Use `findById()` → throw if absent → `delete(entity)` for two.

### L3 — Missing `equals`/`hashCode` on `WordList`, `StudySession`, `StudyReview`
**Files:** `lists/domain/WordList.java`, `study/domain/StudySession.java`, `study/domain/StudyReview.java` — Inconsistent with other entities that correctly implement them. Low risk currently but worth making consistent.

### L4 — `useVocabularyDetail` Can Construct `/vocabulary/null` URL
**File:** `features/vocabulary/api/useVocabularyDetail.ts:16` — The `enabled: id !== null` guard should prevent this, but a defensive check inside `queryFn` costs nothing.

### L5 — `NOT IN (subquery)` in `SrsStateRepository.findLemmaIdsWithoutSrsState()`
**File:** `study/repository/SrsStateRepository.java:22–29` — `NOT IN` with a subquery degrades at scale; `NOT EXISTS` or `LEFT JOIN ... WHERE IS NULL` is more efficient. Fine for current dataset size.

---

## Docker Compose / Frontend Containerisation Assessment

**Verdict: Not needed right now, and current setup is correct.**

The current architecture (nginx static serve of `frontend/dist/` + Spring Boot container) is production-ready. The API layer is clean:

- `frontend/src/lib/api.ts:5` uses `VITE_API_BASE_URL || '/api'` — no hardcoded `localhost`
- All API calls use relative paths — work anywhere nginx proxies `/api/` to backend
- nginx config correctly handles SPA fallback, TLS, `/oauth2/` proxying, and LAN-only admin restriction

There is no `frontend/Dockerfile` and none is needed while the current nginx approach is in place. If you ever want to containerise for portability, the only nginx config change needed is swapping `root /Users/.../dist` for `proxy_pass http://frontend:80` — no frontend code changes required. The OAuth2 path (`/oauth2/authorization/google` in `LoginPage.tsx`) continues to work as long as nginx (or equivalent) proxies it to the backend.

---

## Model Selection Assessment

The dual-model approach is well-reasoned:

- **BgGPT (todorov/bggpt:9B-IT-v1.0.Q4_K_M)** for lemma detection, inflection, and metadata — correct choice. Built on Gemma 2 with ~85B Bulgarian training tokens. Substantially stronger for Bulgarian morphology than llama3.x, mistral:7b, or raw gemma2:9b.
- **Qwen 2.5:14B** for sentence generation — correct choice. Strong multilingual capability, sufficient capacity for natural Bulgarian sentence construction, 0.7 temperature appropriate for creative output.
- **edge-tts** for TTS — appropriate for the use case.

The model choices are good. The improvements are in configuration (temperature, context window, compiler target) not model selection.

---

## Priority Fix Order

| Priority | Issue | Effort |
|----------|-------|--------|
| 1 | **C1** — Fix AOP self-invocation so `@Cacheable`/`@CircuitBreaker` actually work | Medium |
| 2 | **C2** — Restrict actuator endpoints; set `show-details: when-authorized` | Small |
| 3 | **C3** — Remove email from OAuth2 exception message | Trivial |
| 4 | **C4** — Remove `optional:` from `vault://` import | Trivial |
| 5 | **C5** — Bound the frontend audio cache; add unmount cleanup | Small |
| 6 | **H1** — Add `ollama-sentence` circuit breaker config to `application.yml` | Trivial |
| 7 | **H5** — Fix `<release>21</release>` → `<release>${java.version}</release>` | Trivial |
| 8 | **H4** — Align `existsByTextAndSource` with V11 migration uniqueness constraint | Small |
| 9 | **I1** — Remove duplicate `OllamaApi` construction in `OllamaConfig` | Small |
| 10 | **I2** — Increase BgGPT context window to 4096 | Small |
| 11 | **I3** — Set BgGPT temperature to 0.0 | Trivial |
| 12 | **I4** — Register `translations` cache in `CacheConfig` | Trivial |
| 13 | **H3** — Fix N+1 in `getAllLists()` | Small |
| 14 | **I11** — Replace `findLemmaIdsWithoutSrsState().size()` with COUNT query | Small |
| 15 | Remaining IMPORTANT and LOW items | Various |

---

*Generated by 4 parallel specialist review agents · 2026-02-23*
