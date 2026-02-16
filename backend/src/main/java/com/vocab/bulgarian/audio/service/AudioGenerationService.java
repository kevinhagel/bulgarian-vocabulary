package com.vocab.bulgarian.audio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for asynchronous audio generation.
 * Provides async wrapper around synchronous EdgeTtsService for non-blocking execution.
 */
@Service
public class AudioGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AudioGenerationService.class);

    private final EdgeTtsService edgeTtsService;

    public AudioGenerationService(EdgeTtsService edgeTtsService) {
        this.edgeTtsService = edgeTtsService;
    }

    /**
     * Asynchronously generates audio file for Bulgarian text using the default voice.
     *
     * @param bulgarianText the text to speak
     * @return CompletableFuture containing the filename
     */
    @Async("audioTaskExecutor")
    public CompletableFuture<String> generateAudioAsync(String bulgarianText) {
        log.debug("Async audio generation requested for: {}", bulgarianText);
        String filename = edgeTtsService.generateAudio(bulgarianText);
        log.debug("Async audio generation completed: {}", filename);
        return CompletableFuture.completedFuture(filename);
    }

    /**
     * Asynchronously generates audio file for Bulgarian text with specified voice.
     *
     * @param bulgarianText the text to speak
     * @param voiceName the Azure TTS voice name
     * @return CompletableFuture containing the filename
     */
    @Async("audioTaskExecutor")
    public CompletableFuture<String> generateAudioAsync(String bulgarianText, String voiceName) {
        log.debug("Async audio generation requested for '{}' with voice {}", bulgarianText, voiceName);
        String filename = edgeTtsService.generateAudio(bulgarianText, voiceName);
        log.debug("Async audio generation completed: {}", filename);
        return CompletableFuture.completedFuture(filename);
    }
}
