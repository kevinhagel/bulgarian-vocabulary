package com.vocab.bulgarian.llm.service;

import com.vocab.bulgarian.llm.dto.LemmaDetectionResponse;
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
 * Service for detecting Bulgarian lemmas (dictionary forms) from word forms using LLM.
 * Provides async execution, caching, and circuit breaker protection.
 */
@Service
public class LemmaDetectionService {

    private static final Logger log = LoggerFactory.getLogger(LemmaDetectionService.class);

    private final ChatClient chatClient;
    private final LlmOutputValidator validator;

    public LemmaDetectionService(ChatClient chatClient, LlmOutputValidator validator) {
        this.chatClient = chatClient;
        this.validator = validator;
    }

    /**
     * Asynchronously detects the lemma for a given Bulgarian word form.
     *
     * @param wordForm the inflected word form
     * @return CompletableFuture containing the lemma detection response
     */
    @Async("llmTaskExecutor")
    public CompletableFuture<LemmaDetectionResponse> detectLemmaAsync(String wordForm) {
        log.debug("Async lemma detection requested for: {}", wordForm);
        LemmaDetectionResponse response = detectLemma(wordForm);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Synchronous lemma detection with caching and circuit breaker.
     * This method is called by the async wrapper to ensure proper cache behavior.
     *
     * @param wordForm the inflected word form
     * @return the lemma detection response
     */
    @Cacheable(value = "lemmaDetection", key = "#wordForm.trim().toLowerCase()")
    @CircuitBreaker(name = "ollama", fallbackMethod = "detectLemmaFallback")
    LemmaDetectionResponse detectLemma(String wordForm) {
        String normalizedWordForm = wordForm.trim().toLowerCase();

        log.debug("Calling LLM for lemma detection: {}", normalizedWordForm);

        String prompt = String.format("""
            Given the Bulgarian word "%s", identify its lemma (dictionary form).
            For verbs, the lemma is the 1st person singular present tense form.
            For nouns, the lemma is the singular indefinite form.
            For adjectives, the lemma is the masculine singular indefinite form.

            Respond in JSON format matching this structure:
            {
              "wordForm": "the original word",
              "lemma": "detected lemma",
              "partOfSpeech": "VERB|NOUN|ADJECTIVE|etc",
              "detectionFailed": false
            }
            """, normalizedWordForm);

        try {
            LemmaDetectionResponse response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .entity(LemmaDetectionResponse.class);

            log.debug("LLM response for {}: lemma={}, pos={}",
                normalizedWordForm, response.lemma(), response.partOfSpeech());

            // Validate response
            validator.validateLemmaDetection(response);

            return response;
        } catch (LlmValidationException e) {
            log.error("Validation failed for lemma detection of {}: {}", normalizedWordForm, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("LLM call failed for lemma detection of {}: {}", normalizedWordForm, e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method when circuit breaker opens or LLM call fails.
     * Returns a failed detection response.
     */
    @SuppressWarnings("unused")
    LemmaDetectionResponse detectLemmaFallback(String wordForm, Exception ex) {
        log.warn("Circuit breaker activated for lemma detection of {}: {}", wordForm, ex.getMessage());
        return LemmaDetectionResponse.failed(wordForm);
    }
}
