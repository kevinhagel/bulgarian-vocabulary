# Architecture Research

**Domain:** Vocabulary Learning Application (Bulgarian Language)
**Researched:** 2026-02-15
**Confidence:** HIGH

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                          │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ Vocabulary   │  │  Study       │  │  Text        │              │
│  │ Management   │  │  Interface   │  │  Analysis    │              │
│  │ (React)      │  │  (Flashcards)│  │  (Paste)     │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│         │                 │                 │                       │
│         └─────────────────┴─────────────────┘                       │
│                           │                                          │
│                    State Management                                  │
│                  (React Query + Zustand)                             │
│                           │                                          │
├───────────────────────────┴──────────────────────────────────────────┤
│                        REST API LAYER                                │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ Vocabulary   │  │  Study       │  │  Text        │              │
│  │ Controller   │  │  Controller  │  │  Controller  │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│         │                 │                 │                       │
├─────────┴─────────────────┴─────────────────┴───────────────────────┤
│                        SERVICE LAYER                                 │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ Lemma        │  │  LLM         │  │  TTS         │              │
│  │ Service      │  │  Service     │  │  Service     │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│         │                 │                 │                       │
├─────────┴─────────────────┼─────────────────┼───────────────────────┤
│                           │                 │                       │
│      PERSISTENCE LAYER    │  INTEGRATION    │  INTEGRATION          │
├───────────────────────────┤─────────────────┤───────────────────────┤
│  ┌──────────────────────┐ │  ┌───────────┐  │  ┌─────────────┐     │
│  │ Spring Data JPA      │ │  │  Ollama   │  │  │  Edge TTS   │     │
│  │ Repositories         │ │  │  (LLM)    │  │  │  (Audio)    │     │
│  └──────┬───────────────┘ │  └───────────┘  │  └─────────────┘     │
│         │                 │                 │                       │
│  ┌──────┴───────────────┐ │   (Networked)   │   (Local/Network)    │
│  │    PostgreSQL DB     │ │                 │                       │
│  │  ┌────────────────┐  │ │                 │                       │
│  │  │ lemmas         │  │ │                 │                       │
│  │  │ inflections    │  │ │                 │                       │
│  │  │ categories     │  │ │                 │                       │
│  │  │ word_lists     │  │ │                 │                       │
│  │  │ study_sessions │  │ │                 │                       │
│  │  │ audio_cache    │  │ │                 │                       │
│  │  └────────────────┘  │ │                 │                       │
│  └──────────────────────┘ │                 │                       │
└────────────────────────────┴─────────────────┴───────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| **Vocabulary Management UI** | Word entry forms, lemma editing, list management | React components with form libraries (React Hook Form), validation with Zod/Yup |
| **Study Interface** | Flashcard display, audio playback controls, spaced repetition logic | React components with audio HTML5 API, state-driven flashcard transitions |
| **Text Analysis UI** | Text paste interface, word extraction display, lemma review | React components with text parsing, word highlighting, selection UI |
| **REST Controllers** | HTTP request handling, input validation, response formatting | Spring Boot @RestController with @RequestMapping, Pageable support |
| **Lemma Service** | Business logic for vocabulary CRUD, lemma detection coordination, metadata management | Spring @Service with transaction boundaries, orchestrates LLM + persistence |
| **LLM Service** | Ollama integration, prompt engineering, response parsing, caching, retries | Spring @Service with WebClient/RestTemplate, async processing (@Async), circuit breaker |
| **TTS Service** | Edge TTS integration, audio generation, caching, format management | Spring @Service with WebSocket/HTTP client, file storage, cache management |
| **Spring Data JPA Repositories** | Data access abstraction, query methods, pagination | JpaRepository interfaces with derived query methods and @Query annotations |
| **PostgreSQL Database** | Persistent storage of vocabulary, inflections, audio metadata, study progress | Relational database with foreign keys, indexes, JSONB for flexible metadata |

## Recommended Project Structure

