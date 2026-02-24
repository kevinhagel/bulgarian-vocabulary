package com.vocab.bulgarian.llm.service;

import com.vocab.bulgarian.llm.dto.LemmaMetadata;
import com.vocab.bulgarian.llm.validation.LlmOutputValidator;
import com.vocab.bulgarian.llm.validation.LlmValidationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.concurrent.CompletableFuture;

/**
 * Service for generating metadata (part of speech, category, difficulty) for Bulgarian lemmas using LLM.
 * Provides async execution, caching, and circuit breaker protection.
 */
@Service
public class MetadataGenerationService {

    private static final Logger log = LoggerFactory.getLogger(MetadataGenerationService.class);

    private final ChatClient chatClient;
    private final LlmOutputValidator validator;
    private final Timer successTimer;
    private final Timer failureTimer;

    @Lazy
    @Autowired
    private MetadataGenerationService self;

    public MetadataGenerationService(ChatClient chatClient, LlmOutputValidator validator, MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.validator = validator;
        this.successTimer = Timer.builder("vocab.llm.metadata")
                .tag("outcome", "success")
                .description("Ollama metadata generation duration")
                .register(meterRegistry);
        this.failureTimer = Timer.builder("vocab.llm.metadata")
                .tag("outcome", "failure")
                .description("Ollama metadata generation duration (failed)")
                .register(meterRegistry);
    }

    /**
     * Asynchronously generates metadata for a given Bulgarian lemma.
     *
     * @param lemma the lemma (dictionary form)
     * @return CompletableFuture containing the metadata, or null if generation fails
     */
    @Async("llmTaskExecutor")
    public CompletableFuture<LemmaMetadata> generateMetadataAsync(String lemma, String translationHint) {
        log.debug("Async metadata generation requested for: {} (hint: {})", lemma, translationHint);
        LemmaMetadata response = self.generateMetadata(lemma, translationHint);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Synchronous metadata generation with caching and circuit breaker.
     * This method is called by the async wrapper to ensure proper cache behavior.
     *
     * @param lemma           the lemma (dictionary form)
     * @param translationHint optional English translation/notes provided by user for disambiguation
     * @return the metadata, or null if generation fails
     */
    @Cacheable(value = "metadataGeneration", key = "#lemma.trim().toLowerCase() + ':' + (#translationHint != null ? #translationHint.trim().toLowerCase() : '')")
    @CircuitBreaker(name = "ollama", fallbackMethod = "generateMetadataFallback")
    LemmaMetadata generateMetadata(String lemma, String translationHint) {
        String normalizedLemma = lemma.trim().toLowerCase();

        log.debug("Calling LLM for metadata generation: {} (hint: {})", normalizedLemma, translationHint);

        String hintLine = (translationHint != null && !translationHint.isBlank())
            ? String.format("\nIMPORTANT: The user says this word means \"%s\" in English. Use this to determine the correct part of speech.", translationHint)
            : "";

        String prompt = String.format("""
            For the Bulgarian word "%s", determine:%s
            1. Part of speech (one of: NOUN, VERB, ADJECTIVE, ADVERB, PRONOUN, PREPOSITION, CONJUNCTION, NUMERAL, INTERJECTION, PARTICLE, INTERROGATIVE)
            2. Topic category (e.g., "food", "travel", "emotions", "daily life", "academic")
            3. Difficulty level (one of: BEGINNER, INTERMEDIATE, ADVANCED)

            BEGINNER: common everyday words (greetings, numbers, family, food basics)
            INTERMEDIATE: general conversation words (opinions, descriptions, activities)
            ADVANCED: specialized or abstract vocabulary (politics, philosophy, technical)

            Respond in JSON format matching this structure:
            {
              "lemma": "the lemma",
              "partOfSpeech": "VERB|NOUN|ADJECTIVE|etc",
              "category": "topic category",
              "difficultyLevel": "BEGINNER|INTERMEDIATE|ADVANCED"
            }
            """, normalizedLemma, hintLine);

        Timer.Sample sample = Timer.start();
        try {
            LemmaMetadata response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .entity(LemmaMetadata.class);

            log.debug("LLM response for {}: pos={}, category={}, difficulty={}",
                normalizedLemma, response.partOfSpeech(), response.category(), response.difficultyLevel());

            // Validate response
            validator.validateLemmaMetadata(response);

            sample.stop(successTimer);
            return response;
        } catch (LlmValidationException e) {
            sample.stop(failureTimer);
            log.error("Validation failed for metadata generation of {}: {}", normalizedLemma, e.getMessage());
            throw e;
        } catch (Exception e) {
            sample.stop(failureTimer);
            log.error("LLM call failed for metadata generation of {}: {}", normalizedLemma, e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method when circuit breaker opens or LLM call fails.
     * Returns null to signal generation failure (caller handles gracefully).
     */
    @SuppressWarnings("unused")
    LemmaMetadata generateMetadataFallback(String lemma, String translationHint, Exception ex) {
        log.warn("Circuit breaker activated for metadata generation of {}: {}", lemma, ex.getMessage());
        return null;
    }
}
