---
phase: 03-tts-audio-generation
plan: 01
subsystem: audio-infrastructure
tags: [audio, tts, configuration, async, storage]

dependency-graph:
  requires:
    - Spring Boot configuration infrastructure
    - Async configuration pattern from Phase 02
  provides:
    - AudioGenerationException for TTS error handling
    - ContentHashUtil for deterministic audio file naming
    - AudioStorageConfig with directory creation on startup
    - AudioAsyncConfig with dedicated thread pool
    - Audio storage path configuration (environment-specific)
    - TTS voice configuration (female/male Bulgarian voices)
  affects:
    - Phase 03-02: TTS service will use exception, hash util, and configs

tech-stack:
  added:
    - edge-tts voice configuration (bg-BG-KalinaNeural, bg-BG-BorislavNeural)
    - SHA-256 content hashing for audio caching
    - Dedicated thread pool for audio generation (2-4 threads)
  patterns:
    - @PostConstruct for startup initialization
    - Thread-safe utility (local MessageDigest instance)
    - Named executor bean pattern (@Bean(name = "audioTaskExecutor"))
    - Environment-specific property overrides (dev uses external SSD)
    - CallerRunsPolicy rejection handler for backpressure

key-files:
  created:
    - backend/src/main/java/com/vocab/bulgarian/audio/exception/AudioGenerationException.java
    - backend/src/main/java/com/vocab/bulgarian/audio/util/ContentHashUtil.java
    - backend/src/main/java/com/vocab/bulgarian/audio/config/AudioStorageConfig.java
    - backend/src/main/java/com/vocab/bulgarian/audio/config/AudioAsyncConfig.java
  modified:
    - backend/src/main/resources/application.yml
    - backend/src/main/resources/application-dev.yml

decisions:
  - key: Thread pool sizing for audio generation
    choice: 2-4 threads (vs 4-8 for LLM)
    rationale: Audio generation is I/O-bound waiting on edge-tts process, requires fewer threads than CPU-intensive LLM operations
  - key: Content hash collision prevention
    choice: Pipe separator between text and voice name
    rationale: Prevents collisions like "ab" + "c" vs "a" + "bc", ensures deterministic hashing per voice
  - key: MessageDigest thread safety
    choice: Create new instance per generateHash() call
    rationale: MessageDigest is NOT thread-safe, local instance ensures safe concurrent usage
  - key: Storage directory initialization
    choice: @PostConstruct with fail-fast on error
    rationale: Ensures storage is ready before any audio generation, fails early if misconfigured
  - key: Dev environment storage location
    choice: External SSD (/Volumes/T7-NorthStar)
    rationale: Audio files are large, external SSD provides more space and faster I/O than internal drive

metrics:
  duration_seconds: 132
  duration_minutes: 2
  tasks_completed: 2
  files_created: 4
  files_modified: 2
  commits: 2
  completed_at: "2026-02-16T07:47:24Z"
---

# Phase 03 Plan 01: Audio Infrastructure Summary

JWT auth with refresh rotation using jose library

## Overview

Established foundational audio generation infrastructure including custom exception handling, thread-safe content hashing, storage configuration with automatic directory creation, and a dedicated async executor thread pool separate from LLM operations. This infrastructure will support the TTS service implementation in the next plan.

## What Was Built

### 1. Exception Handling (Task 1)

**AudioGenerationException.java**
- Custom runtime exception for TTS failures
- Follows project pattern (unchecked, like LlmValidationException from Phase 2)
- Two constructors: (String message), (String message, Throwable cause)
- Package: `com.vocab.bulgarian.audio.exception`

### 2. Content Hashing Utility (Task 1)

**ContentHashUtil.java**
- Static utility class for deterministic SHA-256 hash generation
- `generateHash(String text, String voiceName)` produces 64-char hex string
- Thread-safe: creates NEW MessageDigest instance per call (critical for concurrency)
- Pipe separator (`text + "|" + voiceName`) prevents hash collisions
- Wraps NoSuchAlgorithmException in RuntimeException (SHA-256 always available)
- Package: `com.vocab.bulgarian.audio.util`

**Example usage:**
```java
String hash = ContentHashUtil.generateHash("Здравей", "bg-BG-KalinaNeural");
// Returns: deterministic 64-char hex string like "a3b2c1d4..."
```

### 3. Storage Configuration (Task 2)

**AudioStorageConfig.java**
- @Configuration class with @PostConstruct initialization
- @Value injection: `audio.storage.path`, `tts.bulgarian.voice.default`
- `ensureStorageDirectory()`: creates directory on startup, logs path, fails fast on error
- @Bean `audioStoragePath()`: provides storage path for injection
- Package: `com.vocab.bulgarian.audio.config`

**Startup behavior:**
- Creates storage directory if missing
- Throws RuntimeException if directory creation fails
- Ensures audio generation never fails due to missing directory

### 4. Async Configuration (Task 2)