```
bulgarian-vocabulary/
├── backend/                      # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/vocab/
│   │   │   │       ├── domain/              # JPA entities
│   │   │   │       │   ├── Lemma.java
│   │   │   │       │   ├── Inflection.java
│   │   │   │       │   ├── Category.java
│   │   │   │       │   ├── WordList.java
│   │   │   │       │   └── StudySession.java
│   │   │   │       ├── repository/          # Spring Data JPA repos
│   │   │   │       │   ├── LemmaRepository.java
│   │   │   │       │   ├── InflectionRepository.java
│   │   │   │       │   └── WordListRepository.java
│   │   │   │       ├── service/             # Business logic
│   │   │   │       │   ├── LemmaService.java
│   │   │   │       │   ├── LLMService.java
│   │   │   │       │   ├── TTSService.java
│   │   │   │       │   ├── TextAnalysisService.java
│   │   │   │       │   └── StudyService.java
│   │   │   │       ├── controller/          # REST endpoints
│   │   │   │       │   ├── VocabularyController.java
│   │   │   │       │   ├── StudyController.java
│   │   │   │       │   └── TextController.java
│   │   │   │       ├── dto/                 # Data transfer objects
│   │   │   │       │   ├── request/
│   │   │   │       │   │   ├── LemmaRequest.java
│   │   │   │       │   │   └── TextAnalysisRequest.java
│   │   │   │       │   └── response/
│   │   │   │       │       ├── LemmaResponse.java
│   │   │   │       │       └── WordExtractionResponse.java
│   │   │   │       ├── integration/         # External service clients
│   │   │   │       │   ├── ollama/
│   │   │   │       │   │   ├── OllamaClient.java
│   │   │   │       │   │   ├── OllamaRequest.java
│   │   │   │       │   │   └── OllamaResponse.java
│   │   │   │       │   └── tts/
│   │   │   │       │       ├── EdgeTTSClient.java
│   │   │   │       │       └── TTSAudioResult.java
│   │   │   │       ├── config/              # Spring configuration
│   │   │   │       │   ├── WebClientConfig.java
│   │   │   │       │   ├── AsyncConfig.java
│   │   │   │       │   ├── CacheConfig.java
│   │   │   │       │   └── SecurityConfig.java
│   │   │   │       └── util/                # Helper utilities
│   │   │   │           ├── BulgarianTextUtils.java
│   │   │   │           └── PromptTemplates.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── db/migration/            # Flyway migrations
│   │   │           ├── V1__create_schema.sql
│   │   │           ├── V2__seed_reference_vocab.sql
│   │   │           └── V3__add_audio_cache.sql
│   │   └── test/
│   │       └── java/
│   │           └── com/example/vocab/
│   │               ├── service/
│   │               ├── controller/
│   │               └── integration/
│   └── pom.xml                              # Maven dependencies
│
├── frontend/                     # React application
│   ├── src/
│   │   ├── components/                      # Reusable UI components
│   │   │   ├── common/
│   │   │   │   ├── Button.tsx
│   │   │   │   ├── Input.tsx
│   │   │   │   └── AudioPlayer.tsx
│   │   │   ├── vocabulary/
│   │   │   │   ├── LemmaForm.tsx
│   │   │   │   ├── LemmaCard.tsx
│   │   │   │   ├── InflectionList.tsx
│   │   │   │   └── VocabularyTable.tsx
│   │   │   ├── study/
│   │   │   │   ├── Flashcard.tsx
│   │   │   │   ├── FlashcardControls.tsx
│   │   │   │   └── StudyProgress.tsx
│   │   │   └── text/
│   │   │       ├── TextInput.tsx
│   │   │       ├── WordExtraction.tsx
│   │   │       └── LemmaReview.tsx
│   │   ├── pages/                           # Page-level components
│   │   │   ├── VocabularyPage.tsx
│   │   │   ├── StudyPage.tsx
│   │   │   ├── TextAnalysisPage.tsx
│   │   │   └── WordListsPage.tsx
│   │   ├── hooks/                           # Custom React hooks
│   │   │   ├── useAudio.ts
│   │   │   ├── useLemmas.ts
│   │   │   ├── useStudySession.ts
│   │   │   └── useTextAnalysis.ts
│   │   ├── api/                             # Backend API client
│   │   │   ├── client.ts                    # Axios/fetch setup
│   │   │   ├── vocabulary.ts
│   │   │   ├── study.ts
│   │   │   └── text.ts
│   │   ├── store/                           # State management
│   │   │   ├── vocabulary.ts                # Zustand store for vocab state
│   │   │   └── study.ts                     # Zustand store for study state
│   │   ├── types/                           # TypeScript types
│   │   │   ├── lemma.ts
│   │   │   ├── inflection.ts
│   │   │   └── study.ts
│   │   ├── utils/                           # Helper utilities
│   │   │   ├── audio.ts
│   │   │   └── text.ts
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   └── index.css                        # Tailwind imports
│   ├── package.json
│   ├── vite.config.ts
│   └── tailwind.config.js
│
├── docker-compose.yml            # PostgreSQL container
└── README.md
```

### Structure Rationale

