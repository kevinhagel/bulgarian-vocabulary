# Phase 3: TTS Audio Generation - Research

**Researched:** 2026-02-16
**Domain:** Text-to-Speech audio generation with Edge TTS, file-based caching, async processing
**Confidence:** MEDIUM

## Summary

Phase 3 implements Bulgarian pronunciation audio generation using Microsoft Edge's text-to-speech service. The implementation requires integrating either a Java library wrapper for Edge TTS or executing the edge-tts CLI as an external process, storing generated audio files on disk with content-hash based filenames to enable automatic deduplication, and processing audio generation asynchronously to avoid blocking HTTP requests.

Bulgarian language support is confirmed with two neural voices available: `bg-BG-KalinaNeural` (Female) and `bg-BG-BorislavNeural` (Male). Edge TTS is a free, reverse-engineered implementation of Microsoft Edge's Read Aloud API, which means it has no API keys or costs but lacks official Microsoft support and may have stability risks for production environments.

The established async pattern from Phase 2 (ThreadPoolTaskExecutor with @Async wrapper calling @Cacheable method) should be reused. File-based caching using SHA-256 content hashes as filenames provides automatic deduplication—identical Bulgarian text will always generate the same hash, preventing redundant audio generation.

**Primary recommendation:** Use edge-tts CLI via ProcessBuilder for maximum reliability, implement content-hash based file naming (SHA-256 of Bulgarian text), and serve audio files via Spring's ResourceHttpRequestHandler with strict path validation to prevent directory traversal attacks.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| edge-tts | Latest (CLI) | Generate TTS audio from Microsoft Edge API | Official Python implementation, most reliable and maintained |
| Spring @Async | Built-in | Background async execution | Already established in Phase 2 |
| Spring ResourceHttpRequestHandler | Built-in | Serve static/dynamic files via HTTP | Standard Spring MVC component for file serving |
| Java MessageDigest (SHA-256) | JDK built-in | Content hash generation | Industry standard for content-addressable storage |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| tts-edge-java | 1.3.3 | Java wrapper for Edge TTS | Alternative if ProcessBuilder approach is problematic |
| edge-tts-4j | Experimental | Java Edge TTS wrapper | NOT recommended (no releases, JDK 21+ requirement, GPL-3.0 license) |
| Apache Commons Codec | Latest | DigestUtils for simplified hashing | Optional - JDK MessageDigest sufficient |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Edge TTS (free) | Azure Speech Service | Paid service, more stable/supported, official Microsoft product |
| CLI via ProcessBuilder | tts-edge-java library | Library has limited Bulgarian voice docs, CLI is more reliable |
| MP3 format | WAV format | WAV is 10x larger, overkill for web delivery, slower loads |

**Installation:**

**Option 1: Edge TTS CLI (Recommended)**
```bash
pip install edge-tts
```

**Option 2: tts-edge-java library**
```xml
<dependency>
  <groupId>io.github.whitemagic2014</groupId>
  <artifactId>tts-edge-java</artifactId>
  <version>1.3.3</version>
</dependency>
```

## Architecture Patterns

### Recommended Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/vocab/
│   │       ├── service/
│   │       │   ├── AudioGenerationService.java      # @Async wrapper
│   │       │   └── EdgeTTSService.java              # @Cacheable TTS logic
│   │       ├── controller/
│   │       │   └── AudioController.java             # GET /api/audio/{hash}
│   │       └── util/
│   │           ├── ContentHashUtil.java             # SHA-256 hash generation
│   │           └── PathValidator.java               # Security: prevent path traversal
│   └── resources/
│       └── application.properties                   # audio.storage.path config
└── storage/
    └── audio/                                        # Generated audio files (not in classpath)
```

### Pattern 1: Content-Hash Based File Naming
**What:** Generate SHA-256 hash of Bulgarian text content, use as filename to enable automatic deduplication
**When to use:** Always - ensures identical text never regenerates audio
**Example:**
```java
// Source: https://www.baeldung.com/sha-256-hashing-java
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class ContentHashUtil {

    public static String generateHash(String content) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Usage: generateHash("Здравей") -> "a3f5b8c2..." (deterministic)
}
```

**CRITICAL:** MessageDigest is NOT thread-safe. Create new instance per method call (local variable), never class field.

### Pattern 2: Async Audio Generation with File Caching
**What:** @Async method checks if file exists (by hash), generates only if missing, returns filename
**When to use:** All audio generation requests
**Example:**
```java
@Service
public class AudioGenerationService {