**AudioAsyncConfig.java**
- @Configuration class providing dedicated audio thread pool
- @Bean(name = "audioTaskExecutor") returning ThreadPoolTaskExecutor
- Core pool: 2 threads, Max pool: 4 threads, Queue capacity: 50
- Thread prefix: "audio-async-"
- CallerRunsPolicy rejection handler (consistent with LLM executor)
- Graceful shutdown: waits 30s for tasks to complete
- Package: `com.vocab.bulgarian.audio.config`

**Thread pool rationale:**
- Smaller than LLM pool (2-4 vs 4-8) because audio generation is I/O-bound
- Larger queue (50 vs 25) because edge-tts is faster than LLM calls
- Separate executor prevents audio and LLM operations from interfering

### 5. Application Properties (Task 2)

**application.yml additions:**
```yaml
audio:
  storage:
    path: ${AUDIO_STORAGE_PATH:./storage/audio}
  cleanup:
    max-age-days: 30

tts:
  bulgarian:
    voice:
      female: bg-BG-KalinaNeural
      male: bg-BG-BorislavNeural
      default: ${tts.bulgarian.voice.female}
```

**application-dev.yml additions:**
```yaml
audio:
  storage:
    path: ${AUDIO_STORAGE_PATH:/Volumes/T7-NorthStar/bulgarian-vocab/audio}
```

**Configuration highlights:**
- Default storage: `./storage/audio` (production/test)
- Dev override: External SSD for more space and faster I/O
- TTS voices: Female (default), Male (both Azure Cognitive Services voices)
- Cleanup policy: 30-day max age for audio files (future implementation)

## Task Breakdown

| Task | Description | Commit | Files |
|------|-------------|--------|-------|
| 1 | Create audio exception and content hash utility | 3058306 | AudioGenerationException.java, ContentHashUtil.java |
| 2 | Create audio storage config, async config, and update properties | 64695fe | AudioStorageConfig.java, AudioAsyncConfig.java, application.yml, application-dev.yml |

## Verification Results

All verification checks passed:
- ✅ `mvn compile -q` passes with no errors
- ✅ All 6 files exist at specified paths
- ✅ ContentHashUtil produces 64-char hex strings (SHA-256)
- ✅ AudioStorageConfig has @PostConstruct method
- ✅ AudioAsyncConfig has audioTaskExecutor bean
- ✅ application.yml has audio.storage.path and tts.bulgarian.voice properties
- ✅ application-dev.yml overrides audio storage path to external SSD

## Success Criteria Met

- ✅ Audio infrastructure package (`com.vocab.bulgarian.audio`) established with config, exception, and util sub-packages
- ✅ Dedicated audio thread pool configured separately from LLM thread pool
- ✅ Audio storage path externalized and configurable per environment
- ✅ Content hash utility is thread-safe (local MessageDigest instance)
- ✅ Application compiles successfully

## Deviations from Plan

None - plan executed exactly as written.

## Key Technical Decisions

**1. Thread-Safe Content Hashing**
- Decision: Create new MessageDigest instance per call (not a class field)
- Why: MessageDigest is NOT thread-safe; concurrent access would cause data corruption
- Impact: Slight performance overhead (negligible), significant correctness gain

**2. Pipe Separator in Hash Input**
- Decision: Concatenate `text + "|" + voiceName`
- Why: Prevents collision between "ab" + "c" and "a" + "bc"
- Impact: Deterministic, collision-resistant hash for audio caching

**3. Fail-Fast Storage Initialization**
- Decision: @PostConstruct throws RuntimeException if directory creation fails
- Why: Audio generation cannot proceed without storage; fail early to surface misconfiguration
- Impact: Application won't start if storage is misconfigured (desired behavior)

**4. Smaller Thread Pool for Audio**
- Decision: 2-4 threads for audio vs 4-8 for LLM
- Why: Audio generation is I/O-bound (waiting on edge-tts process), not CPU-intensive
- Impact: More efficient resource utilization, prevents thread pool saturation

**5. External SSD for Dev Environment**
- Decision: Override storage path to `/Volumes/T7-NorthStar/bulgarian-vocab/audio` in dev
- Why: Audio files are large; external SSD provides more space and faster I/O
- Impact: Better dev experience, mirrors production-like storage constraints

## Next Steps

**Phase 03 Plan 02: TTS Service Implementation**
- AudioService: edge-tts wrapper with async generation
- AudioController: REST endpoint for audio generation
- Integration with ContentHashUtil for caching
- Use of audioTaskExecutor for async operations
- Dependency on AudioStorageConfig for storage path

## Self-Check: PASSED

**Files created:**
- FOUND: backend/src/main/java/com/vocab/bulgarian/audio/exception/AudioGenerationException.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/audio/util/ContentHashUtil.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/audio/config/AudioStorageConfig.java
- FOUND: backend/src/main/java/com/vocab/bulgarian/audio/config/AudioAsyncConfig.java

**Files modified:**
- FOUND: backend/src/main/resources/application.yml
- FOUND: backend/src/main/resources/application-dev.yml

**Commits verified:**
- FOUND: 3058306 (feat(03-01): add audio exception and content hash utility)
- FOUND: 64695fe (feat(03-01): add audio storage and async config with TTS properties)