- **backend/domain/**: Entity classes map directly to database tables, representing the core linguistic model (Lemma, Inflection, Category)
- **backend/service/**: Business logic layer isolates domain rules and orchestrates between repositories and external services (LLM, TTS)
- **backend/integration/**: Dedicated packages for Ollama and Edge TTS clients keep external service contracts separate and testable
- **backend/dto/**: Separate request/response objects from entities prevent over-fetching and enable API versioning without breaking domain model
- **db/migration/**: Flyway migrations provide versioned schema evolution and seed data (reference vocabulary)
- **frontend/components/**: Organized by feature domain (vocabulary, study, text) rather than technical role (containers, presentational)
- **frontend/hooks/**: Custom hooks encapsulate data fetching (React Query), audio playback, and study session logic
- **frontend/store/**: Zustand stores for client-side state that doesn't belong in the server cache (UI state, study session progress)
- **frontend/api/**: Centralized API client with React Query integration for server state management

## Architectural Patterns

### Pattern 1: Repository Pattern with Spring Data JPA

**What:** Abstract data access behind repository interfaces, letting Spring Data JPA generate implementations from method names and @Query annotations.

**When to use:** All database operations. Spring Data JPA provides derived queries, pagination, sorting out of the box.

**Trade-offs:**
- Pro: Minimal boilerplate, type-safe queries, built-in pagination/sorting
- Pro: Easy to test by mocking repository interfaces
- Con: Complex queries may require @Query or Specification API
- Con: N+1 query problems require explicit @EntityGraph or JOIN FETCH

**Example:**
```java
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    // Derived query method - Spring generates SQL
    Page<Lemma> findBySourceAndCategory(
        String source,
        Category category,
        Pageable pageable
    );

    // Custom query for complex search
    @Query("""
        SELECT l FROM Lemma l
        LEFT JOIN FETCH l.inflections
        WHERE l.lemmaText LIKE %:search%
        OR l.translation LIKE %:search%
        """)
    List<Lemma> searchLemmas(@Param("search") String search);

    // Find lemmas needing audio generation
    @Query("SELECT l FROM Lemma l WHERE l.audioGenerated = false")
    List<Lemma> findLemmasWithoutAudio();
}
```

### Pattern 2: Async Service Integration with Circuit Breaker

**What:** LLM calls are expensive and slow. Execute them asynchronously with retries, timeouts, and circuit breaker to prevent cascading failures.

**When to use:** All Ollama LLM integration calls, especially during text analysis (multiple LLM calls per paste).

**Trade-offs:**
- Pro: Prevents blocking the request thread, improves throughput
- Pro: Circuit breaker prevents overwhelming failed Ollama service
- Pro: Graceful degradation when LLM unavailable
- Con: Adds complexity with CompletableFuture/reactive types
- Con: Error handling becomes more complex (need fallback strategies)

**Example:**
```java
@Service
public class LLMService {

    private final WebClient ollamaClient;
    private final CircuitBreaker circuitBreaker;

    @Async
    @Retry(name = "ollama", fallbackMethod = "fallbackLemmaDetection")
    @CircuitBreaker(name = "ollama")
    public CompletableFuture<LemmaMetadata> detectLemma(String word) {
        OllamaRequest request = OllamaRequest.builder()
            .model("llama3.2")
            .prompt(PromptTemplates.lemmaDetection(word))
            .build();

        return ollamaClient.post()
            .uri("/api/generate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OllamaResponse.class)
            .map(this::parseLemmaMetadata)
            .toFuture()
            .exceptionally(ex -> {
                log.error("LLM call failed for word: {}", word, ex);
                throw new LLMException("Lemma detection failed", ex);
            });
    }

    // Fallback when circuit is open
    private CompletableFuture<LemmaMetadata> fallbackLemmaDetection(
        String word,
        Exception ex
    ) {
        log.warn("Using fallback for word: {}", word);
        // Return minimal metadata or throw user-friendly error
        return CompletableFuture.completedFuture(
            LemmaMetadata.minimal(word)
        );
    }
}
```

### Pattern 3: Cache-Aside Pattern for LLM Responses and Audio

**What:** Cache expensive LLM responses and generated TTS audio files to reduce latency and external API calls.

**When to use:**
- LLM responses for identical prompts (lemma detection, inflection generation)
- Generated TTS audio files (same text always produces same audio)

**Trade-offs:**
- Pro: Massive performance improvement (30s → <1s for cached LLM responses)
- Pro: Reduces load on Ollama and Edge TTS services
- Pro: Improves user experience with instant audio playback
- Con: Cache invalidation complexity (when to regenerate?)
- Con: Storage cost for audio files (mitigated by serving from disk)

**Example:**
```java
@Service
public class TTSService {

    private final EdgeTTSClient ttsClient;
    private final Path audioStoragePath;

    @Cacheable(value = "ttsAudio", key = "#text + '_' + #voice")
    public AudioResult generateAudio(String text, String voice) {
        String cacheKey = DigestUtils.sha256Hex(text + voice);
        Path audioFile = audioStoragePath.resolve(cacheKey + ".mp3");

        // Check disk cache first
        if (Files.exists(audioFile)) {
            return AudioResult.fromFile(audioFile);
        }

        // Generate new audio via Edge TTS
        byte[] audioData = ttsClient.synthesize(text, voice);
        Files.write(audioFile, audioData);

        return AudioResult.fromBytes(audioData, audioFile);
    }
}

// Spring Cache configuration
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "ttsAudio",      // TTS audio cache
            "llmResponses"   // LLM response cache
        );
    }
}
```

### Pattern 4: React Query for Server State Management

**What:** Use React Query (TanStack Query) to manage all server data (vocabulary, study sessions) with automatic caching, background refetching, and optimistic updates.

**When to use:** All API calls from React frontend. Separates server state from client UI state.

**Trade-offs:**
- Pro: Automatic background refetching keeps data fresh
- Pro: Built-in loading/error states reduce boilerplate
- Pro: Optimistic updates for better UX (update UI before server confirms)
- Con: Learning curve for cache invalidation patterns
- Con: Need to carefully design query keys for cache consistency

**Example:**
```typescript
// Custom hook for lemma management
export function useLemmas(filters: LemmaFilters) {
    const queryClient = useQueryClient();

    // Fetch lemmas with automatic caching
    const { data, isLoading, error } = useQuery({
        queryKey: ['lemmas', filters],
        queryFn: () => vocabularyApi.fetchLemmas(filters),
        staleTime: 5 * 60 * 1000, // 5 minutes
    });

    // Create new lemma with optimistic update
    const createMutation = useMutation({
        mutationFn: vocabularyApi.createLemma,
        onMutate: async (newLemma) => {
            // Cancel outgoing refetches
            await queryClient.cancelQueries({ queryKey: ['lemmas'] });

            // Snapshot previous value
            const previous = queryClient.getQueryData(['lemmas', filters]);

            // Optimistically update cache
            queryClient.setQueryData(['lemmas', filters], (old) => {
                return [...old, { ...newLemma, id: 'temp' }];
            });

            return { previous };
        },
        onError: (err, newLemma, context) => {
            // Rollback on error
            queryClient.setQueryData(['lemmas', filters], context.previous);
        },
        onSuccess: () => {
            // Invalidate to refetch fresh data
            queryClient.invalidateQueries({ queryKey: ['lemmas'] });
        },
    });

    return {
        lemmas: data?.items ?? [],
        totalCount: data?.total ?? 0,
        isLoading,
        error,
        createLemma: createMutation.mutate,
    };
}
```

### Pattern 5: Zustand for Client UI State

**What:** Use Zustand for lightweight global state that doesn't belong on the server (flashcard current index, audio playback state, UI flags).

**When to use:** Study session progress, audio player state, UI preferences, temporary form state.

**Trade-offs:**
- Pro: Minimal boilerplate compared to Redux
- Pro: No provider wrapping needed
- Pro: DevTools support for debugging
- Con: Less ecosystem/middleware than Redux
- Con: Need discipline to avoid putting server data in Zustand (use React Query instead)

**Example:**
```typescript
import { create } from 'zustand';

interface StudyState {
    currentCardIndex: number;
    showAnswer: boolean;
    audioPlaying: boolean;

    nextCard: () => void;
    previousCard: () => void;
    toggleAnswer: () => void;
    setAudioPlaying: (playing: boolean) => void;
    reset: () => void;
}

export const useStudyStore = create<StudyState>((set) => ({
    currentCardIndex: 0,
    showAnswer: false,
    audioPlaying: false,

    nextCard: () => set((state) => ({
        currentCardIndex: state.currentCardIndex + 1,
        showAnswer: false,
    })),

    previousCard: () => set((state) => ({
        currentCardIndex: Math.max(0, state.currentCardIndex - 1),
        showAnswer: false,
    })),

    toggleAnswer: () => set((state) => ({
        showAnswer: !state.showAnswer,
    })),

    setAudioPlaying: (playing) => set({ audioPlaying: playing }),

    reset: () => set({
        currentCardIndex: 0,
        showAnswer: false,
        audioPlaying: false,
    }),
}));

// Usage in component
function Flashcard({ lemma }: Props) {
    const {
        showAnswer,
        toggleAnswer,
        nextCard
    } = useStudyStore();

    return (
        <div onClick={toggleAnswer}>
            <h2>{lemma.lemmaText}</h2>
            {showAnswer && <p>{lemma.translation}</p>}
            <button onClick={nextCard}>Next</button>
        </div>
    );
}
```

## Data Flow

### Request Flow: User Enters Word

```
[User enters "пиша" in form]
    ↓
[VocabularyForm.tsx] → POST /api/vocabulary
    ↓
[VocabularyController] → validate input
    ↓
[LemmaService] → orchestrate lemma creation
    ↓ (async)
[LLMService] → detect lemma metadata
    ↓
[Ollama LLM] → generate:
    - lemma: "пиша"
    - part of speech: "verb"
    - inflections: ["пиша", "пишеш", "пише", ...]
    - category: "writing"
    - difficulty: "beginner"
    ↓
[LemmaService] → save to DB via LemmaRepository
    ↓
[TTSService] → queue audio generation (async)
    ↓
[Edge TTS] → generate MP3 for each inflection
    ↓
[TTSService] → save audio files to disk + update DB
    ↓
[LemmaService] → return LemmaResponse
    ↓
[VocabularyController] → 201 Created with full lemma
    ↓
[React Query] → update cache + invalidate 'lemmas' query
    ↓
[VocabularyTable.tsx] → re-render with new lemma
```

### Request Flow: User Pastes Text

```
[User pastes Bulgarian text]
    ↓
[TextAnalysisPage.tsx] → POST /api/text/analyze
    ↓
[TextController] → validate text
    ↓
[TextAnalysisService] → extract words (regex)
    ↓
[LLMService] → batch lemma detection (parallel async calls)
    ↓
[Ollama LLM] → detect lemma for each word
    ↓
[TextAnalysisService] → deduplicate + check existing lemmas
    ↓
[LemmaRepository] → query DB for existing lemmas
    ↓
[TextAnalysisService] → return WordExtractionResponse
    - words: [{ word, lemma, exists }]
    ↓
[WordExtraction.tsx] → display review UI
    ↓
[User clicks "Add to Vocabulary"]
    ↓
[LemmaReview.tsx] → POST /api/vocabulary (for each new lemma)
    ↓
[Vocabulary creation flow] (see above)
```

### Request Flow: Flashcard Study Mode

```
[User clicks "Study"]
    ↓
[StudyPage.tsx] → GET /api/study/flashcards?listId=123
    ↓
[StudyController] → fetch word list + lemmas
    ↓
[WordListRepository] → load word list with lemmas
    ↓
[StudyService] → apply spaced repetition algorithm
    ↓
[StudyController] → return FlashcardResponse[]
    ↓
[React Query] → cache flashcards in 'flashcards' query
    ↓
[StudyPage.tsx] → initialize Zustand study state
    ↓
[Flashcard.tsx] → display current card
    ↓
[User clicks card] → toggleAnswer()
    ↓
[Zustand] → update showAnswer = true
    ↓
[User clicks audio icon]
    ↓
[AudioPlayer.tsx] → GET /api/audio/{lemmaId}/{inflectionIndex}
    ↓
[TTSService] → return cached audio file
    ↓
[AudioPlayer.tsx] → play audio via HTML5 Audio API
    ↓
[User clicks "Next"]
    ↓
[Zustand] → nextCard() → currentCardIndex++
    ↓
[Flashcard.tsx] → re-render with next card
```

### State Management Flow

```
┌─────────────────────────────────────────────────────────┐
│                     REACT FRONTEND                       │
│                                                          │
│  ┌────────────────────┐     ┌──────────────────────┐    │
│  │  React Query       │     │  Zustand Store       │    │
│  │  (Server State)    │     │  (Client State)      │    │
│  ├────────────────────┤     ├──────────────────────┤    │
│  │ - lemmas           │     │ - currentCardIndex   │    │
│  │ - wordLists        │     │ - showAnswer         │    │
│  │ - flashcards       │     │ - audioPlaying       │    │
│  │ - categories       │     │ - UI preferences     │    │
│  └────────┬───────────┘     └──────────────────────┘    │
│           │                                              │
│           │ (fetch/mutate)                               │
│           ↓                                              │
│  ┌────────────────────────────────────────────────┐     │
│  │           API Client (Axios/Fetch)              │     │
│  └────────────────────┬───────────────────────────┘     │
│                       │                                  │
└───────────────────────┼──────────────────────────────────┘
                        │ HTTP
                        ↓
┌───────────────────────────────────────────────────────────┐
│                  SPRING BOOT BACKEND                       │
│                                                           │
│  ┌─────────────────────────────────────────────────┐     │
│  │              REST Controllers                    │     │
│  └─────────────────┬───────────────────────────────┘     │
│                    │                                      │
│  ┌─────────────────┴───────────────────────────────┐     │
│  │                Services                          │     │
│  │  ┌────────────────┐    ┌────────────────────┐   │     │
│  │  │ Spring Cache   │    │ Async @Scheduled   │   │     │
│  │  │ (LLM/TTS)      │    │ (background audio) │   │     │
│  │  └────────────────┘    └────────────────────┘   │     │
│  └─────────────────┬───────────────────────────────┘     │
│                    │                                      │
│  ┌─────────────────┴───────────────────────────────┐     │
│  │         Spring Data JPA Repositories             │     │
│  └─────────────────┬───────────────────────────────┘     │
│                    │                                      │
│  ┌─────────────────┴───────────────────────────────┐     │
│  │              PostgreSQL Database                 │     │
│  └──────────────────────────────────────────────────┘     │
└───────────────────────────────────────────────────────────┘
```

### Key Data Flows

1. **Vocabulary Entry Flow:** User input → Controller validation → LLM async processing → Database save → TTS async processing → Response with metadata → React Query cache update
2. **Text Analysis Flow:** Bulk text → Word extraction → Parallel LLM calls → Deduplication → Review UI → User confirmation → Batch vocabulary creation
3. **Study Flow:** Query flashcards → Apply spaced repetition → Cache in React Query → Display with Zustand UI state → Audio playback from cached files
4. **Background Processing:** Scheduled tasks check for lemmas without audio → Generate audio asynchronously → Update database when complete

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-100 users | Monolithic Spring Boot + React; single PostgreSQL instance; Ollama on dedicated hardware (Mac Studio); file-based audio cache |
| 100-1k users | Add database connection pooling (HikariCP); enable Spring @Async thread pool tuning; implement Redis for distributed caching (replace Spring Cache); add CDN for audio file serving |
| 1k-10k users | Separate LLM service into dedicated microservice with queue (RabbitMQ/Kafka) for async processing; move audio files to object storage (S3-compatible); add database read replicas; implement rate limiting per user |
| 10k+ users | Horizontal scaling of Spring Boot instances (Kubernetes); partition PostgreSQL by user; implement distributed tracing (Micrograph/Zipkin); consider swapping Ollama for commercial LLM API with better SLA; add full-text search engine (Elasticsearch) for vocabulary search |

### Scaling Priorities

1. **First bottleneck:** Ollama LLM calls during text analysis (multiple sequential calls). **Fix:** Batch prompts into single LLM call or parallelize with async processing + circuit breaker.
2. **Second bottleneck:** TTS audio generation blocking request threads. **Fix:** Move to background job queue with status polling; cache aggressively; pre-generate audio for common words.
3. **Third bottleneck:** Database query performance as vocabulary grows. **Fix:** Add indexes on `lemma_text`, `category`, `source`; implement full-text search; consider materialized views for study session queries.
4. **Fourth bottleneck:** Audio file storage grows large. **Fix:** Migrate to object storage (S3/Minio); implement audio CDN; add audio compression (Opus codec).

## Anti-Patterns

### Anti-Pattern 1: Blocking Request Thread for LLM Calls

**What people do:** Make synchronous HTTP calls to Ollama from controller methods, blocking the request thread for 10-30 seconds.

**Why it's wrong:**
- Exhausts Tomcat thread pool (default 200 threads)
- Users see unresponsive UI during long waits
- Single slow LLM call blocks other requests
- No ability to cancel or timeout gracefully

**Do this instead:**
- Use Spring @Async for LLM service methods
- Return CompletableFuture or Mono (reactive)
- Implement timeout with @Timeout annotation
- Add circuit breaker to fail fast when Ollama is down
- Consider queue-based processing for bulk operations

```java
// BAD: Blocking
@PostMapping("/vocabulary")
public LemmaResponse createLemma(@RequestBody LemmaRequest request) {
    // Blocks for 10-30 seconds!
    LemmaMetadata metadata = llmService.detectLemma(request.getWord());
    return lemmaService.create(metadata);
}

// GOOD: Async with timeout
@PostMapping("/vocabulary")
public CompletableFuture<LemmaResponse> createLemma(@RequestBody LemmaRequest request) {
    return llmService.detectLemma(request.getWord())
        .thenApply(metadata -> lemmaService.create(metadata))
        .orTimeout(30, TimeUnit.SECONDS);
}
```

### Anti-Pattern 2: Storing Audio as BLOBs in PostgreSQL

**What people do:** Store generated MP3 audio directly in PostgreSQL as BYTEA or BLOB columns.

**Why it's wrong:**
- PostgreSQL backups become massive (GBs of audio)
- Memory pressure during query result buffering
- No CDN caching for audio files
- Inefficient for read-heavy workloads (audio playback)
- Wastes database resources better used for relational queries

**Do this instead:**
- Store audio files on disk (or S3/object storage)
- Store file path/URL in database as VARCHAR
- Serve audio files directly via Nginx/CDN (bypass Spring)
- Implement cache headers (immutable, long TTL)
- Use hash-based filenames for cache-busting

```sql
-- BAD: Audio in database
CREATE TABLE lemmas (
    id BIGSERIAL PRIMARY KEY,
    lemma_text VARCHAR(255) NOT NULL,
    audio_data BYTEA  -- ❌ Stores entire MP3 in DB
);

-- GOOD: Audio file path only
CREATE TABLE lemmas (
    id BIGSERIAL PRIMARY KEY,
    lemma_text VARCHAR(255) NOT NULL,
    audio_file_path VARCHAR(512),  -- ✅ Path to file
    audio_generated_at TIMESTAMP
);
```

### Anti-Pattern 3: Mixing Server State and Client State in Zustand

**What people do:** Store fetched vocabulary data in Zustand store instead of React Query.

**Why it's wrong:**
- Duplicates caching logic (React Query already does this)
- No automatic background refetching
- Manual invalidation required after mutations
- Harder to implement optimistic updates
- Loses React Query DevTools visibility

**Do this instead:**
- Use React Query for **all** server data (vocabulary, word lists, study sessions)
- Use Zustand **only** for UI state that doesn't exist on server (flashcard index, show answer, audio playing)
- Let React Query handle caching, refetching, invalidation
- Use React Query's optimistic updates for instant UI feedback

```typescript
// BAD: Server data in Zustand
const useVocabStore = create((set) => ({
    lemmas: [],
    fetchLemmas: async () => {
        const data = await api.getLemmas();
        set({ lemmas: data });  // ❌ Manual cache management
    },
}));

// GOOD: Server data in React Query, UI state in Zustand
const { data: lemmas } = useQuery({
    queryKey: ['lemmas'],
    queryFn: api.getLemmas,  // ✅ Automatic caching
});

const useStudyStore = create((set) => ({
    currentCardIndex: 0,  // ✅ UI state only
    showAnswer: false,
    nextCard: () => set((s) => ({ currentCardIndex: s.currentCardIndex + 1 })),
}));
```

### Anti-Pattern 4: N+1 Query Problem with Lazy Loading

**What people do:** Fetch lemmas without inflections, then lazy-load inflections in a loop during rendering.

**Why it's wrong:**
- Executes 1 query for lemmas + N queries for inflections (1 per lemma)
- Causes severe performance degradation with large datasets
- Triggers database round-trips during UI rendering
- Undetectable in small test datasets but fails in production

**Do this instead:**
- Use @EntityGraph or JOIN FETCH to eagerly load associations
- Fetch all needed data in single query
- Use DTO projections if full entity graph isn't needed
- Monitor with Hibernate `show_sql` and `format_sql` during development

```java
// BAD: N+1 queries
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    List<Lemma> findAll();  // ❌ Returns lemmas without inflections
}

// In service/controller, accessing inflections triggers N queries
lemmas.forEach(lemma ->
    System.out.println(lemma.getInflections())  // ❌ Lazy load = new query
);

// GOOD: Single query with JOIN FETCH
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    @Query("SELECT l FROM Lemma l LEFT JOIN FETCH l.inflections")
    List<Lemma> findAllWithInflections();  // ✅ Single query

    @EntityGraph(attributePaths = {"inflections", "category"})
    Page<Lemma> findAll(Pageable pageable);  // ✅ Single query with pagination
}
```

### Anti-Pattern 5: No Caching Strategy for Identical LLM Prompts

**What people do:** Call Ollama every time the same word is entered, regenerating identical metadata.

**Why it's wrong:**
- Wastes 10-30 seconds per duplicate LLM call
- Increases load on Ollama service unnecessarily
- Poor user experience (waiting for known results)
- Consumes compute resources for deterministic outputs

**Do this instead:**
- Cache LLM responses by prompt hash in Spring Cache or Redis
- Set appropriate TTL (lemma detection can be cached indefinitely)
- Implement cache key based on prompt + model version
- Provide manual cache invalidation if LLM behavior changes
- Add cache hit metrics to monitor effectiveness

```java
// BAD: No caching
@Service
public class LLMService {
    public LemmaMetadata detectLemma(String word) {
        // ❌ Calls Ollama every time, even for duplicate words
        return ollamaClient.generate(PromptTemplates.lemmaDetection(word));
    }
}

// GOOD: Cache-aside pattern
@Service
public class LLMService {

    @Cacheable(value = "llmResponses", key = "#word + '_' + #modelVersion")
    public LemmaMetadata detectLemma(String word) {
        // ✅ Cached by word + model version
        return ollamaClient.generate(PromptTemplates.lemmaDetection(word));
    }

    @CacheEvict(value = "llmResponses", allEntries = true)
    public void clearLLMCache() {
        // Manual cache invalidation when model updated
    }
}

// Spring Cache config
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("llmResponses",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String.class, LemmaMetadata.class,
                    ResourcePoolsBuilder.heap(1000))  // Cache up to 1000 entries
                .withExpiry(ExpiryPolicyBuilder.noExpiration()))  // Never expire
            .build(true);
    }
}
```

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| **Ollama LLM** | HTTP REST API (POST /api/generate) via Spring WebClient | Async with @Async + circuit breaker; keep-alive model in memory (OLLAMA_KEEP_ALIVE=5m); semantic caching via prompt hash; timeout: 30s; retry: 3 attempts with exponential backoff |
| **Edge TTS** | WebSocket or HTTP (language-specific client libraries: Python, Node.js, Rust, Dart) | Generate audio files, save to disk; cache aggressively (immutable audio); file naming: SHA-256 hash of text+voice; support streaming for long text; consider pre-generation for common vocabulary |
| **PostgreSQL** | Spring Data JPA with HikariCP connection pool | Flyway for schema migrations; connection pool: min=5, max=20; enable statement caching; use indexes on `lemma_text`, `category`, `source` columns; consider JSONB for flexible metadata |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| **React ↔ Spring Boot** | REST API (JSON over HTTP) | CORS enabled for development; Pageable for list endpoints; DTOs separate from entities; React Query manages caching |
| **Controller ↔ Service** | Direct method calls (same JVM) | Controllers handle HTTP concerns; Services contain business logic; Use @Transactional in service layer, not controllers |
| **Service ↔ Repository** | Spring Data JPA interfaces | Repositories return entities or DTOs; Use @EntityGraph to avoid N+1 queries; Pageable passed through from controller |
| **Service ↔ LLM/TTS** | Async via @Async methods returning CompletableFuture | Timeout and circuit breaker configured; LLM calls never block request threads; Background jobs for audio generation |
| **Frontend Components ↔ Hooks** | React hooks encapsulate data fetching | Custom hooks wrap React Query; Zustand hooks provide UI state; Components stay presentational |
| **Hooks ↔ API Client** | API client wrapped in React Query | API client uses Axios/fetch; React Query manages caching layer; Optimistic updates for mutations |

## Build Order and Dependencies

### Phase 1: Foundation (Backend Core)

**Build order:**
1. PostgreSQL schema (Flyway migrations) - `V1__create_schema.sql`
2. JPA entities (Lemma, Inflection, Category)
3. Spring Data JPA repositories
4. Basic REST controllers for CRUD (without LLM)
5. Frontend skeleton with routing and Tailwind

**Why this order:** Establish data model first; validate schema design with real queries before adding LLM complexity.

### Phase 2: LLM Integration

**Build order:**
1. LLM service with synchronous calls first
2. Cache configuration (Spring Cache)
3. Convert to @Async with CompletableFuture
4. Add circuit breaker and retry logic
5. Prompt engineering and response parsing

**Why this order:** Start simple (synchronous) to validate prompts and parsing; add complexity (async, circuit breaker) incrementally.

**Dependencies:** Requires Ollama running on Mac Studio; test with small dataset before bulk operations.

### Phase 3: TTS Integration

**Build order:**
1. TTS service with synchronous audio generation
2. File storage setup (disk-based cache)
3. Audio serving endpoint (GET /api/audio/{id})
4. Convert to async background generation
5. Frontend audio player component

**Why this order:** File storage must work before async processing; test audio playback in browser before automating generation.

**Dependencies:** Requires Edge TTS installed; decide storage path early (affects dev vs. prod config).

### Phase 4: Text Analysis

**Build order:**
1. Text extraction service (regex-based word splitting)
2. Batch LLM processing (parallel async calls)
3. Deduplication logic (check existing lemmas)
4. Review UI (frontend)
5. Bulk vocabulary creation endpoint

**Why this order:** Text extraction is independent of LLM; validate word splitting before LLM integration; review UI needed before bulk creates.

**Dependencies:** Requires LLM integration from Phase 2; test with sample Bulgarian text paragraphs.

### Phase 5: Study Features

**Build order:**
1. WordList entity and CRUD endpoints
2. Flashcard query logic (with spaced repetition algorithm)
3. Frontend Flashcard component (without audio)
4. Audio integration in flashcards
5. Study session tracking (optional)

**Why this order:** WordList CRUD is straightforward; flashcard logic can be tested without audio; audio is added last for polish.

**Dependencies:** Requires vocabulary data (Phase 1-3); spaced repetition algorithm can start simple (random order) and improve later.

### Component Dependency Graph

```
PostgreSQL Schema (V1)
    ↓
JPA Entities + Repositories
    ↓
    ├─→ Basic CRUD Controllers → Frontend Skeleton
    │                                ↓
    │                         React Query Setup
    │
    ├─→ LLM Service → Cache Config → Async + Circuit Breaker
    │       ↓
    │   Lemma Creation Flow
    │       ↓
    ├─→ TTS Service → File Storage → Audio Endpoints
    │       ↓
    │   Audio Generation
    │       ↓
    ├─→ Text Analysis Service → Batch LLM → Review UI
    │       ↓
    │   Bulk Vocabulary Creation
    │
    └─→ WordList CRUD → Flashcard Logic → Study UI → Audio Integration
            ↓
        Study Sessions
```

**Critical path:** PostgreSQL → Entities → LLM Service → TTS Service → Study Features

**Parallelizable:** Frontend skeleton can develop alongside backend (using mock data initially).

## Sources

**Spring Boot + React Architecture:**
- [CRUD Application With React and Spring Boot | Baeldung](https://www.baeldung.com/spring-boot-react-crud)
- [Creating Spring Boot and React Java Full Stack Application | Spring Boot Tutorial](https://www.springboottutorial.com/spring-boot-react-full-stack-crud-maven-application)
- [Spring Boot Architecture | GeeksforGeeks](https://www.geeksforgeeks.org/springboot/spring-boot-architecture/)

**LLM Integration Patterns:**
- [Master Spring AI: LLM Integration for Spring Boot Devs | Mobisoft](https://mobisoftinfotech.com/resources/blog/ai-development/spring-ai-llm-integration-spring-boot)
- [Introduction to Spring AI | Baeldung](https://www.baeldung.com/spring-ai)
- [Spring AI: Streamlining Local LLM Integration for Java Developers | Layer5](https://layer5.io/blog/docker/spring-ai-streamlining-local-llm-integration-for-java-developers)

**Ollama Caching & Optimization:**
- [Ollama Caching Strategies: Boost Repeat Query Performance by 300% | Markaicode](https://markaicode.com/ollama-caching-strategies-improve-repeat-query-performance/)
- [Turbocharging Your LLM with Redis Semantic Caching and Ollama | Medium](https://medium.com/@aashmit13/turbocharging-your-llm-with-redis-semantic-caching-and-ollama-fd749b5f61c3)

**Text-to-Speech Architecture:**
- [Building Long-Running TTS Pipelines with LangGraph | Vadim's Blog](https://vadim.blog/2026/01/18/langgraph-tts-therapeutic-audio-architecture)
- [Edge TTS: The Ultimate Guide for Developers | VideoSDK](https://www.videosdk.live/developer-hub/ai/edge-tts)

**React State Management 2026:**
- [State Management in React (2026): Best Practices, Tools & Real-World Patterns | C# Corner](https://www.c-sharpcorner.com/article/state-management-in-react-2026-best-practices-tools-real-world-patterns/)
- [Top 5 React State Management Tools Developers Actually Use in 2026 | Syncfusion](https://www.syncfusion.com/blogs/post/react-state-management-libraries)

**Spring Boot Pagination:**
- [REST Pagination in Spring | Baeldung](https://www.baeldung.com/rest-api-pagination-in-spring)
- [Spring Boot Pagination and Sorting example | BezKoder](https://www.bezkoder.com/spring-boot-pagination-sorting-example/)

**Linguistic Data Processing:**
- [Lemma Dilemma: On Lemma Generation Without Domain- or Language-Specific Training Data | arXiv](https://arxiv.org/html/2510.07434v1)
- [LemmInflect | GitHub](https://github.com/bjascob/LemmInflect)

**Frontend System Design:**
- [Frontend System Design: The Complete Guide 2026 | System Design Handbook](https://www.systemdesignhandbook.com/guides/frontend-system-design/)
- [Front-End Architecture: In-Depth Analysis for 2026 | ELITEX](https://elitex.systems/blog/front-end-architecture-in-depth-analysis)

**Spaced Repetition Systems:**
- [Spaced Repetition Algorithm: A Three‐Day Journey from Novice to Expert | GitHub](https://github.com/open-spaced-repetition/fsrs4anki/wiki/spaced-repetition-algorithm:-a-three%E2%80%90day-journey-from-novice-to-expert)
- [The power of spaced repetition and flashcards | Ness Labs](https://nesslabs.com/spaced-repetition)

---
*Architecture research for: Bulgarian Vocabulary Learning Application*
*Researched: 2026-02-15*