    @Autowired
    private EdgeTTSService edgeTTSService;

    @Async("audioTaskExecutor")  // Use dedicated thread pool
    public CompletableFuture<String> generateAudioAsync(String bulgarianText, String voiceName) {
        return CompletableFuture.supplyAsync(() -> {
            return edgeTTSService.generateAudio(bulgarianText, voiceName);
        }).handle((result, ex) -> {
            if (ex != null) {
                // Log error, return fallback or rethrow
                log.error("Audio generation failed for text: {}", bulgarianText, ex);
                throw new AudioGenerationException("Failed to generate audio", ex);
            }
            return result;
        });
    }
}

@Service
public class EdgeTTSService {

    @Value("${audio.storage.path}")
    private String audioStoragePath;

    @Cacheable(value = "audioFiles", key = "#bulgarianText + '-' + #voiceName")
    public String generateAudio(String bulgarianText, String voiceName) {
        String hash = ContentHashUtil.generateHash(bulgarianText + voiceName);
        String filename = hash + ".mp3";
        Path filePath = Paths.get(audioStoragePath, filename);

        // Idempotency: check if file exists before generating
        if (Files.exists(filePath)) {
            log.info("Audio file already exists: {}", filename);
            return filename;
        }

        // Generate via edge-tts CLI
        ProcessBuilder pb = new ProcessBuilder(
            "edge-tts",
            "--voice", voiceName,
            "--text", bulgarianText,
            "--write-media", filePath.toString()
        );

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new AudioGenerationException("edge-tts failed with exit code: " + exitCode);
        }

        return filename;
    }
}
```

**Key insight:** Separating @Async (AudioGenerationService) from @Cacheable (EdgeTTSService) follows Phase 2 pattern and ensures proper cache behavior.

### Pattern 3: Secure File Serving with Path Validation
**What:** Validate requested filename, prevent directory traversal, serve via ResourceHttpRequestHandler
**When to use:** All file serving endpoints
**Example:**
```java
// Source: https://www.stackhawk.com/blog/spring-path-traversal-guide-examples-and-prevention/
@RestController
@RequestMapping("/api/audio")
public class AudioController {

    @Value("${audio.storage.path}")
    private String audioStoragePath;

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getAudioFile(@PathVariable String filename) {
        // Validation: prevent path traversal attacks
        if (!isValidFilename(filename)) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = Paths.get(audioStoragePath, filename).normalize();

        // Security check: ensure resolved path is within audio directory
        if (!filePath.startsWith(Paths.get(audioStoragePath).normalize())) {
            log.warn("Path traversal attempt detected: {}", filename);
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/mpeg"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
            .body(resource);
    }

    private boolean isValidFilename(String filename) {
        // Whitelist: SHA-256 hex (64 chars) + .mp3 extension
        return filename.matches("^[a-f0-9]{64}\\.mp3$");
    }
}
```

### Pattern 4: External File Storage Configuration
**What:** Configure audio storage path outside classpath/JAR for persistence
**When to use:** Always - enables file persistence across deployments
**Example:**
```properties
# application.properties
# Source: https://www.baeldung.com/spring-properties-file-outside-jar

# Absolute path to audio storage directory
audio.storage.path=/var/lib/bulgarian-vocab/audio

# Alternative: relative to JAR location
# audio.storage.path=./storage/audio

# Bulgarian TTS voice configuration
tts.bulgarian.voice.female=bg-BG-KalinaNeural
tts.bulgarian.voice.male=bg-BG-BorislavNeural
tts.bulgarian.voice.default=${tts.bulgarian.voice.female}
```

**Configuration class:**
```java
@Configuration
public class AudioStorageConfig {

    @Value("${audio.storage.path}")
    private String audioStoragePath;

