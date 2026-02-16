package com.vocab.bulgarian.audio.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for audio file storage.
 * Ensures storage directory exists on application startup.
 */
@Configuration
public class AudioStorageConfig {

    private static final Logger logger = LoggerFactory.getLogger(AudioStorageConfig.class);

    @Value("${audio.storage.path}")
    private String audioStoragePath;

    @Value("${tts.bulgarian.voice.default}")
    private String defaultVoice;

    /**
     * Creates audio storage directory on application startup if it does not exist.
     * Fails fast if directory cannot be created.
     */
    @PostConstruct
    public void ensureStorageDirectory() {
        try {
            Path storagePath = Paths.get(audioStoragePath);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                logger.info("Created audio storage directory: {}", audioStoragePath);
            } else {
                logger.info("Audio storage directory exists: {}", audioStoragePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create audio storage directory: " + audioStoragePath, e);
        }
    }

    /**
     * Provides audio storage path as a bean for injection.
     */
    @Bean
    public String audioStoragePath() {
        return audioStoragePath;
    }
}
