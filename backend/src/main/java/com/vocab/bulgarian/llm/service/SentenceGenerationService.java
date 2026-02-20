package com.vocab.bulgarian.llm.service;

import com.vocab.bulgarian.llm.dto.SentenceSet;
import com.vocab.bulgarian.llm.validation.LlmOutputValidator;
import com.vocab.bulgarian.llm.validation.LlmValidationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Generates Bulgarian example sentences using Qwen 2.5 14B.
 * Provides async execution, caching, and circuit breaker protection.
 */
@Service
public class SentenceGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SentenceGenerationService.class);

    private final ChatClient sentenceChatClient;
    private final LlmOutputValidator validator;
    private final Timer successTimer;
    private final Timer failureTimer;

    public SentenceGenerationService(
            @Qualifier("sentenceChatClient") ChatClient sentenceChatClient,
            LlmOutputValidator validator,
            MeterRegistry meterRegistry) {
        this.sentenceChatClient = sentenceChatClient;
        this.validator = validator;
        this.successTimer = Timer.builder("vocab.llm.sentences")
                .tag("outcome", "success")
                .description("Qwen sentence generation duration")
                .register(meterRegistry);
        this.failureTimer = Timer.builder("vocab.llm.sentences")
                .tag("outcome", "failure")
                .description("Qwen sentence generation duration (failed)")
                .register(meterRegistry);
    }

    @Async("llmTaskExecutor")
    public CompletableFuture<SentenceSet> generateSentencesAsync(String lemma, String translation, String partOfSpeech) {
        log.debug("Async sentence generation requested for: {}", lemma);
        SentenceSet result = generateSentences(lemma, translation, partOfSpeech);
        return CompletableFuture.completedFuture(result);
    }

    @Cacheable(value = "sentenceGeneration", key = "#lemma.trim().toLowerCase()")
    @CircuitBreaker(name = "ollama-sentence", fallbackMethod = "generateSentencesFallback")
    SentenceSet generateSentences(String lemma, String translation, String partOfSpeech) {
        String normalizedLemma = lemma.trim().toLowerCase();
        String posLabel = (partOfSpeech != null && !partOfSpeech.isBlank()) ? partOfSpeech.toLowerCase() : "word";
        String translationClause = (translation != null && !translation.isBlank())
            ? ", means \"" + translation + "\""
            : "";

        log.debug("Calling Qwen 2.5 14B for sentence generation: {}", normalizedLemma);

        String prompt = String.format("""
            Generate 4 natural Bulgarian example sentences for the %s "%s"%s.

            Requirements:
            - Each sentence must clearly feature "%s" used naturally
            - Sentences should progress from simple to more complex
            - Include a mix of contexts (everyday conversation, questions, descriptions)
            - Bulgarian text must be grammatically correct
            - Translations must be accurate English

            Respond ONLY in this exact JSON format:
            {
              "lemma": "%s",
              "sentences": [
                {"bulgarianText": "...", "englishTranslation": "..."},
                {"bulgarianText": "...", "englishTranslation": "..."},
                {"bulgarianText": "...", "englishTranslation": "..."},
                {"bulgarianText": "...", "englishTranslation": "..."}
              ]
            }
            """, posLabel, normalizedLemma, translationClause, normalizedLemma, normalizedLemma);

        Timer.Sample sample = Timer.start();
        try {
            SentenceSet response = sentenceChatClient
                .prompt()
                .user(prompt)
                .call()
                .entity(SentenceSet.class);

            validator.validateSentenceSet(response);

            log.debug("Sentence generation completed for {}: {} sentences", normalizedLemma,
                response != null ? response.sentences().size() : 0);

            sample.stop(successTimer);
            return response;
        } catch (LlmValidationException e) {
            sample.stop(failureTimer);
            log.error("Validation failed for sentence generation of {}: {}", normalizedLemma, e.getMessage());
            throw e;
        } catch (Exception e) {
            sample.stop(failureTimer);
            log.error("Sentence generation failed for {}: {}", normalizedLemma, e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unused")
    SentenceSet generateSentencesFallback(String lemma, String translation, String partOfSpeech, Exception ex) {
        log.warn("Circuit breaker activated for sentence generation of {}: {}", lemma, ex.getMessage());
        return null;
    }
}