    @PostConstruct
    public void ensureStorageDirectory() throws IOException {
        Path path = Paths.get(audioStoragePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Created audio storage directory: {}", path);
        }
    }
}
```

### Anti-Patterns to Avoid
- **Storing audio as BLOBs in database:** Degrades database performance, complicates backups, loses file system caching benefits
- **Using filename from original text:** Non-deterministic, prevents deduplication, exposes Cyrillic encoding issues
- **Synchronous audio generation in request handler:** Blocks thread, terrible UX for slow TTS operations (1-3 seconds)
- **Shared MessageDigest instance:** NOT thread-safe, causes race conditions in concurrent requests
- **No path validation on file serving:** Critical security vulnerability, allows path traversal attacks
- **Using WAV format for web delivery:** 10x larger than MP3, wastes bandwidth, slow page loads

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| TTS synthesis | Custom neural network model | edge-tts CLI or Azure Speech SDK | TTS models require massive training data, specialized expertise, and GPU resources |
| Content hashing | Custom hash function | Java MessageDigest SHA-256 | Cryptographic hash functions have subtle security pitfalls, use battle-tested JDK implementation |
| File serving | Custom FileInputStream controller | Spring ResourceHttpRequestHandler | Handles HTTP range requests, ETag caching, MIME types, error cases automatically |
| Async execution | Manual Thread/ExecutorService | Spring @Async + ThreadPoolTaskExecutor | Spring manages thread lifecycle, exception handling, context propagation |
| Path normalization | String manipulation for ".." removal | java.nio.file.Path.normalize() + startsWith validation | Path traversal attacks have many bypass techniques, use proven NIO.2 Path API |

**Key insight:** Audio generation, file serving, and security validation are deceptively complex domains. Edge TTS has already solved Bulgarian neural voice synthesis, Spring has battle-tested file serving infrastructure, and JDK provides secure path handling. Don't reinvent these wheels.

## Common Pitfalls

### Pitfall 1: ProcessBuilder Argument Quoting
**What goes wrong:** Passing entire command as single string to ProcessBuilder fails with "command not found" errors
**Why it happens:** ProcessBuilder expects separate strings per argument, NOT shell-style quoted strings
**How to avoid:** Use list/array constructor: `new ProcessBuilder("edge-tts", "--voice", "bg-BG-KalinaNeural", "--text", text)` NOT `new ProcessBuilder("edge-tts --voice bg-BG-KalinaNeural --text '" + text + "'")`
**Warning signs:** Process exits with code 127 (command not found), error mentions "edge-tts --voice" as single command

### Pitfall 2: MessageDigest Thread Safety
**What goes wrong:** Sharing MessageDigest instance across threads causes incorrect hash values and race conditions
**Why it happens:** MessageDigest maintains internal state (digest buffer) that mutates during digest() calls
**How to avoid:** Create new MessageDigest instance in method scope: `MessageDigest digest = MessageDigest.getInstance("SHA-256")` as local variable, NEVER as class field
**Warning signs:** Non-deterministic hash values for same input, intermittent cache misses, ConcurrentModificationException-like symptoms

### Pitfall 3: Path Traversal Vulnerability
**What goes wrong:** User provides filename like `../../../etc/passwd`, server exposes sensitive files
**Why it happens:** Direct path concatenation without validation allows directory escape
**How to avoid:** (1) Whitelist filename pattern (hex hash + .mp3), (2) normalize path, (3) verify resolved path starts with storage directory
**Warning signs:** Security scanner flags path traversal, filenames with `..` or `/` characters accepted

### Pitfall 4: Missing Process Output Consumption
**What goes wrong:** ProcessBuilder.start() hangs indefinitely, never completes
**Why it happens:** Process output buffers (stdout/stderr) fill up, blocking process until consumed
**How to avoid:** Read process.getInputStream() and process.getErrorStream() in separate threads or redirect to file: `pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)`
**Warning signs:** Process hangs on `waitFor()`, timeout exceptions, no error output visible

### Pitfall 5: Assuming edge-tts is Installed
**What goes wrong:** Application crashes with "edge-tts command not found" in production
**Why it happens:** Development machine has edge-tts installed, deployment environment doesn't
**How to avoid:** (1) Document edge-tts as system dependency in README, (2) add health check that verifies edge-tts availability, (3) fail fast on startup if missing
**Warning signs:** Works locally, fails in Docker/production, IOException with "Cannot run program edge-tts"

### Pitfall 6: Large Text Without Chunking
**What goes wrong:** edge-tts fails or produces truncated audio for very long text
**Why it happens:** TTS services often have character limits (unknown for edge-tts)
**How to avoid:** Document max character limit, validate input length, consider chunking for long texts
**Warning signs:** Audio cuts off mid-sentence, silent failures for long inputs

### Pitfall 7: No Disk Space Monitoring
**What goes wrong:** Audio storage fills disk, application crashes or becomes unavailable
**Why it happens:** Generated audio files accumulate over time, no cleanup strategy
**How to avoid:** (1) Configure Spring Boot disk space health indicator, (2) implement scheduled cleanup of old/unused files, (3) set up disk space alerts
**Warning signs:** Disk usage grows unbounded, eventual IOException "No space left on device"

### Pitfall 8: Caching by Text Instead of Hash
**What goes wrong:** Cache keys are Cyrillic text, causing encoding issues or excessive memory usage
**Why it happens:** Using `@Cacheable(key = "#bulgarianText")` with raw Cyrillic strings
**How to avoid:** Use hash as cache key: `@Cacheable(key = "T(ContentHashUtil).generateHash(#bulgarianText + #voiceName)")`
**Warning signs:** Cache misses for identical text, encoding errors in cache keys, large cache memory footprint

## Code Examples

Verified patterns from official sources and best practices:

### SHA-256 Content Hashing (Thread-Safe)
```java
// Source: https://www.baeldung.com/sha-256-hashing-java
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class ContentHashUtil {

    /**
     * Generates deterministic SHA-256 hash of content.
     * Thread-safe: creates new MessageDigest instance per call.
     */
    public static String generateHash(String content) throws NoSuchAlgorithmException {
        // CRITICAL: Local variable, not class field - MessageDigest is NOT thread-safe
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
```

### ProcessBuilder for edge-tts CLI
```java
// Source: https://mkyong.com/java/java-processbuilder-examples/
// Source: https://www.baeldung.com/java-lang-processbuilder-api

public String executeEdgeTTS(String voice, String text, Path outputPath) throws IOException, InterruptedException {
    // CORRECT: Each argument as separate string
    ProcessBuilder pb = new ProcessBuilder(
        "edge-tts",
        "--voice", voice,
        "--text", text,
        "--write-media", outputPath.toString()
    );

    // Prevent process hanging: discard output or capture it
    pb.redirectErrorStream(true);  // Merge stderr into stdout
    pb.redirectOutput(ProcessBuilder.Redirect.PIPE);

    Process process = pb.start();

    // Consume output to prevent buffer blocking
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("edge-tts output: {}", line);
        }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
        throw new AudioGenerationException("edge-tts failed with exit code: " + exitCode);
    }

    return outputPath.getFileName().toString();
}
```

### Async + Error Handling with CompletableFuture
```java
// Source: https://medium.com/@codesculpturersh/master-async-error-handling-in-2025-new-patterns-best-practices-972d17cdbf91
// Source: https://howtodoinjava.com/spring-boot/spring-async-completablefuture/

