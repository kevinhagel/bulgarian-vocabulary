package com.vocab.bulgarian.audio.service;

import com.vocab.bulgarian.audio.exception.AudioGenerationException;
import com.vocab.bulgarian.audio.util.ContentHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating audio files using the edge-tts CLI.
 * Provides synchronous audio generation with file caching and idempotency.
 */
@Service
public class EdgeTtsService {

    private static final Logger log = LoggerFactory.getLogger(EdgeTtsService.class);

    private final String audioStoragePath;
    private final String defaultVoice;

    public EdgeTtsService(
            @Value("${audio.storage.path}") String audioStoragePath,
            @Value("${tts.bulgarian.voice.default}") String defaultVoice) {
        this.audioStoragePath = audioStoragePath;
        this.defaultVoice = defaultVoice;
    }

    /**
     * Generates audio file for Bulgarian text using the default voice.
     *
     * @param bulgarianText the text to speak
     * @return the filename (content hash + .mp3)
     */
    public String generateAudio(String bulgarianText) {
        return generateAudio(bulgarianText, defaultVoice);
    }

    /**
     * Generates audio file for Bulgarian text using edge-tts CLI.
     * Uses content-hash based caching to avoid regenerating identical audio.
     *
     * @param bulgarianText the text to speak
     * @param voiceName the Azure TTS voice name (e.g., bg-BG-KalinaNeural)
     * @return the filename (content hash + .mp3)
     * @throws AudioGenerationException if generation fails
     */
    @Cacheable(value = "audioFiles", key = "T(com.vocab.bulgarian.audio.util.ContentHashUtil).generateHash(#bulgarianText, #voiceName)")
    public String generateAudio(String bulgarianText, String voiceName) {
        // Generate content hash for filename
        String hash = ContentHashUtil.generateHash(bulgarianText, voiceName);
        String filename = hash + ".mp3";
        Path filePath = Paths.get(audioStoragePath, filename);

        // Idempotency check: skip generation if file already exists
        if (Files.exists(filePath)) {
            log.debug("Audio file already exists: {}", filename);
            return filename;
        }

        log.info("Generating audio for text '{}' with voice {}", bulgarianText, voiceName);

        Path tempFile = null;
        try {
            // Create temp file in same directory for atomic move
            tempFile = Files.createTempFile(Paths.get(audioStoragePath), "audio-", ".tmp");

            // Build edge-tts command with separate arguments (prevents shell injection)
            ProcessBuilder pb = new ProcessBuilder(
                "edge-tts",
                "--voice", voiceName,
                "--text", bulgarianText,
                "--write-media", tempFile.toString()
            );

            // Redirect stderr to stdout for unified output consumption
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Consume process output to prevent buffer blocking
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("edge-tts: {}", line);
                }
            }

            // Wait for process with timeout
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                Files.deleteIfExists(tempFile);
                throw new AudioGenerationException(
                    "edge-tts process timed out after 30 seconds for text: " + bulgarianText
                );
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                Files.deleteIfExists(tempFile);
                throw new AudioGenerationException(
                    "edge-tts process failed with exit code " + exitCode + " for text: " + bulgarianText
                );
            }

            // Atomic move to final location
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Successfully generated audio file: {}", filename);
            return filename;

        } catch (AudioGenerationException e) {
            throw e;
        } catch (Exception e) {
            // Clean up temp file on any failure
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception cleanupException) {
                    log.warn("Failed to clean up temp file: {}", tempFile, cleanupException);
                }
            }
            throw new AudioGenerationException(
                "Failed to generate audio for text: " + bulgarianText, e
            );
        }
    }

    /**
     * Checks if edge-tts CLI is available on the system.
     * Used by health checks and startup validation.
     *
     * @return true if edge-tts is available, false otherwise
     */
    public boolean isEdgeTtsAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("edge-tts", "--version");
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Consume output to prevent blocking
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("edge-tts version check: {}", line);
                }
            }

            boolean completed = process.waitFor(5, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                log.warn("edge-tts version check timed out");
                return false;
            }

            boolean available = process.exitValue() == 0;
            if (!available) {
                log.warn("edge-tts is not available (exit code: {})", process.exitValue());
            }
            return available;

        } catch (Exception e) {
            log.warn("edge-tts availability check failed: {}", e.getMessage());
            return false;
        }
    }
}
