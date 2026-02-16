---
phase: 03-tts-audio-generation
plan: 02
subsystem: audio
tags: [tts, edge-tts, async, rest-api, security]
dependencies:
  requires:
    - 03-01: Audio infrastructure (exception, hash util, storage config, async executor)
  provides:
    - EdgeTtsService: Edge TTS CLI execution with content-hash caching
    - AudioGenerationService: Async wrapper for non-blocking audio generation
    - AudioController: REST endpoint for serving audio files
  affects:
    - Phase 04: Vocabulary service will use AudioGenerationService for lemma audio
tech_stack:
  added:
    - edge-tts CLI via ProcessBuilder
    - Spring @Async for audio thread pool
    - Spring @Cacheable for content-hash deduplication
  patterns:
    - Two-layer async pattern (sync @Cacheable + async @Async wrapper)
    - Atomic file generation (temp file + move)
    - Defense-in-depth security (whitelist + normalization + existence)
    - Content-hash based immutable caching
key_files:
  created:
    - backend/src/main/java/com/vocab/bulgarian/audio/service/EdgeTtsService.java
    - backend/src/main/java/com/vocab/bulgarian/audio/service/AudioGenerationService.java
    - backend/src/main/java/com/vocab/bulgarian/audio/controller/AudioController.java
  modified: []
decisions:
  - ProcessBuilder with separate arguments prevents shell injection
  - Temp file + atomic move prevents partial MP3 files from being served
  - Process output consumption (BufferedReader loop) prevents buffer blocking
  - 30-second timeout for edge-tts prevents indefinite hangs
  - Cache key uses hash (not Cyrillic text) to avoid encoding issues
  - Three-layer path security: whitelist regex, path normalization, existence check
  - Immutable content-hash filenames enable aggressive browser caching (1 year)
  - Health check endpoint reports edge-tts availability and storage status
metrics:
  duration: 2m 2s
  tasks_completed: 2
  files_created: 3
  commits: 2
  completed_at: 2026-02-16T07:52:21Z
---

# Phase 03 Plan 02: TTS Service and Audio REST Endpoint Summary

**One-liner:** Edge TTS CLI execution with ProcessBuilder, async audio generation, and secure REST endpoint for serving content-hash cached MP3 files.

## What Was Built

This plan implements the core TTS functionality for Phase 3 — generating Bulgarian audio using the edge-tts CLI and serving it via HTTP. It completes all five Phase 3 success criteria.

### EdgeTtsService (Task 1)
Synchronous service for edge-tts CLI execution with content-hash based caching.

**Key features:**
- **ProcessBuilder with separate arguments** — shell injection prevention
- **Idempotency check** — skip generation if file already exists (Files.exists check)
- **Atomic file generation** — temp file + Files.move prevents partial MP3s
- **Process output consumption** — BufferedReader loop prevents buffer blocking
- **30-second timeout** — destroyForcibly() prevents indefinite hangs
- **@Cacheable with hash-based key** — avoids Cyrillic encoding issues in cache keys
- **isEdgeTtsAvailable()** — health check method for startup validation

**Implementation details:**
- Constructor injection: `@Value("${audio.storage.path}")`, `@Value("${tts.bulgarian.voice.default}")`
- Two-method overload: `generateAudio(text)` uses default voice, `generateAudio(text, voice)` allows custom voice
- Cache key: `T(com.vocab.bulgarian.audio.util.ContentHashUtil).generateHash(#bulgarianText, #voiceName)`
- Filename format: `{64-char-hash}.mp3`

### AudioGenerationService (Task 2)
Async wrapper following Phase 2's two-layer pattern (sync @Cacheable method + async @Async wrapper).

**Key features:**
- **@Async("audioTaskExecutor")** — uses dedicated audio thread pool (2-4 threads)
- **CompletableFuture return type** — enables non-blocking execution
- **Two-method overload** — mirrors EdgeTtsService API

**Why this pattern?**
Caching the synchronous method (not the CompletableFuture) avoids cache pollution. This is the same pattern used by LemmaDetectionService in Phase 2.

### AudioController (Task 2)
REST endpoint for serving audio files with three-layer path traversal prevention.

**Defense Layer 1: Whitelist validation**
- Regex: `^[a-f0-9]{64}\.mp3$`
- Only allows 64-char lowercase hex + .mp3 extension
- Returns 400 Bad Request for invalid format

**Defense Layer 2: Path normalization and containment**
- `storagePath.resolve(filename).normalize()`
- Checks `requestedPath.startsWith(storagePath)`
- Returns 403 Forbidden for traversal attempts

