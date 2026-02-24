package com.vocab.bulgarian.llm.service;

import com.vocab.bulgarian.llm.dto.InflectionSet;
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
 * Service for generating Bulgarian inflections for a lemma using LLM.
 * Provides async execution, caching, and circuit breaker protection.
 */
@Service
public class InflectionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(InflectionGenerationService.class);

    private final ChatClient chatClient;
    private final LlmOutputValidator validator;
    private final Timer successTimer;
    private final Timer failureTimer;

    @Lazy
    @Autowired
    private InflectionGenerationService self;

    public InflectionGenerationService(ChatClient chatClient, LlmOutputValidator validator, MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.validator = validator;
        this.successTimer = Timer.builder("vocab.llm.inflections")
                .tag("outcome", "success")
                .description("Ollama inflection generation duration")
                .register(meterRegistry);
        this.failureTimer = Timer.builder("vocab.llm.inflections")
                .tag("outcome", "failure")
                .description("Ollama inflection generation duration (failed)")
                .register(meterRegistry);
    }

    /**
     * Asynchronously generates all inflections for a given Bulgarian lemma.
     *
     * @param lemma the lemma (dictionary form)
     * @param partOfSpeech the part of speech
     * @return CompletableFuture containing the inflection set, or null if generation fails
     */
    @Async("llmTaskExecutor")
    public CompletableFuture<InflectionSet> generateInflectionsAsync(String lemma, String partOfSpeech) {
        log.debug("Async inflection generation requested for: {} ({})", lemma, partOfSpeech);
        InflectionSet response = self.generateInflections(lemma, partOfSpeech);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Synchronous inflection generation with caching and circuit breaker.
     * This method is called by the async wrapper to ensure proper cache behavior.
     *
     * @param lemma the lemma (dictionary form)
     * @param partOfSpeech the part of speech
     * @return the inflection set, or null if generation fails
     */
    @Cacheable(value = "inflectionGeneration", key = "#lemma.trim().toLowerCase() + ':' + #partOfSpeech")
    @CircuitBreaker(name = "ollama", fallbackMethod = "generateInflectionsFallback")
    InflectionSet generateInflections(String lemma, String partOfSpeech) {
        String normalizedLemma = lemma.trim().toLowerCase();

        log.debug("Calling LLM for inflection generation: {} ({})", normalizedLemma, partOfSpeech);

        String prompt = String.format("""
            Generate ALL inflections for the Bulgarian %s "%s".

            For verbs, include all persons (1st, 2nd, 3rd) and numbers (singular, plural)
            for present tense, past aorist, past imperfect, and imperative mood.
            Include the grammatical tags for each form (e.g., "1sg.pres", "3pl.past.aor").

            IMPORTANT: Tag each inflection with a difficulty level:
            - BASIC: Only 1sg.pres (аз) and 3sg.pres (той/тя/то) - beginner forms matching Elena's teaching
            - INTERMEDIATE: Remaining present tense: 2sg.pres (ти), 1pl.pres (ние), 2pl.pres (вие), 3pl.pres (те)
            - ADVANCED: All past tenses (aorist, imperfect) and imperative forms

            For nouns, include singular and plural forms, with and without the definite article.
            Include grammatical tags (e.g., "sg.indef", "sg.def", "pl.indef", "pl.def").
            Tag: BASIC (sg.indef), INTERMEDIATE (pl.indef), ADVANCED (definite articles).

            For adjectives, include masculine, feminine, neuter, and plural forms,
            with and without the definite article.
            Tag: BASIC (masc), INTERMEDIATE (fem, neut, pl), ADVANCED (definite forms).

            For each inflection, add accentedForm with the Unicode combining acute accent (U+0301)
            on the stressed vowel (e.g. часа́, ра́бота). This is critical where the same spelling
            carries different stress in different meanings. Omit (null) if stress is unambiguous.

            Respond in JSON format matching this structure:
            {
              "lemma": "the lemma",
              "partOfSpeech": "VERB|NOUN|ADJECTIVE|etc",
              "inflections": [
                {
                  "text": "inflected form",
                  "grammaticalTags": "tags",
                  "difficultyLevel": "BASIC|INTERMEDIATE|ADVANCED",
                  "accentedForm": "form with acute accent on stressed vowel e.g. часа́ — use Unicode U+0301, or null"
                }
              ]
            }
            """, partOfSpeech, normalizedLemma);

        Timer.Sample sample = Timer.start();
        try {
            InflectionSet response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .entity(InflectionSet.class);

            log.debug("LLM response for {} ({}): {} inflections generated",
                normalizedLemma, partOfSpeech, response.inflections().size());

            // Validate response
            validator.validateInflectionSet(response);

            sample.stop(successTimer);
            return response;
        } catch (LlmValidationException e) {
            sample.stop(failureTimer);
            log.error("Validation failed for inflection generation of {} ({}): {}",
                normalizedLemma, partOfSpeech, e.getMessage());
            throw e;
        } catch (Exception e) {
            sample.stop(failureTimer);
            log.error("LLM call failed for inflection generation of {} ({}): {}",
                normalizedLemma, partOfSpeech, e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method when circuit breaker opens or LLM call fails.
     * Returns null to signal generation failure (caller handles gracefully).
     */
    @SuppressWarnings("unused")
    InflectionSet generateInflectionsFallback(String lemma, String partOfSpeech, Exception ex) {
        log.warn("Circuit breaker activated for inflection generation of {} ({}): {}",
            lemma, partOfSpeech, ex.getMessage());
        return null;
    }
}
