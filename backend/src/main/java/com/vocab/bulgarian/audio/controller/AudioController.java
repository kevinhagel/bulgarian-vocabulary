package com.vocab.bulgarian.audio.controller;

import com.vocab.bulgarian.audio.service.AudioGenerationService;
import com.vocab.bulgarian.audio.service.EdgeTtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for serving audio files.
 * Implements defense-in-depth security for path traversal prevention.
 */
@RestController
@RequestMapping("/api/audio")
public class AudioController {

    private static final Logger log = LoggerFactory.getLogger(AudioController.class);

    private final String audioStoragePath;
    private final EdgeTtsService edgeTtsService;
    private final AudioGenerationService audioGenerationService;

    public AudioController(
            @Value("${audio.storage.path}") String audioStoragePath,
            EdgeTtsService edgeTtsService,
            AudioGenerationService audioGenerationService) {
        this.audioStoragePath = audioStoragePath;
        this.edgeTtsService = edgeTtsService;
        this.audioGenerationService = audioGenerationService;
    }

    /**
     * Serves an audio file by filename.
     * Implements three-layer security: whitelist validation, path normalization, existence check.
     *
     * @param filename the audio filename (content hash + .mp3)
     * @return the audio file as audio/mpeg
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getAudioFile(@PathVariable String filename) {
        // Defense Layer 1: Whitelist validation
        // Only accept 64-character lowercase hex hash + .mp3 extension
        if (!filename.matches("^[a-f0-9]{64}\\.mp3$")) {
            log.warn("Invalid filename format rejected: {}", filename);
            return ResponseEntity.badRequest().build();
        }

        // Defense Layer 2: Path normalization and containment check
        Path storagePath = Paths.get(audioStoragePath).toAbsolutePath().normalize();
        Path requestedPath = storagePath.resolve(filename).normalize();

        if (!requestedPath.startsWith(storagePath)) {
            log.error("Path traversal attempt detected: {}", filename);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Defense Layer 3: Existence and file type check
        if (!Files.exists(requestedPath) || !Files.isRegularFile(requestedPath)) {
            log.debug("Audio file not found: {}", filename);
            return ResponseEntity.notFound().build();
        }

        // Serve the file with appropriate headers
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        // Content-hash filenames are immutable - aggressive caching is safe
        headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable");

        Resource resource = new FileSystemResource(requestedPath);

        log.debug("Serving audio file: {}", filename);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    /**
     * Generates audio file for Bulgarian text.
     * Asynchronously generates MP3 file and returns the filename for retrieval via GET /{filename}.
     *
     * @param request JSON body with "text" field containing Bulgarian text
     * @return CompletableFuture with JSON containing "filename" field
     */
    @PostMapping("/generate")
    public CompletableFuture<ResponseEntity<Map<String, String>>> generateAudio(@RequestBody Map<String, String> request) {
        String text = request.get("text");

        // Validate text is present
        if (text == null || text.isBlank()) {
            log.warn("Audio generation request with blank text");
            Map<String, String> error = Map.of("error", "Text is required");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(error));
        }

        log.debug("Audio generation requested for: {}", text);

        // Call async generation service
        return audioGenerationService.generateAudioAsync(text)
                .thenApply(filename -> {
                    Map<String, String> response = Map.of("filename", filename);
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Audio generation failed for text: {}", text, ex);
                    Map<String, String> error = Map.of("error", "Audio generation failed");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                });
    }

    /**
     * Health check endpoint for audio service.
     * Checks edge-tts availability and storage path status.
     *
     * @return health status JSON
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();

        boolean edgeTtsAvailable = edgeTtsService.isEdgeTtsAvailable();
        health.put("available", edgeTtsAvailable);

        Path storagePath = Paths.get(audioStoragePath);
        boolean storageExists = Files.exists(storagePath) && Files.isDirectory(storagePath);
        health.put("storagePath", audioStoragePath);
        health.put("storageExists", storageExists);

        // Overall health is OK if both edge-tts is available and storage exists
        HttpStatus status = (edgeTtsAvailable && storageExists) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(health);
    }
}
