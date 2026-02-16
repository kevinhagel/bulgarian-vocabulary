package com.vocab.bulgarian.llm.service;

import com.vocab.bulgarian.llm.dto.LemmaMetadata;
import com.vocab.bulgarian.llm.validation.LlmOutputValidator;
import com.vocab.bulgarian.llm.validation.LlmValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
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

    public MetadataGenerationService(ChatClient chatClient, LlmOutputValidator validator) {
        this.chatClient = chatClient;
        this.validator = validator;
    }

    /**
     * Asynchronously generates metadata for a given Bulgarian lemma.
     *
     * @param lemma the lemma (dictionary form)
     * @return CompletableFuture containing the metadata, or null if generation fails
     */
    @Async("llmTaskExecutor")
    public CompletableFuture<LemmaMetadata> generateMetadataAsync(String lemma) {
        log.debug("Async metadata generation requested for: {}", lemma);
        LemmaMetadata response = generateMetadata(lemma);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Synchronous metadata generation with caching and circuit breaker.
     * This method is called by the async wrapper to ensure proper cache behavior.
     *
     * @param lemma the lemma (dictionary form)
     * @return the metadata, or null if generation fails
     */
    @Cacheable(value = "metadataGeneration", key = "#lemma.trim().toLowerCase()")
    @CircuitBreaker(name = "ollama", fallbackMethod = "generateMetadataFallback")
    LemmaMetadata generateMetadata(String lemma) {
        String normalizedLemma = lemma.trim().toLowerCase();

        log.debug("Calling LLM for metadata generation: {}", normalizedLemma);

        String prompt = String.format("""
            For the Bulgarian word "%s", determine:
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
            """, normalizedLemma);

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

            return response;
        } catch (LlmValidationException e) {
            log.error("Validation failed for metadata generation of {}: {}", normalizedLemma, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("LLM call failed for metadata generation of {}: {}", normalizedLemma, e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method when circuit breaker opens or LLM call fails.
     * Returns null to signal generation failure (caller handles gracefully).
     */
    @SuppressWarnings("unused")
    LemmaMetadata generateMetadataFallback(String lemma, Exception ex) {
        log.warn("Circuit breaker activated for metadata generation of {}: {}", lemma, ex.getMessage());
        return null;
    }
}