**Defense Layer 3: Existence check**
- `Files.exists(requestedPath) && Files.isRegularFile(requestedPath)`
- Returns 404 Not Found for missing files

**HTTP response:**
- Content-Type: `audio/mpeg`
- Content-Disposition: `inline; filename="{filename}"` (enables browser playback)
- Cache-Control: `public, max-age=31536000, immutable` (content-hash = immutable)

**Health check endpoint:**
- GET `/api/audio/health`
- Returns JSON: `{"available": true/false, "storagePath": "...", "storageExists": true/false}`
- Status: 200 OK if both edge-tts available and storage exists, 503 otherwise

## Phase 3 Success Criteria Completion

All five Phase 3 success criteria are now met:

1. ✓ **System can generate an MP3 audio file for Bulgarian text using edge-tts CLI** — EdgeTtsService.generateAudio() calls edge-tts via ProcessBuilder
2. ✓ **Audio files are stored on disk at the configured storage path** — Files saved to `${audio.storage.path}` from application.yml
3. ✓ **Identical text with the same voice produces the same filename and skips regeneration** — ContentHashUtil.generateHash() + Files.exists() idempotency check
4. ✓ **Audio generation runs asynchronously without blocking the calling thread** — AudioGenerationService with @Async("audioTaskExecutor")
5. ✓ **GET /api/audio/{filename} returns an audio/mpeg response for valid files** — AudioController.getAudioFile() with three-layer security

## Verification Results

All verification checks passed:

- ✓ mvn compile -q passes with no errors
- ✓ All 3 files exist at specified paths
- ✓ EdgeTtsService uses ProcessBuilder with separate argument strings
- ✓ EdgeTtsService has @Cacheable with hash-based cache key
- ✓ AudioGenerationService has @Async("audioTaskExecutor")
- ✓ AudioController validates filename with regex `^[a-f0-9]{64}\.mp3$`
- ✓ AudioController normalizes paths and checks containment within storage directory
- ✓ AudioController returns proper Content-Type (audio/mpeg) and Cache-Control headers
- ✓ Health check endpoint exists at /api/audio/health

## Deviations from Plan

None — plan executed exactly as written.

## Technical Highlights

### ProcessBuilder Pattern (Pitfall #1 prevention)
```java
ProcessBuilder pb = new ProcessBuilder(
    "edge-tts",
    "--voice", voiceName,
    "--text", bulgarianText,
    "--write-media", tempFile.toString()
);
```
Separate arguments prevent shell injection. No string concatenation.

### Output Consumption (Pitfall #4 prevention)
```java
try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
    String line;
    while ((line = reader.readLine()) != null) {
        log.debug("edge-tts: {}", line);
    }
}
```
Consuming process output prevents buffer blocking that would hang the process.

### Atomic File Generation
```java
Path tempFile = Files.createTempFile(Paths.get(audioStoragePath), "audio-", ".tmp");
// ... edge-tts writes to tempFile ...
Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
```
Prevents partial MP3 files from being served if generation is interrupted.

### Cache Key Design
```java
@Cacheable(value = "audioFiles", key = "T(com.vocab.bulgarian.audio.util.ContentHashUtil).generateHash(#bulgarianText, #voiceName)")
```
Using the hash as the cache key (not the Cyrillic text) avoids encoding issues in Redis. This follows research pitfall #8.

### Path Traversal Prevention
```java
if (!filename.matches("^[a-f0-9]{64}\\.mp3$")) return badRequest();
Path requestedPath = storagePath.resolve(filename).normalize();
if (!requestedPath.startsWith(storagePath)) return forbidden();
if (!Files.exists(requestedPath)) return notFound();
```
Three independent layers ensure comprehensive security.

## Next Steps

Phase 3 is now complete. Phase 4 (Vocabulary Management) will:
- Use AudioGenerationService to generate audio for lemmas
- Store audio filenames in the database
- Integrate with LLM orchestration from Phase 2

## Self-Check: PASSED

**Files created:**
- ✓ FOUND: backend/src/main/java/com/vocab/bulgarian/audio/service/EdgeTtsService.java
- ✓ FOUND: backend/src/main/java/com/vocab/bulgarian/audio/service/AudioGenerationService.java
- ✓ FOUND: backend/src/main/java/com/vocab/bulgarian/audio/controller/AudioController.java

**Commits exist:**
- ✓ FOUND: 4115170 (Task 1: EdgeTtsService)
- ✓ FOUND: e4254b5 (Task 2: AudioGenerationService + AudioController)