@Service
public class AudioGenerationService {

    @Autowired
    private EdgeTTSService edgeTTSService;

    @Async("audioTaskExecutor")
    public CompletableFuture<String> generateAudioAsync(String bulgarianText, String voiceName) {
        return CompletableFuture.supplyAsync(() -> {
            return edgeTTSService.generateAudio(bulgarianText, voiceName);
        }).handle((result, ex) -> {
            // handle() gives both result and exception - better than exceptionally()
            if (ex != null) {
                log.error("Audio generation failed for text: {}", bulgarianText, ex);
                // Option 1: Return fallback value
                return "error-fallback.mp3";
                // Option 2: Rethrow as unchecked exception
                // throw new CompletionException("Audio generation failed", ex);
            }
            return result;
        });
    }
}
```

### Idempotent File Generation
```java
// Source: https://medium.com/@mbneto/achieving-idempotency-there-are-more-ways-than-you-think-12c832f76841

@Service
public class EdgeTTSService {

    public String generateAudio(String bulgarianText, String voiceName) throws Exception {
        String hash = ContentHashUtil.generateHash(bulgarianText + voiceName);
        String filename = hash + ".mp3";
        Path filePath = Paths.get(audioStoragePath, filename);

        // Idempotency check: if file exists, skip generation
        if (Files.exists(filePath)) {
            log.info("Audio file already exists: {}", filename);
            return filename;
        }

        // Atomic file creation to prevent race condition
        Path tempFile = Files.createTempFile(audioStoragePath, "audio-", ".mp3");
        try {
            // Generate to temp file first
            executeEdgeTTS(voiceName, bulgarianText, tempFile);

            // Atomic move to final location (overwrites if exists due to race)
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Generated new audio file: {}", filename);
            return filename;
        } catch (Exception e) {
            // Cleanup temp file on failure
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }
}
```

### Path Traversal Prevention
```java
// Source: https://www.stackhawk.com/blog/spring-path-traversal-guide-examples-and-prevention/

@GetMapping("/{filename}")
public ResponseEntity<Resource> getAudioFile(@PathVariable String filename) {
    // Defense layer 1: Whitelist validation
    if (!filename.matches("^[a-f0-9]{64}\\.mp3$")) {
        log.warn("Invalid filename format rejected: {}", filename);
        return ResponseEntity.badRequest().build();
    }

    // Defense layer 2: Path normalization and containment check
    Path storagePath = Paths.get(audioStoragePath).toAbsolutePath().normalize();
    Path requestedPath = storagePath.resolve(filename).normalize();

    if (!requestedPath.startsWith(storagePath)) {
        log.error("Path traversal attack detected: {}", filename);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // Defense layer 3: Existence check
    if (!Files.exists(requestedPath) || !Files.isRegularFile(requestedPath)) {
        return ResponseEntity.notFound().build();
    }

    Resource resource = new FileSystemResource(requestedPath);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("audio/mpeg"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
        .body(resource);
}
```

### Scheduled File Cleanup
```java
// Source: https://alexanderobregon.substack.com/p/cleaning-up-old-files-with-scheduled

@Configuration
@EnableScheduling
public class AudioCleanupScheduler {

    @Value("${audio.storage.path}")
    private String audioStoragePath;

    @Value("${audio.cleanup.max-age-days:30}")
    private int maxAgeDays;

    // Run daily at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldAudioFiles() throws IOException {
        Path storageDir = Paths.get(audioStoragePath);
        Instant cutoff = Instant.now().minus(maxAgeDays, ChronoUnit.DAYS);

        try (Stream<Path> files = Files.list(storageDir)) {
            List<Path> deletedFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".mp3"))
                .filter(path -> {
                    try {
                        FileTime lastModified = Files.getLastModifiedTime(path);
                        return lastModified.toInstant().isBefore(cutoff);
                    } catch (IOException e) {
                        log.error("Failed to read file time: {}", path, e);
                        return false;
                    }
                })
                .peek(path -> {
                    try {
                        Files.delete(path);
                        log.info("Deleted old audio file: {}", path.getFileName());
                    } catch (IOException e) {
                        log.error("Failed to delete file: {}", path, e);
                    }
                })
                .collect(Collectors.toList());

            log.info("Cleanup completed: {} files deleted", deletedFiles.size());
        }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Azure Speech Service SDK | edge-tts (reverse-engineered) | ~2021 | Free TTS, no API keys, but unofficial and unsupported |
| WAV format for web delivery | MP3/Opus for streaming | Mid-2010s | 90% file size reduction, faster page loads |
| Runtime.exec() | ProcessBuilder API | Java 5 (2004) | Better process control, separate args, I/O redirection |
| Manual thread pooling | Spring @Async + ThreadPoolTaskExecutor | Spring 3.0 (2009) | Declarative async, easier configuration, better error handling |
| SimpleAsyncTaskExecutor | ThreadPoolTaskExecutor | Production best practice | Prevents thread explosion, bounded resources |
| File paths as Strings | java.nio.file.Path API | Java 7 (2011) | Better path normalization, security, cross-platform |

**Deprecated/outdated:**
- **Runtime.exec(String command)**: Use ProcessBuilder with argument array instead - prevents shell injection, better control
- **SimpleAsyncTaskExecutor for production**: Creates unlimited threads - use ThreadPoolTaskExecutor with bounded queue
- **Storing audio in database BLOBs**: Anti-pattern - degrades DB performance, use file system storage

## Open Questions

1. **What is the actual character limit for edge-tts input text?**
   - What we know: TTS services typically have limits (Azure Speech is 10,000 characters)
   - What's unclear: edge-tts documentation doesn't specify limits
   - Recommendation: Test empirically with increasing lengths, implement 5,000 character safety limit with validation

2. **Does edge-tts have rate limiting or usage quotas?**
   - What we know: tts-edge-java library mentions `.isRateLimited(true)` option, suggesting rate limits exist
   - What's unclear: Specific limits not documented, may vary by region
   - Recommendation: Implement exponential backoff retry logic, monitor for 429/503 responses, add circuit breaker

3. **How stable is edge-tts for production use?**
   - What we know: Reverse-engineered from Microsoft Edge, not officially supported, has periodic breakage (v1.3.3 fixed 403 errors in Jan 2026)
   - What's unclear: Long-term reliability, Microsoft's stance on usage
   - Recommendation: Implement fallback mechanism (manual upload, alternate TTS), monitor health checks, budget for migration to Azure Speech if needed

4. **What audio format does edge-tts produce by default?**
   - What we know: Examples show MP3, `--write-media` saves to file, some libraries mention Opus format
   - What's unclear: Default bitrate, sample rate, mono vs stereo
   - Recommendation: Test output quality, verify format with `file` command, may need to specify format explicitly

5. **Should audio files be tied to user accounts or globally cached?**
   - What we know: Phase 3 doesn't mention user-specific audio, content hash is deterministic
   - What's unclear: Privacy implications, multi-tenant considerations
   - Recommendation: Start with global cache (same hash = same file for all users), revisit if user-specific voice preferences emerge

## Sources

### Primary (HIGH confidence)
- [Azure Speech Service Language Support](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/language-support) - Confirmed bg-BG-KalinaNeural and bg-BG-BorislavNeural voices
- [SHA-256 Hashing in Java | Baeldung](https://www.baeldung.com/sha-256-hashing-java) - MessageDigest implementation, thread safety
- [Guide to java.lang.ProcessBuilder API | Baeldung](https://www.baeldung.com/java-lang-processbuilder-api) - ProcessBuilder best practices
- [Spring Path Traversal Guide | StackHawk](https://www.stackhawk.com/blog/spring-path-traversal-guide-examples-and-prevention/) - Path validation, security patterns
- [Java MessageDigest API Docs](https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html) - Thread safety documentation

### Secondary (MEDIUM confidence)
- [tts-edge-java GitHub](https://github.com/WhiteMagic2014/tts-edge-java) - Java library, v1.3.3 release notes
- [edge-tts GitHub (Python)](https://github.com/rany2/edge-tts) - CLI usage, voice listing
- [Spring Boot @Async + CompletableFuture | HowToDoInJava](https://howtodoinjava.com/spring-boot/spring-async-completablefuture/) - Async patterns
- [Master @Async Error Handling in 2025 | Medium](https://medium.com/@codesculpturersh/master-async-error-handling-in-2025-new-patterns-best-practices-972d17cdbf91) - handle() vs exceptionally()
- [Serve Static Resources with Spring | Baeldung](https://www.baeldung.com/spring-mvc-static-resources) - ResourceHttpRequestHandler
- [Cleaning Up Old Files with Scheduled Tasks | Medium](https://alexanderobregon.substack.com/p/cleaning-up-old-files-with-scheduled) - @Scheduled file cleanup
- [MP3 vs WAV for Web Delivery | Muvi](https://www.muvi.com/blogs/wav-vs-mp3/) - Audio format tradeoffs
- [Achieving Idempotency | Medium](https://medium.com/@mbneto/achieving-idempotency-there-are-more-ways-than-you-think-12c832f76841) - Idempotent operations

### Tertiary (LOW confidence - needs validation)
- [Edge TTS Voices List (GitHub Gist)](https://gist.github.com/BettyJJ/17cbaa1de96235a7f5773b8690a20462) - Voice availability (not from official source)
- [edge-tts-4j GitHub](https://github.com/K12f/edge-tts-4j) - Experimental library, no releases
- [FoloToy Edge-TTS-Voices Wiki](https://github.com/FoloToy/folotoy-server-self-hosting/wiki/Edge%E2%80%90TTS%E2%80%90Voices) - Community-maintained voice list

## Metadata

**Confidence breakdown:**
- Standard stack: MEDIUM - edge-tts is unofficial/unsupported, but Bulgarian voices confirmed via Azure docs
- Architecture: HIGH - Patterns are standard Spring Boot + JDK practices, well-documented
- Pitfalls: HIGH - ProcessBuilder, MessageDigest, path traversal are well-known issues with proven solutions
- Edge TTS reliability: LOW - Reverse-engineered service, stability unknown, documentation sparse

**Research date:** 2026-02-16
**Valid until:** ~2026-03-16 (30 days - edge-tts is stable but unofficial, may break)

**Key risks:**
1. edge-tts is reverse-engineered and may break without warning (mitigated by health checks, fallback plan)
2. Bulgarian voice availability not verified in actual edge-tts CLI (confirmed in Azure docs, assumed compatible)
3. No documented rate limits or usage constraints (mitigated by circuit breaker, retry logic)

**Recommended validations during planning:**
- [ ] Verify `edge-tts --list-voices` includes bg-BG-KalinaNeural and bg-BG-BorislavNeural
- [ ] Test edge-tts CLI with sample Bulgarian text, confirm audio quality
- [ ] Verify edge-tts is available in deployment environment (Docker image needs Python + pip)
- [ ] Confirm acceptable audio quality for 64kbps MP3 (edge-tts default, or configure higher)
